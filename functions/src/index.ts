import * as functions from "firebase-functions/v2";
import * as legacyFunctions from "firebase-functions";
import * as admin from "firebase-admin";
import { CallableRequest, onRequest } from "firebase-functions/v2/https";
import { onCall } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import Stripe from "stripe";
import { onDocumentCreated, onDocumentUpdated, onDocumentDeleted } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { VertexAI } from "@google-cloud/vertexai";

admin.initializeApp();

const stripeSecretKey = defineSecret("STRIPE_SECRET_KEY");
const stripeWebhookSecret = defineSecret("STRIPE_WEBHOOK_SECRET");
const replicateApiToken = defineSecret("REPLICATE_API_TOKEN");
const DEFAULT_VIRTUAL_TRY_ON_MODEL_VERSION =
  "39ed52f2a78e934b3ba6e2a89f5b1c712de7dfea535525255b1aa35c5565e08b";

// Deposit rate constant (20%)
const DEPOSIT_RATE = 0.20;
const DEFAULT_AT_HOME_SERVICE_FEE = 20;
const FIRST_BOOKING_PROMO_CODE = "FIRST10";
const FIRST_BOOKING_PROMO_AMOUNT = 10;
export const BUILD_ENV = "live"; // v2

// Initialize Vertex AI
const vertexAi = new VertexAI({ project: process.env.GCLOUD_PROJECT, location: "us-central1" });
const generativeModel = vertexAi.getGenerativeModel({ model: "gemini-1.5-flash-001" });

// --- PUSH NOTIFICATION HELPERS ---

async function sendPushNotification(userId: string, title: string, body: string, data?: any) {
    try {
        const userDoc = await admin.firestore().collection("users").doc(userId).get();
        let fcmToken = userDoc.data()?.fcmToken;

        if (!fcmToken) {
            // Check stylist doc too
            const stylistDoc = await admin.firestore().collection("stylists").doc(userId).get();
            fcmToken = stylistDoc.data()?.fcmToken;
        }

        if (!fcmToken) {
            console.log(`No FCM token for user ${userId}, skipping notification.`);
            return;
        }

        const message: admin.messaging.Message = {
            token: fcmToken,
            notification: {
                title: title,
                body: body,
            },
            data: data || {},
            android: {
                priority: "high",
                notification: {
                    channelId: notificationChannelForType(data?.type),
                }
            }
        };

        await admin.messaging().send(message);
        console.log(`Notification sent to user ${userId}`);
    } catch (error) {
        console.error(`Error sending notification to user ${userId}:`, error);
    }
}

function notificationChannelForType(type: unknown): string {
  if (type === "chat") return "chat_channel";
  if (type === "booking_request" || type === "asap_request" || type === "new_booking") {
    return "urgent_channel";
  }
  return "default_channel";
}

function publicProfileFrom(data: Record<string, any>, role: "CUSTOMER" | "STYLIST") {
  const name = stringValue(data.name) || stringValue(data.displayName);
  const profileImageUrl = stringValue(data.profileImageUrl) || stringValue(data.imageUrl);
  return {
    name,
    displayName: name,
    profileImageUrl,
    imageUrl: profileImageUrl,
    role,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
}

function publicUserRoleFrom(data: Record<string, any>): "CUSTOMER" | "STYLIST" {
  const role = stringValue(data.role) || stringValue(data.userRoleValue);
  return role.toUpperCase() === "STYLIST" ? "STYLIST" : "CUSTOMER";
}

function isCompletedStatus(status: unknown): boolean {
  return typeof status === "string" && status.toUpperCase() === "COMPLETED";
}

function normalizedStatus(status: unknown): string {
  return typeof status === "string" ? status.trim().toUpperCase() : "";
}

function isCancelledStatus(status: unknown): boolean {
  return ["CANCELLED", "CANCELED"].includes(normalizedStatus(status));
}

function isTerminalStatus(status: unknown): boolean {
  return [
    "CANCELLED",
    "CANCELED",
    "DECLINED",
    "COMPLETED",
    "COMPLETION_DISPUTED",
    "PAYMENT_CANCELLED",
    "PAYMENT_FAILED",
  ].includes(normalizedStatus(status));
}

async function awardLoyaltyPointsForCompletedBooking(bookingId: string): Promise<void> {
  const db = admin.firestore();
  const bookingRef = db.collection("bookings").doc(bookingId);

  await db.runTransaction(async (txn) => {
    const bookingSnap = await txn.get(bookingRef);
    if (!bookingSnap.exists) return;

    const booking = bookingSnap.data() || {};
    if (!isCompletedStatus(booking.status)) return;
    if (booking.pointsAwarded === true) return;
    if (!booking.paidAt) {
      console.log(`Booking ${bookingId} is completed but not paid; skipping loyalty points.`);
      return;
    }

    const customerId = String(booking.customerId || booking.userId || "");
    if (!customerId) {
      console.warn(`Booking ${bookingId} has no customerId/userId; cannot award loyalty points.`);
      return;
    }

    const servicePrice = Number(booking.servicePrice);
    const priceFromCents = Number(booking.priceCents) / 100;
    const pointsEarned = Math.floor(
      Number.isFinite(servicePrice) && servicePrice > 0 ? servicePrice : priceFromCents
    );

    if (!Number.isFinite(pointsEarned) || pointsEarned <= 0) {
      txn.update(bookingRef, { pointsAwarded: true });
      return;
    }

    const userRef = db.collection("users").doc(customerId);
    const userSnap = await txn.get(userRef);
    const currentPoints = userSnap.exists ? Number(userSnap.get("loyaltyPoints") || 0) : 0;
    const safeCurrentPoints = Number.isFinite(currentPoints) && currentPoints > 0 ? currentPoints : 0;
    const nextPoints = safeCurrentPoints + pointsEarned;
    const serviceName = String(booking.serviceName || "Service");
    const stylistName = String(booking.stylistName || "your stylist");
    const histRef = userRef.collection("pointsHistory").doc();

    if (userSnap.exists) {
      txn.update(userRef, { loyaltyPoints: nextPoints });
    } else {
      txn.set(userRef, { loyaltyPoints: nextPoints }, { merge: true });
    }

    txn.set(histRef, {
      type: "earned",
      amount: pointsEarned,
      description: `${serviceName} with ${stylistName}`,
      bookingId,
      date: admin.firestore.FieldValue.serverTimestamp(),
    });
    txn.update(bookingRef, { pointsAwarded: true });
  });
}

// --- AUTH TRIGGERS (CLEANUP) ---

// Automatically delete user data when their account is deleted
export const onUserDeleted = legacyFunctions.auth.user().onDelete(async (user) => {
    const uid = user.uid;
    const db = admin.firestore();
    const batch = db.batch();

    console.log(`User ${uid} deleted. Cleaning up Firestore data...`);

    // 1. Delete user profile
    batch.delete(db.collection("users").doc(uid));

    // 2. Delete stylist profile if it exists
    batch.delete(db.collection("stylists").doc(uid));
    batch.delete(db.collection("publicUserProfiles").doc(uid));

    // 3. Mark their bookings as 'cancelled' or anonymize them
    const bookingsSnapshot = await db.collection("bookings").where("userId", "==", uid).get();
    bookingsSnapshot.docs.forEach(doc => {
        batch.update(doc.ref, {
            customerName: "[Deleted Account]",
            customerPhotoUrl: null,
            status: "CANCELLED",
            deletionNote: "Customer deleted their account"
        });
    });

    return batch.commit();
});

// --- FIRESTORE TRIGGERS ---

export const syncPublicUserProfileOnCreate = onDocumentCreated("users/{userId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    await admin.firestore()
        .collection("publicUserProfiles")
        .doc(event.params.userId)
        .set(publicProfileFrom(snapshot.data(), publicUserRoleFrom(snapshot.data())), { merge: true });
});

export const syncPublicUserProfileOnUpdate = onDocumentUpdated("users/{userId}", async (event) => {
    const snapshot = event.data?.after;
    if (!snapshot) return;
    await admin.firestore()
        .collection("publicUserProfiles")
        .doc(event.params.userId)
        .set(publicProfileFrom(snapshot.data(), publicUserRoleFrom(snapshot.data())), { merge: true });
});

export const syncPublicStylistProfileOnCreate = onDocumentCreated("stylists/{stylistId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    await admin.firestore()
        .collection("publicUserProfiles")
        .doc(event.params.stylistId)
        .set(publicProfileFrom(snapshot.data(), "STYLIST"), { merge: true });
});

export const syncPublicStylistProfileOnUpdate = onDocumentUpdated("stylists/{stylistId}", async (event) => {
    const snapshot = event.data?.after;
    if (!snapshot) return;
    await admin.firestore()
        .collection("publicUserProfiles")
        .doc(event.params.stylistId)
        .set(publicProfileFrom(snapshot.data(), "STYLIST"), { merge: true });
});

// 1. STYLE RECOMMENDATIONS
export const generateStyleRecommendations = onDocumentCreated("aiStyleRequests/{requestId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const requestData = snapshot.data();
    const requestId = event.params.requestId;

    try {
        await admin.firestore().collection("aiStyleRequests").doc(requestId).update({ status: "processing" });
        const stylesSnapshot = await admin.firestore().collection("styles").get();
        const styleCatalog = stylesSnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));

        if (styleCatalog.length === 0) throw new Error("Style catalog is empty.");

        const prompt = `
            You are an expert hairstylist AI. A user has provided the following preferences:
            ${JSON.stringify(requestData.answers, null, 2)}
            ... (rest of prompt)
        `;

        const resp = await generativeModel.generateContent(prompt);
        const content = resp?.response?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
        const jsonString = content.substring(content.indexOf("{"), content.lastIndexOf("}") + 1);
        const result = JSON.parse(jsonString);

        await admin.firestore().collection("aiStyleRequests").doc(requestId).update({
            status: "done",
            result: result,
        });
    } catch (error) {
        console.error("Error generating style recommendations:", error);
    }
});

// 2. NEW BOOKING NOTIFICATION (To Stylist)
export const onBookingCreated = onDocumentCreated("bookings/{bookingId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const booking = snapshot.data();

    // Only notify stylist after payment is confirmed.
    // If status is pending_payment, the Stripe webhook will update it to
    // DEPOSIT_PAID and onBookingUpdated will send the notification then.
    if (booking.status === "pending_payment") return;

    const stylistId = booking.stylistId;
    const customerName = booking.customerName || "A customer";

    await sendPushNotification(
        stylistId,
        "New Booking Request! ??",
        `${customerName} requested an appointment for ${booking.serviceName}.`,
        {
            type: "booking_request",
            targetId: event.params.bookingId
        }
    );
});

// 3. BOOKING STATUS UPDATE NOTIFICATION (To Customer)
export const onBookingUpdated = onDocumentUpdated("bookings/{bookingId}", async (event) => {
    const newData = event.data?.after.data();
    const prevData = event.data?.before.data();
    
    if (!newData || !prevData) return;

    // If status changed
    if (newData.status !== prevData.status) {
        const userId = newData.userId || newData.customerId;
        const stylistName = newData.stylistName || "Your stylist";

        let title = "Booking Update";
        let body = `Your appointment status is now: ${newData.status}`;

        if (newData.status === "ACCEPTED" || newData.status === "accepted") {
            title = "Booking Confirmed! ?";
            body = `${stylistName} has accepted your appointment request. See you soon!`;
        } else if (newData.status === "DECLINED" || newData.status === "declined") {
            title = "Booking Declined ?";
            body = `Unfortunately, ${stylistName} could not accept your request at this time.`;
        } else if (newData.status === "AWAITING_CUSTOMER_CONFIRMATION") {
            title = "Confirm your session";
            body = `${stylistName} marked your session complete. Please confirm or report an issue within 24 hours.`;
        } else if (newData.status === "COMPLETED") {
            title = "Session completed";
            body = "Thanks for booking with RefreshMe. You can now leave a rating.";
        } else if (newData.status === "COMPLETION_DISPUTED") {
            await sendPushNotification(
                newData.stylistId,
                "Completion disputed",
                `${newData.customerName || "Your client"} reported an issue. Payout is paused for review.`,
                { type: "booking", targetId: event.params.bookingId }
            );
            return;
        } else if (newData.status === "DEPOSIT_PAID" || newData.status === "paid") {
            // Payment confirmed — now notify the stylist with a booking request
            // (this handles the case where booking was created as pending_payment
            // and the Stripe webhook updated it to DEPOSIT_PAID)
            const customerName = newData.customerName || "A customer";
            await sendPushNotification(
                newData.stylistId,
                "New Booking Request! ??",
                `${customerName} requested an appointment for ${newData.serviceName}.`,
                { type: "booking_request", targetId: event.params.bookingId }
            );
            return;
        }

        await sendPushNotification(userId, title, body, {
            type: "booking",
            targetId: event.params.bookingId
        });

        if (isCompletedStatus(newData.status)) {
            await awardLoyaltyPointsForCompletedBooking(event.params.bookingId);
        }
    }
});

// 4. NEW MESSAGE NOTIFICATION — listens to conversations/ (iOS/Flutter path)
export const onConversationMessageCreated = onDocumentCreated("conversations/{chatId}/messages/{messageId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const message = snapshot.data();
    const chatId = event.params.chatId;
    const messageId = event.params.messageId;
    const senderId = message.senderId as string;
    if (!senderId) return;

    // Look up conversation to find the other participant(s)
    let participants: string[] = [];
    try {
        const convDoc = await admin.firestore().collection("conversations").doc(chatId).get();
        participants = (convDoc.data()?.participants as string[]) || [];
    } catch (e) {
        console.warn("Could not fetch conversation doc");
        return;
    }

    if (!participants.includes(senderId)) {
        console.warn(`Sender ${senderId} is not a participant in conversation ${chatId}`);
        return;
    }

    // Android still reads from chats/{sortedUserIds}/messages and its inbox reads
    // users/{uid}/conversations. Mirror the iOS/Flutter conversation write so both
    // clients see the same message instead of only receiving a push notification.
    if (participants.length === 2) {
        const recipientId = participants.find((p) => p !== senderId);
        if (recipientId) {
            const androidChatId = [...participants].sort().join("_");
            const chatRef = admin.firestore().collection("chats").doc(androidChatId);
            const mirroredMessageRef = chatRef.collection("messages").doc(messageId);
            await admin.firestore().runTransaction(async (transaction) => {
                transaction.set(chatRef, {
                    participants: [...participants].sort(),
                    lastMessage: message.text || "Sent a message",
                    lastMessageAt: admin.firestore.FieldValue.serverTimestamp(),
                    lastMessageTimestamp: admin.firestore.FieldValue.serverTimestamp(),
                    lastSenderId: senderId,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                    sourceConversationId: chatId,
                }, { merge: true });

                transaction.set(mirroredMessageRef, {
                    ...message,
                    senderId,
                    receiverId: recipientId,
                    timestamp: message.timestamp || admin.firestore.FieldValue.serverTimestamp(),
                    mirroredFrom: "conversations",
                    sourceConversationId: chatId,
                }, { merge: true });

                for (const participantId of participants) {
                    const otherUserId = participantId === senderId ? recipientId : senderId;
                    transaction.set(
                        admin.firestore()
                            .collection("users")
                            .doc(participantId)
                            .collection("conversations")
                            .doc(otherUserId),
                        {
                            otherUserId,
                            lastMessage: message.text || "Sent a message",
                            lastMessageTime: admin.firestore.FieldValue.serverTimestamp(),
                            lastSenderId: senderId,
                        },
                        { merge: true }
                    );
                }
            });
        }
    }

    // Resolve sender display name
    let senderName = "Someone";
    try {
        const stylistDoc = await admin.firestore().collection("stylists").doc(senderId).get();
        if (stylistDoc.exists) {
            senderName = stylistDoc.data()?.name || "Someone";
        } else {
            const userDoc = await admin.firestore().collection("users").doc(senderId).get();
            senderName = userDoc.data()?.name || userDoc.data()?.displayName || "Someone";
        }
    } catch (e) {
        console.warn("Could not fetch sender name");
    }

    // Notify every participant except the sender
    const recipients = participants.filter((p) => p !== senderId);
    for (const recipientId of recipients) {
        await sendPushNotification(
            recipientId,
            `New Message from ${senderName}`,
            message.text || "Sent a message",
            { type: "chat", targetId: chatId, chatId, senderId }
        );
    }
});

// Legacy chats/ trigger kept for Android compatibility
export const onMessageCreated = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const message = snapshot.data();
    if (message.mirroredFrom === "conversations") return;
    const receiverId = message.receiverId;
    const senderId = message.senderId;
    if (!receiverId) return;
    let senderName = "Someone";
    try {
        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        senderName = senderDoc.exists ? (senderDoc.data()?.name || "Someone")
            : ((await admin.firestore().collection("stylists").doc(senderId).get()).data()?.name || "Someone");
    } catch (e) { /* ignore */ }
    await sendPushNotification(receiverId, `New Message from ${senderName}`,
        message.text || "Sent a message", { type: "chat", targetId: senderId });
});

// 5. FLASH DEAL NOTIFICATION (To Customers)
export const onFlashDealCreated = onDocumentUpdated("stylists/{stylistId}", async (event) => {
    const newData = event.data?.after.data();
    const prevData = event.data?.before.data();

    if (!newData || !prevData) return;

    // Check if a flash deal was added
    if (newData.currentFlashDeal && !prevData.currentFlashDeal) {
        const stylistName = newData.name || "A stylist";
        const dealTitle = newData.currentFlashDeal.title;
        const discount = newData.currentFlashDeal.discountPercentage;

        // Find customers (limit for demo/cost)
        const customersSnapshot = await admin.firestore().collection("users").where("userRoleValue", "==", "CUSTOMER").limit(50).get();

        const notifications = customersSnapshot.docs.map(doc => {
            return sendPushNotification(
                doc.id,
                "Flash Deal! ??",
                `${stylistName} just launched a deal: ${dealTitle} (${discount}% OFF). Book now!`,
                { type: "stylist_detail", targetId: event.params.stylistId }
            );
        });

        await Promise.all(notifications);
    }
});


// --- STRIPE CONNECT & PAYMENTS ---

const PLATFORM_FEE_PERCENT = 0.10; // RefreshMe takes 10% of every booking
const STYLIST_PAYOUT_PERCENT = 1 - PLATFORM_FEE_PERCENT;
// Production callables require App Check. Keep Firebase Console enrollment in
// sync with every shipped bundle/package before deploying changes here.
const CALLABLE_APP_CHECK_OPTIONS = { enforceAppCheck: true };
const VIRTUAL_TRY_ON_APP_CHECK_OPTIONS = { enforceAppCheck: false };

type ServiceCatalogItem = {
  id?: string;
  name?: string;
  price?: number;
  durationMinutes?: number;
  isBundle?: boolean;
  bundle?: boolean;
  isAddOn?: boolean;
  addOn?: boolean;
};

type PreparedBooking = {
  bookingData: Record<string, any>;
  depositAmountCents: number;
  depositAmountDollars: number;
  serviceName: string;
  servicePrice: number;
  stylistStripeAccountId?: string;
  userEmail?: string;
};

function connectStatus(account: Stripe.Account): "active" | "pending" {
  return account.charges_enabled && account.details_submitted ? "active" : "pending";
}

function connectStatusUpdate(account: Stripe.Account) {
  const status = connectStatus(account);
  return {
    stripeAccountId: account.id,
    stripeAccountStatus: status,
    stripeChargesEnabled: account.charges_enabled,
    stripePayoutsEnabled: account.payouts_enabled,
    stripeDetailsSubmitted: account.details_submitted,
    stripeOnboardingComplete: status === "active",
  };
}

function requirePayoutReady(account: Stripe.Account) {
  if (!account.charges_enabled || !account.details_submitted || !account.payouts_enabled) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "This stylist has not finished payout setup yet. Please choose another stylist or try again after they complete Stripe onboarding.",
    );
  }
}

function isHttpsError(error: any): boolean {
  return Boolean(error?.code && error?.message && error?.httpErrorCode);
}

function friendlyStripeBookingError(error: any): functions.https.HttpsError {
  const code = stringValue(error?.code);
  const message = stringValue(error?.message);
  const type = stringValue(error?.type);
  const declineCode = stringValue(error?.decline_code);

  console.error("Stripe booking setup error:", {
    code,
    type,
    declineCode,
    message,
    requestId: error?.requestId,
  });

  if (
    code === "account_invalid" ||
    code === "resource_missing" ||
    code === "parameter_invalid_empty" ||
    message.toLowerCase().includes("no such account") ||
    message.toLowerCase().includes("destination")
  ) {
    return new functions.https.HttpsError(
      "failed-precondition",
      "This stylist's payout account is not ready for in-app payments yet. Please ask the stylist to finish Stripe payout setup and try again.",
    );
  }

  if (
    code === "amount_too_small" ||
    code === "amount_too_large" ||
    message.toLowerCase().includes("amount")
  ) {
    return new functions.https.HttpsError(
      "failed-precondition",
      "This service price cannot be charged right now. Please choose another service or ask the stylist to update pricing.",
    );
  }

  return new functions.https.HttpsError(
    "internal",
    "Payment setup failed. Please try again in a moment.",
  );
}

function bookingDepositAmountCents(booking: Record<string, any>): number {
  const storedDeposit = Number(booking.depositAmount);
  const servicePrice = Number(booking.servicePrice);
  const fallbackDeposit = Number.isFinite(servicePrice) ? servicePrice * DEPOSIT_RATE : 0;
  const depositDollars = Number.isFinite(storedDeposit) && storedDeposit > 0
    ? storedDeposit
    : fallbackDeposit;
  return Math.round(depositDollars * 100);
}

async function paymentIntentIdForBooking(
  stripe: Stripe,
  booking: Record<string, any>,
): Promise<string> {
  const paymentIntentId = stringValue(booking.paymentIntentId);
  if (paymentIntentId) return paymentIntentId;

  const checkoutSessionId = stringValue(booking.checkoutSessionId);
  if (!checkoutSessionId) return "";

  const session = await stripe.checkout.sessions.retrieve(checkoutSessionId);
  return typeof session.payment_intent === "string"
    ? session.payment_intent
    : session.payment_intent?.id || "";
}

async function releaseStylistPayoutForCompletedBooking(
  bookingId: string,
  booking: Record<string, any>,
  stripe: Stripe,
): Promise<void> {
  const bookingRef = admin.firestore().collection("bookings").doc(bookingId);
  if (stringValue(booking.payoutTransferId)) return;

  const destination = stringValue(booking.stripeAccountId);
  if (!destination) {
    await bookingRef.update({
      payoutStatus: "release_failed",
      payoutFailureMessage: "Missing stylist Stripe account",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    throw new functions.https.HttpsError(
      "failed-precondition",
      "The stylist payout account is missing for this booking.",
    );
  }

  const paymentIntentId = await paymentIntentIdForBooking(stripe, booking);
  if (!paymentIntentId) {
    await bookingRef.update({
      payoutStatus: "release_failed",
      payoutFailureMessage: "Missing payment intent",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    throw new functions.https.HttpsError(
      "failed-precondition",
      "The payment record is missing for this booking.",
    );
  }

  const paymentIntent = await stripe.paymentIntents.retrieve(paymentIntentId, {
    expand: ["latest_charge"],
  });

  if (paymentIntent.transfer_data?.destination) {
    await bookingRef.update({
      payoutStatus: "released_legacy_destination_charge",
      payoutTransferDestination: String(paymentIntent.transfer_data.destination),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    return;
  }

  const latestCharge = paymentIntent.latest_charge;
  const sourceTransaction = typeof latestCharge === "string" ? latestCharge : latestCharge?.id;
  const payoutAmountCents = Math.round(bookingDepositAmountCents(booking) * STYLIST_PAYOUT_PERCENT);

  if (!sourceTransaction || payoutAmountCents <= 0) {
    await bookingRef.update({
      payoutStatus: "release_failed",
      payoutFailureMessage: "Invalid payout source or amount",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    throw new functions.https.HttpsError(
      "failed-precondition",
      "This booking payment cannot be released yet.",
    );
  }

  const transfer = await stripe.transfers.create(
    {
      amount: payoutAmountCents,
      currency: stringValue(booking.currency) || "usd",
      destination,
      source_transaction: sourceTransaction,
      transfer_group: `booking_${bookingId}`,
      metadata: {
        bookingId,
        customerId: stringValue(booking.customerId) || stringValue(booking.userId),
        stylistId: stringValue(booking.stylistId),
        paymentIntentId,
        releaseReason: stringValue(booking.autoConfirmedAt) ? "auto_confirmed" : "customer_confirmed",
      },
    },
    { idempotencyKey: `booking-payout-release-${bookingId}` },
  );

  await bookingRef.update({
    payoutStatus: "paid",
    payoutReleasedAt: admin.firestore.FieldValue.serverTimestamp(),
    payoutTransferId: transfer.id,
    payoutAmountCents,
    payoutTransferDestination: destination,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}

async function retrieveReadyConnectAccount(
  stripe: Stripe,
  accountId: string,
  stylistId: string,
): Promise<Stripe.Account> {
  try {
    const connectedAccount = await stripe.accounts.retrieve(accountId);
    requirePayoutReady(connectedAccount);
    await admin.firestore().collection("stylists").doc(stylistId).set(
      connectStatusUpdate(connectedAccount),
      { merge: true },
    );
    return connectedAccount;
  } catch (error: any) {
    if (isHttpsError(error)) throw error;
    throw friendlyStripeBookingError(error);
  }
}

function stringValue(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

function booleanValue(value: unknown): boolean {
  return value === true;
}

function positiveInteger(value: unknown, fallback: number, max: number): number {
  const n = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(n)) return fallback;
  return Math.min(Math.max(Math.floor(n), 1), max);
}

function stringArray(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return value.map((item) => stringValue(item)).filter(Boolean);
}

function timestampFromMillis(value: unknown) {
  const millis = typeof value === "number" ? value : Number(value);
  if (!Number.isFinite(millis) || millis <= 0) {
    throw new functions.https.HttpsError("invalid-argument", "bookingDate is required");
  }
  return admin.firestore.Timestamp.fromMillis(millis);
}

function servicePrice(service: ServiceCatalogItem): number {
  const price = Number(service.price);
  if (!Number.isFinite(price) || price < 0) {
    throw new functions.https.HttpsError("failed-precondition", "Selected service has an invalid price");
  }
  return price;
}

function findService(
  services: ServiceCatalogItem[],
  serviceId: string,
  serviceName: string,
): ServiceCatalogItem {
  const byId = serviceId ? services.find((service) => stringValue(service.id) === serviceId) : undefined;
  if (byId) return byId;

  const normalizedName = serviceName.toLowerCase();
  const byName = normalizedName
    ? services.find((service) => stringValue(service.name).toLowerCase() === normalizedName)
    : undefined;
  if (byName) return byName;

  throw new functions.https.HttpsError("failed-precondition", "Selected service is no longer available");
}

function findAddOns(
  services: ServiceCatalogItem[],
  addOnIds: string[],
  addOnNames: string[],
): ServiceCatalogItem[] {
  const selected = new Map<string, ServiceCatalogItem>();

  for (const addOnId of addOnIds) {
    const match = services.find((service) => stringValue(service.id) === addOnId);
    if (!match) {
      throw new functions.https.HttpsError("failed-precondition", "One or more add-ons are no longer available");
    }
    selected.set(stringValue(match.id) || stringValue(match.name), match);
  }

  for (const addOnName of addOnNames) {
    const normalizedName = addOnName.toLowerCase();
    const match = services.find((service) => stringValue(service.name).toLowerCase() === normalizedName);
    if (!match) {
      throw new functions.https.HttpsError("failed-precondition", "One or more add-ons are no longer available");
    }
    selected.set(stringValue(match.id) || stringValue(match.name), match);
  }

  return Array.from(selected.values());
}

async function prepareBooking(
  request: CallableRequest,
  bookingId: string,
): Promise<PreparedBooking> {
  if (!request.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  }

  const data = request.data || {};
  const uid = request.auth.uid;
  const stylistId = stringValue(data.stylistId);
  if (!stylistId) {
    throw new functions.https.HttpsError("invalid-argument", "stylistId is required");
  }

  const [stylistDoc, userDoc] = await Promise.all([
    admin.firestore().collection("stylists").doc(stylistId).get(),
    admin.firestore().collection("users").doc(uid).get(),
  ]);
  if (!stylistDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Stylist not found");
  }

  const stylist = stylistDoc.data() || {};
  const services = Array.isArray(stylist.services) ? stylist.services as ServiceCatalogItem[] : [];
  const selectedService = findService(
    services,
    stringValue(data.serviceId),
    stringValue(data.selectedServiceName) || stringValue(data.serviceName),
  );
  const addOns = findAddOns(
    services,
    stringArray(data.addOnServiceIds),
    stringArray(data.addOnServiceNames),
  );

  const groupSize = positiveInteger(data.groupSize, 1, 25);
  const mobileBooking = booleanValue(data.isMobile);
  const eventBooking = booleanValue(data.isEvent);
  const emergencyBooking = booleanValue(data.isEmergencyAsap);
  const serviceBase = servicePrice(selectedService) + addOns.reduce((sum, addOn) => sum + servicePrice(addOn), 0);
  const savedTravelFee = Math.max(Number(stylist.atHomeServiceFee) || 0, 0);
  const travelFeeApplied = mobileBooking
    ? (savedTravelFee > 0 ? savedTravelFee : DEFAULT_AT_HOME_SERVICE_FEE)
    : 0;
  const emergencyFeeApplied = emergencyBooking ? Math.max(Number(stylist.emergencyFee) || 0, 0) : 0;
  const servicePriceTotal = (serviceBase * groupSize) + travelFeeApplied + emergencyFeeApplied;
  if (servicePriceTotal <= 0) {
    throw new functions.https.HttpsError("failed-precondition", "Booking total must be greater than zero");
  }
  let promoDiscountApplied = 0;
  if (stringValue(data.promoCode) === FIRST_BOOKING_PROMO_CODE) {
    const existingBooking = await admin.firestore()
      .collection("bookings")
      .where("customerId", "==", uid)
      .limit(1)
      .get();
    if (existingBooking.empty) {
      promoDiscountApplied = Math.min(FIRST_BOOKING_PROMO_AMOUNT, servicePriceTotal);
    }
  }
  const discountedServicePriceTotal = Number((servicePriceTotal - promoDiscountApplied).toFixed(2));

  const addOnNames = addOns.map((addOn) => stringValue(addOn.name)).filter(Boolean);
  let serviceName = stringValue(selectedService.name);
  if (addOnNames.length > 0) serviceName += ` + ${addOnNames.join(", ")}`;
  if (eventBooking && groupSize > 1) serviceName = `Group of ${groupSize}: ${serviceName}`;

  const bookingDate = timestampFromMillis(data.bookingDate ?? data.date);
  const user = userDoc.data() || {};
  const customerName =
    stringValue(user.name) ||
    stringValue(user.displayName) ||
    stringValue(request.auth.token.name) ||
    "Client";
  const customerPhotoUrl =
    stringValue(user.photoUrl) ||
    stringValue(user.profileImageUrl) ||
    stringValue(request.auth.token.picture);
  const waiverAcceptedVersion = stringValue(data.waiverAcceptedVersion);
  const allergyDisclosureVersion = stringValue(data.allergyDisclosureVersion);
  const depositAmountCents = Math.round(discountedServicePriceTotal * DEPOSIT_RATE * 100);
  const depositAmountDollars = depositAmountCents / 100;

  const bookingData: Record<string, any> = {
    id: bookingId,
    userId: uid,
    customerId: uid,
    customerName,
    customerPhotoUrl,
    stylistId,
    stylistName: stringValue(stylist.name) || stringValue(data.stylistName),
    stylistPhotoUrl: stringValue(stylist.profileImageUrl) || stringValue(stylist.imageUrl),
    stripeAccountId: stringValue(stylist.stripeAccountId),
    serviceId: stringValue(selectedService.id),
    serviceName,
    selectedServiceName: stringValue(selectedService.name),
    addOnServiceIds: addOns.map((addOn) => stringValue(addOn.id)).filter(Boolean),
    addOnServiceNames: addOnNames,
    servicePrice: discountedServicePriceTotal,
    originalServicePrice: servicePriceTotal,
    promoCode: promoDiscountApplied > 0 ? FIRST_BOOKING_PROMO_CODE : "",
    promoDiscountApplied,
    durationMinutes: Number(selectedService.durationMinutes) || 60,
    emergencyFeeApplied,
    travelFeeApplied,
    bookingDate,
    date: bookingDate,
    currency: "usd",
    isMobile: mobileBooking,
    isAtHome: mobileBooking,
    isEvent: eventBooking,
    groupSize,
    eventType: stringValue(data.eventType),
    isSilentAppointment: booleanValue(data.isSilentAppointment),
    isSensoryFriendly: booleanValue(data.isSensoryFriendly),
    notes: stringValue(data.notes),
    waiverAcceptedVersion,
    allergyDisclosureVersion,
    depositAmount: depositAmountDollars,
    status: "pending_payment",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  if (waiverAcceptedVersion) {
    bookingData.waiverAcceptedAt = admin.firestore.FieldValue.serverTimestamp();
  }
  if (allergyDisclosureVersion) {
    bookingData.allergyDisclosureAcceptedAt = admin.firestore.FieldValue.serverTimestamp();
  }

  return {
    bookingData,
    depositAmountCents,
    depositAmountDollars,
    serviceName,
    servicePrice: discountedServicePriceTotal,
    stylistStripeAccountId: stringValue(stylist.stripeAccountId),
    userEmail: stringValue(request.auth.token.email),
  };
}

export const stripeWebhook = onRequest({ secrets: [stripeSecretKey, stripeWebhookSecret], invoker: "public" }, async (req, res) => {
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});
  const sig = req.headers["stripe-signature"] as string;
  let event: Stripe.Event;

  try {
    event = stripe.webhooks.constructEvent(req.rawBody, sig, stripeWebhookSecret.value().trim());
  } catch (err: any) {
    res.status(400).send(`Webhook Error: ${err.message}`);
    return;
  }

  const db = admin.firestore();

  switch (event.type) {
    // Stripe Connect: sync account status when stylist completes onboarding
    case "account.updated": {
      const connectAccount = event.data.object as Stripe.Account;
      const stylistUID = connectAccount.metadata?.firebaseUID;
      if (stylistUID) {
        await db.collection("stylists").doc(stylistUID).set(connectStatusUpdate(connectAccount), { merge: true });
      }
      break;
    }

    case "identity.verification_session.verified":
      const session = event.data.object as Stripe.Identity.VerificationSession;
      const uid = session.metadata?.userId;
      if (uid) {
        const verificationUpdates = {
          verified: true,
          verificationStatus: "VERIFIED"
        };
        await db.collection("users").doc(uid).set(verificationUpdates, { merge: true });

        const stylistDoc = await db.collection("stylists").doc(uid).get();
        if (stylistDoc.exists) {
            await db.collection("stylists").doc(uid).set(verificationUpdates, { merge: true });
        }
        console.log(`User ${uid} identity verified via Stripe.`);
      }
      break;

    case "identity.verification_session.canceled": {
      const sessionCanceled = event.data.object as Stripe.Identity.VerificationSession;
      const uidCanceled = sessionCanceled.metadata?.userId;
      if (uidCanceled) {
        await db.collection("users").doc(uidCanceled).set({
          verificationStatus: "CANCELED",
          verified: false,
        }, { merge: true });
        console.log(`User ${uidCanceled} identity verification session canceled.`);
      }
      break;
    }

    case "identity.verification_session.requires_input": {
      const sessionInput = event.data.object as Stripe.Identity.VerificationSession;
      const uidInput = sessionInput.metadata?.userId;
      if (uidInput) {
        // "requires_input" means Stripe needs more info or the document was rejected.
        // We map this to "FAILED" so the Android VerificationStatus enum recognises it
        // and shows the "Try Again" button. We also store the raw Stripe status for
        // debugging purposes.
        const requiresInputUpdates = {
          verificationStatus: "FAILED",
          verificationStatusRaw: "requires_input",
          verified: false,
        };
        await db.collection("users").doc(uidInput).set(requiresInputUpdates, { merge: true });
        const stylistDocInput = await db.collection("stylists").doc(uidInput).get();
        if (stylistDocInput.exists) {
          await db.collection("stylists").doc(uidInput).set(requiresInputUpdates, { merge: true });
        }
        console.log(`User ${uidInput} identity verification requires input (mapped to FAILED).`);
      }
      break;
    }

    case "identity.verification_session.redacted": {
      const identitySession = event.data.object as Stripe.Identity.VerificationSession;
      const uidIdentity = identitySession.metadata?.userId;
      if (uidIdentity) {
        const verificationStatus = event.type === "identity.verification_session.redacted" ? "REDACTED" : "CANCELLED";
        const verificationUpdates = {
          verified: false,
          isVerified: false,
          verificationStatus,
          verificationUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
        };
        await db.collection("users").doc(uidIdentity).set(verificationUpdates, { merge: true });

        const stylistDoc = await db.collection("stylists").doc(uidIdentity).get();
        if (stylistDoc.exists) {
          await db.collection("stylists").doc(uidIdentity).set(verificationUpdates, { merge: true });
        }
      }
      break;
    }

    case "checkout.session.completed": {
      const checkoutSession = event.data.object as Stripe.Checkout.Session;
      const bookingId = checkoutSession.metadata?.bookingId;
      if (bookingId) {
        await db.collection("bookings").doc(bookingId).update({
          status: "DEPOSIT_PAID",
          checkoutSessionId: checkoutSession.id,
          paidAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        console.log(`Booking ${bookingId} marked DEPOSIT_PAID via checkout.session.completed`);
      }
      break;
    }

    case "checkout.session.expired": {
      const checkoutSession = event.data.object as Stripe.Checkout.Session;
      const bookingId = checkoutSession.metadata?.bookingId;
      if (bookingId) {
        await db.collection("bookings").doc(bookingId).update({
          status: "payment_expired",
          checkoutSessionId: checkoutSession.id,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
      break;
    }

    case "payment_intent.succeeded": {
      const paymentIntent = event.data.object as Stripe.PaymentIntent;
      const bookingId = paymentIntent.metadata?.bookingId;
      if (bookingId) {
        await db.collection("bookings").doc(bookingId).update({
          status: "DEPOSIT_PAID",
          paymentIntentId: paymentIntent.id,
          paidAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        console.log(`Booking ${bookingId} marked DEPOSIT_PAID via payment_intent.succeeded`);
      }
      break;
    }

    case "payment_intent.payment_failed":
    case "payment_intent.canceled": {
      const paymentIntent = event.data.object as Stripe.PaymentIntent;
      const bookingId = paymentIntent.metadata?.bookingId;
      if (bookingId) {
        await db.collection("bookings").doc(bookingId).update({
          status: event.type === "payment_intent.canceled" ? "payment_cancelled" : "payment_failed",
          paymentIntentId: paymentIntent.id,
          paymentFailureMessage: paymentIntent.last_payment_error?.message ?? null,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
      break;
    }

    case "charge.refunded": {
      const charge = event.data.object as Stripe.Charge;
      const paymentIntentId = typeof charge.payment_intent === "string" ? charge.payment_intent : charge.payment_intent?.id;
      const bookingId = stringValue(charge.metadata?.bookingId);
      const bookingQuery = bookingId
        ? null
        : paymentIntentId
          ? await db.collection("bookings").where("paymentIntentId", "==", paymentIntentId).limit(1).get()
          : null;
      const bookingRef = bookingId
        ? db.collection("bookings").doc(bookingId)
        : bookingQuery && !bookingQuery.empty
          ? bookingQuery.docs[0].ref
          : null;

      if (bookingRef) {
        await bookingRef.update({
          status: charge.refunded ? "REFUNDED" : "PARTIALLY_REFUNDED",
          refundedAt: admin.firestore.FieldValue.serverTimestamp(),
          refundedAmountCents: charge.amount_refunded,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
      break;
    }
  }
  res.json({ received: true });
});

export const createIdentityVerificationSession = onCall({ ...CALLABLE_APP_CHECK_OPTIONS, secrets: [stripeSecretKey] }, async (request) => {
    if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
    const stripe = new Stripe(stripeSecretKey.value().trim(), {});

    try {
      const session = await stripe.identity.verificationSessions.create({
        type: "document",
        metadata: {
          userId: request.auth.uid,
        },
      });

      const ephemeralKey = await stripe.ephemeralKeys.create(
        { verification_session: session.id },
        { apiVersion: "2020-08-27;identity_client_api=v7" } as any
      );

      return {
        id: session.id,
        ephemeral_key_secret: ephemeralKey.secret,
        client_secret: ephemeralKey.secret,
        url: session.url ?? null,
      };
    } catch (error: any) {
      console.error("Stripe Identity Error:", error);
      throw new functions.https.HttpsError("internal", error.message);
    }
  });

// createSubscription removed — using Stripe Connect + platform fee model


export const createBookingPaymentIntent = onCall({ ...CALLABLE_APP_CHECK_OPTIONS, secrets: [stripeSecretKey] }, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});

  try {
    const bookingRef = admin.firestore().collection("bookings").doc();
    const prepared = await prepareBooking(request, bookingRef.id);
    if (!prepared.stylistStripeAccountId) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "This stylist has not connected a payout account yet.",
      );
    }

    await retrieveReadyConnectAccount(
      stripe,
      prepared.stylistStripeAccountId,
      prepared.bookingData.stylistId,
    );

    let customer: Stripe.Customer;
    const existing = prepared.userEmail ? await stripe.customers.list({ email: prepared.userEmail, limit: 1 }) : { data: [] };
    if (existing.data.length > 0) customer = existing.data[0];
    else customer = await stripe.customers.create({
      email: prepared.userEmail,
      metadata: { firebaseUID: request.auth.uid },
    });

    const paymentIntentParams: Stripe.PaymentIntentCreateParams = {
      amount: prepared.depositAmountCents,
      currency: "usd",
      customer: customer.id,
      automatic_payment_methods: { enabled: true },
      metadata: {
        stylistId: prepared.bookingData.stylistId,
        userId: request.auth.uid,
        serviceName: prepared.serviceName,
        bookingId: bookingRef.id,
        paymentType: "deposit",
        promoCode: prepared.bookingData.promoCode || "",
        promoDiscountApplied: String(prepared.bookingData.promoDiscountApplied || 0),
      },
      transfer_group: `booking_${bookingRef.id}`,
    };

    let paymentIntent: Stripe.PaymentIntent;
    try {
      paymentIntent = await stripe.paymentIntents.create(paymentIntentParams);
    } catch (error: any) {
      throw friendlyStripeBookingError(error);
    }

    const bookingData = {
      ...prepared.bookingData,
      paymentIntentId: paymentIntent.id,
      payoutStatus: "held_until_customer_confirmation",
    };

    await bookingRef.set(bookingData);

    return {
      bookingId: bookingRef.id,
      clientSecret: paymentIntent.client_secret,
      depositAmount: prepared.depositAmountDollars,
      servicePrice: prepared.servicePrice,
    };
  } catch (error: any) {
    console.error("createBookingPaymentIntent error:", error);
    if (isHttpsError(error)) throw error;
    throw friendlyStripeBookingError(error);
  }
});

export const cancelBooking = onCall({ ...CALLABLE_APP_CHECK_OPTIONS, secrets: [stripeSecretKey] }, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");

  const bookingId = stringValue(request.data?.bookingId);
  if (!bookingId) {
    throw new functions.https.HttpsError("invalid-argument", "bookingId is required");
  }

  const db = admin.firestore();
  const bookingRef = db.collection("bookings").doc(bookingId);
  const bookingSnap = await bookingRef.get();
  if (!bookingSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Booking not found");
  }

  const booking = bookingSnap.data() || {};
  const uid = request.auth.uid;
  const isParticipant = uid === booking.customerId || uid === booking.userId || uid === booking.stylistId;
  if (!isParticipant) {
    throw new functions.https.HttpsError("permission-denied", "You can only cancel your own bookings");
  }

  if (isCancelledStatus(booking.status)) {
    return { status: "CANCELLED", alreadyCancelled: true };
  }

  const stripe = new Stripe(stripeSecretKey.value().trim(), {});
  let refundId: string | null = null;
  let paymentIntentId = stringValue(booking.paymentIntentId);

  if (!paymentIntentId && booking.paidAt && stringValue(booking.checkoutSessionId)) {
    const session = await stripe.checkout.sessions.retrieve(stringValue(booking.checkoutSessionId));
    paymentIntentId = typeof session.payment_intent === "string" ? session.payment_intent : session.payment_intent?.id || "";
  }

  if (booking.paidAt && paymentIntentId) {
    const refund = await stripe.refunds.create(
      {
        payment_intent: paymentIntentId,
        reason: "requested_by_customer",
        metadata: { bookingId, cancelledBy: uid },
      },
      { idempotencyKey: `cancel-booking-${bookingId}` },
    );
    refundId = refund.id;
  }

  await bookingRef.update({
    status: "CANCELLED",
    cancelledAt: admin.firestore.FieldValue.serverTimestamp(),
    cancelledBy: uid,
    refundId,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { status: "CANCELLED", refundId };
});

export const rescheduleBooking = onCall(CALLABLE_APP_CHECK_OPTIONS, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");

  const bookingId = stringValue(request.data?.bookingId);
  if (!bookingId) {
    throw new functions.https.HttpsError("invalid-argument", "bookingId is required");
  }

  const newStartTime = timestampFromMillis(request.data?.startTime ?? request.data?.date);
  const db = admin.firestore();
  const bookingRef = db.collection("bookings").doc(bookingId);
  const bookingSnap = await bookingRef.get();
  if (!bookingSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Booking not found");
  }

  const booking = bookingSnap.data() || {};
  const uid = request.auth.uid;
  const isCustomer = uid === booking.customerId || uid === booking.userId;
  if (!isCustomer) {
    throw new functions.https.HttpsError("permission-denied", "Only the customer can request a reschedule");
  }
  if (isTerminalStatus(booking.status)) {
    throw new functions.https.HttpsError("failed-precondition", "This booking can no longer be rescheduled");
  }

  await bookingRef.update({
    status: "REQUESTED",
    startTime: newStartTime,
    scheduledStart: newStartTime,
    bookingDate: newStartTime,
    date: newStartTime,
    rescheduledAt: admin.firestore.FieldValue.serverTimestamp(),
    rescheduledBy: uid,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { status: "REQUESTED" };
});

export const requestBookingCompletion = onCall(CALLABLE_APP_CHECK_OPTIONS, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");

  const bookingId = stringValue(request.data?.bookingId);
  if (!bookingId) {
    throw new functions.https.HttpsError("invalid-argument", "bookingId is required");
  }

  const db = admin.firestore();
  const bookingRef = db.collection("bookings").doc(bookingId);
  const completionAutoConfirmAt = admin.firestore.Timestamp.fromMillis(Date.now() + 24 * 60 * 60 * 1000);

  await db.runTransaction(async (txn) => {
    const bookingSnap = await txn.get(bookingRef);
    if (!bookingSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Booking not found");
    }

    const booking = bookingSnap.data() || {};
    if (request.auth?.uid !== booking.stylistId) {
      throw new functions.https.HttpsError("permission-denied", "Only the stylist can finish this session");
    }

    const status = normalizedStatus(booking.status);
    if (status === "COMPLETED" || status === "AWAITING_CUSTOMER_CONFIRMATION") return;
    if (!["IN_PROGRESS", "ON_THE_WAY", "DEPOSIT_PAID", "ACCEPTED", "CONFIRMED"].includes(status)) {
      throw new functions.https.HttpsError("failed-precondition", "This booking is not ready to finish");
    }

    txn.update(bookingRef, {
      status: "AWAITING_CUSTOMER_CONFIRMATION",
      completionRequestedAt: admin.firestore.FieldValue.serverTimestamp(),
      completionRequestedBy: request.auth?.uid,
      completionAutoConfirmAt,
      payoutStatus: "pending_customer_confirmation",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { status: "AWAITING_CUSTOMER_CONFIRMATION", autoConfirmAt: completionAutoConfirmAt.toMillis() };
});

export const confirmBookingCompletion = onCall({ ...CALLABLE_APP_CHECK_OPTIONS, secrets: [stripeSecretKey] }, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");

  const bookingId = stringValue(request.data?.bookingId);
  if (!bookingId) {
    throw new functions.https.HttpsError("invalid-argument", "bookingId is required");
  }

  const db = admin.firestore();
  const bookingRef = db.collection("bookings").doc(bookingId);
  let confirmedBooking: Record<string, any> | null = null;

  await db.runTransaction(async (txn) => {
    const bookingSnap = await txn.get(bookingRef);
    if (!bookingSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Booking not found");
    }

    const booking = bookingSnap.data() || {};
    const uid = request.auth?.uid;
    if (uid !== booking.customerId && uid !== booking.userId) {
      throw new functions.https.HttpsError("permission-denied", "Only the customer can confirm completion");
    }

    const status = normalizedStatus(booking.status);
    if (status === "COMPLETED") {
      if (
        !stringValue(booking.payoutTransferId) &&
        !["paid", "released_legacy_destination_charge"].includes(stringValue(booking.payoutStatus))
      ) {
        confirmedBooking = booking;
      }
      return;
    }
    if (status !== "AWAITING_CUSTOMER_CONFIRMATION") {
      throw new functions.https.HttpsError("failed-precondition", "This booking is not awaiting customer confirmation");
    }

    txn.update(bookingRef, {
      status: "COMPLETED",
      customerConfirmedAt: admin.firestore.FieldValue.serverTimestamp(),
      customerConfirmedBy: uid,
      completedAt: admin.firestore.FieldValue.serverTimestamp(),
      payoutStatus: "release_pending",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    confirmedBooking = {
      ...booking,
      status: "COMPLETED",
      customerConfirmedBy: uid,
    };
  });

  if (confirmedBooking) {
    const stripe = new Stripe(stripeSecretKey.value().trim(), {});
    try {
      await releaseStylistPayoutForCompletedBooking(bookingId, confirmedBooking, stripe);
    } catch (error) {
      console.error(`Payout release failed for booking ${bookingId}; scheduled retry can pick it up.`, error);
    }
  }
  await awardLoyaltyPointsForCompletedBooking(bookingId);
  return { status: "COMPLETED" };
});

export const disputeBookingCompletion = onCall(CALLABLE_APP_CHECK_OPTIONS, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");

  const bookingId = stringValue(request.data?.bookingId);
  if (!bookingId) {
    throw new functions.https.HttpsError("invalid-argument", "bookingId is required");
  }

  const reason = stringValue(request.data?.reason) || "Customer disputed session completion";
  const db = admin.firestore();
  const bookingRef = db.collection("bookings").doc(bookingId);

  await db.runTransaction(async (txn) => {
    const bookingSnap = await txn.get(bookingRef);
    if (!bookingSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Booking not found");
    }

    const booking = bookingSnap.data() || {};
    const uid = request.auth?.uid;
    if (uid !== booking.customerId && uid !== booking.userId) {
      throw new functions.https.HttpsError("permission-denied", "Only the customer can dispute completion");
    }

    if (normalizedStatus(booking.status) !== "AWAITING_CUSTOMER_CONFIRMATION") {
      throw new functions.https.HttpsError("failed-precondition", "This booking is not awaiting customer confirmation");
    }

    txn.update(bookingRef, {
      status: "COMPLETION_DISPUTED",
      disputeReason: reason.slice(0, 500),
      disputedAt: admin.firestore.FieldValue.serverTimestamp(),
      disputedBy: uid,
      payoutStatus: "paused_dispute",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });

  return { status: "COMPLETION_DISPUTED" };
});

export const autoConfirmPendingBookingCompletions = onSchedule(
  { schedule: "every 30 minutes", secrets: [stripeSecretKey] },
  async () => {
  const db = admin.firestore();
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});
  const now = admin.firestore.Timestamp.now();
  const snapshot = await db.collection("bookings")
    .where("status", "==", "AWAITING_CUSTOMER_CONFIRMATION")
    .where("completionAutoConfirmAt", "<=", now)
    .limit(100)
    .get();

  await Promise.all(snapshot.docs.map(async (doc) => {
    const booking = doc.data();
    await doc.ref.update({
      status: "COMPLETED",
      autoConfirmedAt: admin.firestore.FieldValue.serverTimestamp(),
      completedAt: admin.firestore.FieldValue.serverTimestamp(),
      payoutStatus: "release_pending",
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    try {
      await releaseStylistPayoutForCompletedBooking(
        doc.id,
        { ...booking, status: "COMPLETED", autoConfirmedAt: true },
        stripe,
      );
    } catch (error) {
      console.error(`Auto-confirmed booking ${doc.id}, but payout release failed.`, error);
    }
    await awardLoyaltyPointsForCompletedBooking(doc.id);
  }));

  const pendingReleaseSnapshot = await db.collection("bookings")
    .where("status", "==", "COMPLETED")
    .where("payoutStatus", "==", "release_pending")
    .limit(100)
    .get();

  await Promise.all(pendingReleaseSnapshot.docs.map(async (doc) => {
    try {
      await releaseStylistPayoutForCompletedBooking(doc.id, doc.data(), stripe);
    } catch (error) {
      console.error(`Scheduled payout release retry failed for booking ${doc.id}.`, error);
    }
  }));

  console.log(
    `Auto-confirmed ${snapshot.size} pending booking completions; retried ${pendingReleaseSnapshot.size} pending payout releases.`,
  );
});


// --- STRIPE CHECKOUT (SAFARI / WEB) ---

export const createCheckoutSession = onCall({ ...CALLABLE_APP_CHECK_OPTIONS, invoker: "public", secrets: [stripeSecretKey] }, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});

  try {
    const bookingRef = admin.firestore().collection("bookings").doc();
    const bookingId = bookingRef.id;
    const prepared = await prepareBooking(request, bookingId);
    if (!prepared.stylistStripeAccountId) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "This stylist has not connected a payout account yet.",
      );
    }

    await retrieveReadyConnectAccount(
      stripe,
      prepared.stylistStripeAccountId,
      prepared.bookingData.stylistId,
    );

    const sessionParams: Stripe.Checkout.SessionCreateParams = {
      payment_method_types: ["card"],
      line_items: [{
        price_data: {
          currency: "usd",
          product_data: {
            name: `Deposit: ${prepared.serviceName}`,
            description: `20% deposit for ${prepared.serviceName} with ${prepared.bookingData.stylistName}`,
          },
          unit_amount: prepared.depositAmountCents,
        },
        quantity: 1,
      }],
      mode: "payment",
      success_url: `https://us-central1-refreshme-74f79.cloudfunctions.net/stripeSuccess?bookingId=${bookingId}`,
      cancel_url: `https://us-central1-refreshme-74f79.cloudfunctions.net/stripeCancel?bookingId=${bookingId}`,
      metadata: { bookingId, userId: request.auth.uid, stylistId: prepared.bookingData.stylistId },
    };

    sessionParams.payment_intent_data = {
      transfer_group: `booking_${bookingId}`,
      metadata: {
        bookingId,
        userId: request.auth.uid,
        stylistId: prepared.bookingData.stylistId,
        paymentType: "deposit",
      },
    };

    let session: Stripe.Checkout.Session;
    try {
      session = await stripe.checkout.sessions.create(sessionParams);
    } catch (error: any) {
      throw friendlyStripeBookingError(error);
    }

    const bookingData = {
      ...prepared.bookingData,
      checkoutSessionId: session.id,
      payoutStatus: "held_until_customer_confirmation",
    };

    await bookingRef.set(bookingData);

    return {
      checkoutUrl: session.url,
      bookingId,
      sessionId: session.id,
      depositAmount: prepared.depositAmountDollars,
      servicePrice: prepared.servicePrice,
    };
  } catch (error: any) {
    console.error("createCheckoutSession error:", error);
    if (isHttpsError(error)) throw error;
    throw friendlyStripeBookingError(error);
  }
});

export const stripeSuccess = legacyFunctions.https.onRequest(async (req, res) => {
  const bookingId = req.query.bookingId as string;
  if (!bookingId) { res.status(400).send("Missing bookingId"); return; }

  try {
    const safeBookingId = encodeURIComponent(bookingId);

    const html = `<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <meta http-equiv="refresh" content="2;url=refreshme://payment-success?bookingId=${safeBookingId}">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Payment Successful</title>
    <style>
      body { font-family: -apple-system, sans-serif; text-align: center; padding: 60px 20px; background: #f5f5f5; }
      .card { background: white; border-radius: 16px; padding: 40px; max-width: 400px; margin: 0 auto; box-shadow: 0 2px 20px rgba(0,0,0,0.1); }
      .icon { font-size: 60px; margin-bottom: 16px; }
      h1 { color: #1a1a1a; font-size: 24px; margin-bottom: 8px; }
      p { color: #666; font-size: 16px; }
      a { color: #6B46C1; }
    </style>
  </head>
  <body>
    <div class="card">
      <div class="icon">✅</div>
      <h1>Payment Successful!</h1>
      <p>Your payment is being confirmed. Returning to RefreshMe...</p>
      <p><a href="refreshme://payment-success?bookingId=${safeBookingId}">Tap here if not redirected</a></p>
    </div>
  </body>
</html>`;
    res.status(200).send(html);
  } catch (error: any) {
    console.error("stripeSuccess error:", error);
    res.status(500).send("Internal server error");
  }
});

export const stripeCancel = legacyFunctions.https.onRequest(async (req, res) => {
  const bookingId = req.query.bookingId as string;
  if (!bookingId) { res.status(400).send("Missing bookingId"); return; }

  try {
    const safeBookingId = encodeURIComponent(bookingId);

    const html = `<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <meta http-equiv="refresh" content="2;url=refreshme://payment-cancelled?bookingId=${safeBookingId}">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Payment Cancelled</title>
    <style>
      body { font-family: -apple-system, sans-serif; text-align: center; padding: 60px 20px; background: #f5f5f5; }
      .card { background: white; border-radius: 16px; padding: 40px; max-width: 400px; margin: 0 auto; box-shadow: 0 2px 20px rgba(0,0,0,0.1); }
      .icon { font-size: 60px; margin-bottom: 16px; }
      h1 { color: #1a1a1a; font-size: 24px; margin-bottom: 8px; }
      p { color: #666; font-size: 16px; }
      a { color: #6B46C1; }
    </style>
  </head>
  <body>
    <div class="card">
      <div class="icon">❌</div>
      <h1>Payment Cancelled</h1>
      <p>Your payment was cancelled. Returning to RefreshMe...</p>
      <p><a href="refreshme://payment-cancelled?bookingId=${safeBookingId}">Tap here if not redirected</a></p>
    </div>
  </body>
</html>`;
    res.status(200).send(html);
  } catch (error: any) {
    console.error("stripeCancel error:", error);
    res.status(500).send("Internal server error");
  }
});

// --- STRIPE CONNECT ONBOARDING ---

export const createConnectAccount = onCall({ ...CALLABLE_APP_CHECK_OPTIONS, invoker: "public", secrets: [stripeSecretKey] }, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});
  const userId = request.auth.uid;
  console.log("createConnectAccount: invoked", { userId, email: request.auth.token.email });

  try {
    const stylistRef = admin.firestore().collection("stylists").doc(userId);
    const stylistDoc = await stylistRef.get();
    let accountId = stylistDoc.data()?.stripeAccountId as string | undefined;
    console.log("createConnectAccount: stylistDoc loaded", { userId, hadExistingAccount: !!accountId, accountId });

    if (!accountId) {
      const userEmail = request.auth.token.email;
      const account = await stripe.accounts.create({
        type: "express",
        email: userEmail,
        metadata: { firebaseUID: userId },
        capabilities: { card_payments: { requested: true }, transfers: { requested: true } },
      });
      accountId = account.id;
      console.log("createConnectAccount: created new Stripe account", { userId, accountId });
      await stylistRef.set({
        stripeAccountId: accountId,
        stripeAccountStatus: "pending",
        stripeDetailsSubmitted: false,
        stripeOnboardingComplete: false,
      }, { merge: true });
    }

    const accountLink = await stripe.accountLinks.create({
      account: accountId,
      refresh_url: "https://refreshme-74f79.web.app/connect-refresh",
      return_url: "https://refreshme-74f79.web.app/connect-return",
      type: "account_onboarding",
    });

    const url = accountLink?.url ?? "";
    if (!url) {
      console.error("createConnectAccount: Stripe returned empty url", { userId, accountId, accountLink });
      throw new functions.https.HttpsError("internal", "Stripe returned empty onboarding URL");
    }
    console.log("createConnectAccount: returning onboarding link", { userId, accountId, urlLength: url.length });
    return { url, accountId };
  } catch (error: any) {
    console.error("createConnectAccount error:", {
      userId,
      message: error?.message,
      type: error?.type,
      code: error?.code,
      statusCode: error?.statusCode,
      raw: error?.raw,
    });
    throw new functions.https.HttpsError("internal", error?.message ?? "Unknown error in createConnectAccount");
  }
});

export const getConnectAccountStatus = onCall({ ...CALLABLE_APP_CHECK_OPTIONS, invoker: "public", secrets: [stripeSecretKey] }, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});
  const userId = request.auth.uid;

  try {
    const stylistDoc = await admin.firestore().collection("stylists").doc(userId).get();
    const accountId = stylistDoc.data()?.stripeAccountId as string | undefined;

    if (!accountId) {
      return {
        status: "not_connected",
        accountId: null,
        chargesEnabled: false,
        payoutsEnabled: false,
        detailsSubmitted: false,
      };
    }

    const account = await stripe.accounts.retrieve(accountId);
    const update = connectStatusUpdate(account);

    // Sync latest status to Firestore
    await admin.firestore().collection("stylists").doc(userId).set(update, { merge: true });

    return {
      status: update.stripeAccountStatus,
      accountId,
      chargesEnabled: account.charges_enabled,
      payoutsEnabled: account.payouts_enabled,
      detailsSubmitted: account.details_submitted,
    };
  } catch (error: any) {
    throw new functions.https.HttpsError("internal", error.message);
  }
});

function buildVirtualTryOnPrompt(prompt: string): string {
  return [
    prompt.trim(),
    "Photorealistic mobile selfie edit.",
    "Change only the hairstyle and visible hair color/texture requested.",
    "Preserve the exact same face, identity, skin texture, expression, eyes, nose, mouth, jawline, pose, body, clothing, background, camera angle, and lighting.",
    "Natural salon-quality hair with realistic strands, hairline, shadows, and blending.",
    "No beauty filter, no face retouching, no face swap, no age change.",
  ].join(" ");
}

export const runVirtualTryOn = onCall({
  ...VIRTUAL_TRY_ON_APP_CHECK_OPTIONS,
  secrets: [replicateApiToken],
  timeoutSeconds: 180,
  memory: "1GiB",
}, async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");

  const rawImage = stringValue(request.data?.image) || stringValue(request.data?.base64Image);
  const image = rawImage.startsWith("data:")
    ? rawImage
    : rawImage
      ? `data:image/jpeg;base64,${rawImage}`
      : "";
  const prompt = stringValue(request.data?.prompt);
  const modelVersion = stringValue(request.data?.modelVersion) || DEFAULT_VIRTUAL_TRY_ON_MODEL_VERSION;
  const negativePrompt = stringValue(request.data?.negativePrompt) ||
    "nsfw, lowres, bad anatomy, deformed face, altered face, different person, face swap, changed identity, changed eyes, changed nose, changed mouth, changed jaw, plastic skin, airbrushed skin, beauty filter, doll, cartoon, illustration, 3d render, painting, text, watermark, blurry, jpeg artifacts";

  const missingFields = [
    !image ? "base64Image" : "",
    !prompt ? "prompt" : "",
    !modelVersion ? "modelVersion" : "",
  ].filter(Boolean);
  if (missingFields.length > 0) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `Missing required fields: ${missingFields.join(", ")}`,
    );
  }

  const createResponse = await fetch("https://api.replicate.com/v1/predictions", {
    method: "POST",
    headers: {
      "Authorization": `Token ${replicateApiToken.value().trim()}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      version: modelVersion,
      input: {
        image,
        input_image: image,
        prompt: buildVirtualTryOnPrompt(prompt),
        negative_prompt: negativePrompt,
        prompt_strength: 0.38,
        style_strength_ratio: 12,
        num_outputs: 1,
      },
    }),
  });

  const createBody = await createResponse.json() as any;
  if (!createResponse.ok) {
    console.error("runVirtualTryOn create failed:", createBody);
    throw new functions.https.HttpsError("internal", "Could not start virtual try-on");
  }

  let prediction = createBody;
  for (let attempt = 0; attempt < 40; attempt += 1) {
    const status = stringValue(prediction.status);
    if (status === "succeeded") {
      const output = prediction.output;
      const outputUrl = Array.isArray(output) ? stringValue(output[0]) : stringValue(output);
      if (!outputUrl) {
        throw new functions.https.HttpsError("internal", "Virtual try-on finished without an image");
      }
      return { outputUrl };
    }
    if (status === "failed" || status === "canceled") {
      console.error("runVirtualTryOn prediction failed:", prediction.error);
      throw new functions.https.HttpsError("internal", "Virtual try-on failed");
    }

    await new Promise((resolve) => setTimeout(resolve, 3000));
    const getUrl = stringValue(prediction.urls?.get);
    if (!getUrl) break;
    const pollResponse = await fetch(getUrl, {
      headers: { "Authorization": `Token ${replicateApiToken.value().trim()}` },
    });
    prediction = await pollResponse.json();
  }

  throw new functions.https.HttpsError("deadline-exceeded", "Virtual try-on timed out");
});

export const summarizeStylistReviews = onCall(CALLABLE_APP_CHECK_OPTIONS, async (request) => {
    if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
    const { stylistId } = request.data;
    const summaryRef = admin.firestore().collection("stylists").doc(stylistId).collection("aiSummary").doc("current");

    try {
        const reviewsSnapshot = await admin.firestore().collection("reviews").where("stylistId", "==", stylistId).limit(50).get();
        if (reviewsSnapshot.empty) return { summary: "No reviews yet." };

        const reviewsText = reviewsSnapshot.docs.map(doc => `- ${doc.data().text}`).join("\n");
        const prompt = `Analyze these reviews and provide a structred summary with a Vibe Check and 3 Strengths:\n${reviewsText}`;

        const resp = await generativeModel.generateContent(prompt);
        const text = resp?.response?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";

        await summaryRef.set({ summary: text, updatedAt: admin.firestore.FieldValue.serverTimestamp() });
        return { summary: text };
    } catch (error) {
        throw new functions.https.HttpsError("internal", "Failed to generate summary.");
    }
});

// onAppointmentCancelled - notifies waitlisted users when a booking is deleted
export const onAppointmentCancelled = onDocumentDeleted(
  "bookings/{bookingId}",
  async (event) => {
    const eventData = event.data;
    if (!eventData) return;

    const deletedBooking = eventData.data();
    const stylistId = deletedBooking.stylistId;

    // Fallback to "date" if "dateTime" is not present, depending on how you store dates.
    const bookingTimestamp = deletedBooking.dateTime || deletedBooking.date;
    if (!bookingTimestamp) return;

    const bookingDate: Date = bookingTimestamp.toDate();
    const targetDate = new Date(
      bookingDate.getFullYear(),
      bookingDate.getMonth(),
      bookingDate.getDate()
    );

    // Get the start and end of the target date in milliseconds
    const startOfDay = targetDate.getTime();
    const endOfDay = startOfDay + 24 * 60 * 60 * 1000;

    // Query the waitlist for the corresponding stylist and date
    const waitlistSnapshot = await admin.firestore().collection("waitlists")
      .where("stylistId", "==", stylistId)
      .where("targetDate", ">=", startOfDay)
      .where("targetDate", "<", endOfDay)
      .get();

    if (waitlistSnapshot.empty) {
      console.log(`No waitlisted users found for stylist ${stylistId} on ${targetDate}`);
      return;
    }

    // Notify all matched users
    const notifications = waitlistSnapshot.docs.map(doc => {
      const waitlistEntry = doc.data();
      return sendPushNotification(
        waitlistEntry.userId,
        "Waitlist Alert! ??",
        "An appointment just opened up for your requested date. Book now before it's gone!"
      );
    });

    await Promise.all(notifications);
  }
);

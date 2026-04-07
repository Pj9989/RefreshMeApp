import * as functions from "firebase-functions/v2";
import * as legacyFunctions from "firebase-functions";
import * as admin from "firebase-admin";
import { onRequest } from "firebase-functions/v2/https";
import { onCall } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import Stripe from "stripe";
import { onDocumentCreated, onDocumentUpdated, onDocumentDeleted } from "firebase-functions/v2/firestore";
import { VertexAI } from "@google-cloud/vertexai";

admin.initializeApp();

const stripeSecretKey = defineSecret("STRIPE_SECRET_KEY");
const stripeWebhookSecret = defineSecret("STRIPE_WEBHOOK_SECRET");

// Deposit rate constant (20%)
const DEPOSIT_RATE = 0.20;
export const BUILD_ENV = "live";

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
                    channelId: "refresh_me_notifications",
                }
            }
        };

        await admin.messaging().send(message);
        console.log(`Notification sent to user ${userId}`);
    } catch (error) {
        console.error(`Error sending notification to user ${userId}:`, error);
    }
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
        } else if (newData.status === "DEPOSIT_PAID" || newData.status === "paid") {
            // Stylist might want to know
            await sendPushNotification(
                newData.stylistId,
                "Deposit Paid! ??",
                `The deposit for ${newData.customerName}'s appointment has been confirmed.`,
                { type: "booking", targetId: event.params.bookingId }
            );
            return;
        }

        await sendPushNotification(userId, title, body, {
            type: "booking",
            targetId: event.params.bookingId
        });
    }
});

// 4. NEW MESSAGE NOTIFICATION
export const onMessageCreated = onDocumentCreated("chats/{chatId}/messages/{messageId}", async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const message = snapshot.data();

    const receiverId = message.receiverId;
    const senderId = message.senderId;

    // Fetch sender name
    let senderName = "Someone";
    try {
        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        if (!senderDoc.exists) {
            // Check stylists collection if not in users
            const stylistDoc = await admin.firestore().collection("stylists").doc(senderId).get();
            senderName = stylistDoc.data()?.name || "Someone";
        } else {
            senderName = senderDoc.data()?.name || "Someone";
        }
    } catch (e) {
        console.warn("Could not fetch sender name for notification");
    }

    await sendPushNotification(
        receiverId,
        `New Message from ${senderName}`,
        message.type === "IMAGE" ? "?? Sent a photo" : message.text,
        {
            type: "chat",
            targetId: senderId,
            imageUrl: message.imageUrl || ""
        }
    );
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

export const stripeWebhook = onRequest(async (req, res) => {
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
        await db.collection("stylists").doc(stylistUID).set({
          stripeAccountId: connectAccount.id,
          stripeChargesEnabled: connectAccount.charges_enabled,
          stripePayoutsEnabled: connectAccount.payouts_enabled,
          stripeOnboardingComplete: connectAccount.charges_enabled && connectAccount.payouts_enabled,
        }, { merge: true });
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

    case "identity.verification_session.requires_input":
      const sessionInput = event.data.object as Stripe.Identity.VerificationSession;
      const uidInput = sessionInput.metadata?.userId;
      if (uidInput) {
        await db.collection("users").doc(uidInput).set({
            verificationStatus: "REQUIRES_INPUT"
        }, { merge: true });
      }
      break;
  }
  res.json({ received: true });
});

export const createIdentityVerificationSession = onCall(async (request) => {
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
        { apiVersion: "2023-10-16" } as any
      );

      return {
        id: session.id,
        client_secret: ephemeralKey.secret,
      };
    } catch (error: any) {
      console.error("Stripe Identity Error:", error);
      throw new functions.https.HttpsError("internal", error.message);
    }
  });

// createSubscription removed — using Stripe Connect + platform fee model


export const createBookingPaymentIntent = onCall(async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});

  const {
      stylistId,
      userId,
      serviceName,
      servicePrice,
      bookingDate
  } = request.data;

  try {
    const userEmail = request.auth.token.email;
    const depositAmount = Math.round(servicePrice * DEPOSIT_RATE * 100);

    let customer: Stripe.Customer;
    const existing = await stripe.customers.list({ email: userEmail, limit: 1 });
    if (existing.data.length > 0) customer = existing.data[0];
    else customer = await stripe.customers.create({ email: userEmail as string, metadata: { firebaseUID: userId } });

    // Look up stylist's connected Stripe account for destination charges
    const stylistDoc = await admin.firestore().collection("stylists").doc(stylistId).get();
    const stylistStripeAccountId = stylistDoc.data()?.stripeAccountId as string | undefined;

    const paymentIntentParams: Stripe.PaymentIntentCreateParams = {
      amount: depositAmount,
      currency: "usd",
      customer: customer.id,
      metadata: { stylistId, userId, serviceName, paymentType: "deposit" },
    };

    if (stylistStripeAccountId) {
      // Destination charge: 10% platform fee, rest goes to stylist automatically
      paymentIntentParams.application_fee_amount = Math.round(depositAmount * PLATFORM_FEE_PERCENT);
      paymentIntentParams.transfer_data = { destination: stylistStripeAccountId };
    }

    const paymentIntent = await stripe.paymentIntents.create(paymentIntentParams);

    const bookingRef = admin.firestore().collection("bookings").doc();

    const bookingData = {
        ...request.data, // Include all fields sent by Android
        id: bookingRef.id,
        depositAmount: depositAmount / 100,
        date: bookingDate ? admin.firestore.Timestamp.fromMillis(bookingDate) : admin.firestore.FieldValue.serverTimestamp(),
        status: "pending_payment",
        paymentIntentId: paymentIntent.id,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
    };

    await bookingRef.set(bookingData);

    return { bookingId: bookingRef.id, clientSecret: paymentIntent.client_secret, depositAmount: depositAmount / 100 };
  } catch (error: any) {
    throw new functions.https.HttpsError("internal", error.message);
  }
});


// --- STRIPE CONNECT ONBOARDING ---

export const createConnectAccount = onCall(async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});
  const userId = request.auth.uid;

  try {
    const stylistRef = admin.firestore().collection("stylists").doc(userId);
    const stylistDoc = await stylistRef.get();
    let accountId = stylistDoc.data()?.stripeAccountId as string | undefined;

    if (!accountId) {
      const userEmail = request.auth.token.email;
      const account = await stripe.accounts.create({
        type: "express",
        email: userEmail,
        metadata: { firebaseUID: userId },
        capabilities: { card_payments: { requested: true }, transfers: { requested: true } },
      });
      accountId = account.id;
      await stylistRef.set({ stripeAccountId: accountId, stripeOnboardingComplete: false }, { merge: true });
    }

    const accountLink = await stripe.accountLinks.create({
      account: accountId,
      refresh_url: "https://refreshme-74f79.web.app/connect-refresh",
      return_url: "https://refreshme-74f79.web.app/connect-return",
      type: "account_onboarding",
    });

    return { url: accountLink.url, accountId };
  } catch (error: any) {
    console.error("createConnectAccount error:", error);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

export const getConnectAccountStatus = onCall(async (request) => {
  if (!request.auth) throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  const stripe = new Stripe(stripeSecretKey.value().trim(), {});
  const userId = request.auth.uid;

  try {
    const stylistDoc = await admin.firestore().collection("stylists").doc(userId).get();
    const accountId = stylistDoc.data()?.stripeAccountId as string | undefined;

    if (!accountId) return { status: "not_connected", accountId: null, chargesEnabled: false };

    const account = await stripe.accounts.retrieve(accountId);
    const status = account.charges_enabled ? "active" : "pending";

    // Sync latest status to Firestore
    await admin.firestore().collection("stylists").doc(userId).set({
      stripeChargesEnabled: account.charges_enabled,
      stripePayoutsEnabled: account.payouts_enabled,
      stripeOnboardingComplete: account.charges_enabled && account.payouts_enabled,
    }, { merge: true });

    return { status, accountId, chargesEnabled: account.charges_enabled, payoutsEnabled: account.payouts_enabled };
  } catch (error: any) {
    throw new functions.https.HttpsError("internal", error.message);
  }
});

export const summarizeStylistReviews = onCall(async (request) => {
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

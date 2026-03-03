import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { onRequest } from "firebase-functions/v2/https";
import { onCall } from "firebase-functions/v2/https";
import { defineString } from "firebase-functions/params";
import Stripe from "stripe";

admin.initializeApp();

const stripeSecretKey = defineString("STRIPE_SECRET_KEY");
const stripeWebhookSecret = defineString("STRIPE_WEBHOOK_SECRET");

// ---------------------------------------------------------------------------
// Helper: get a Stripe instance
// ---------------------------------------------------------------------------
function getStripe(): Stripe {
  return new Stripe(stripeSecretKey.value(), {
    apiVersion: "2025-12-15.clover",
  });
}

// ---------------------------------------------------------------------------
// stripeWebhook – handles subscription events AND identity verification events
// ---------------------------------------------------------------------------
export const stripeWebhook = onRequest(async (req, res) => {
  const stripe = getStripe();
  const sig = req.headers["stripe-signature"] as string;
  let event: Stripe.Event;

  try {
    event = stripe.webhooks.constructEvent(
      req.rawBody,
      sig,
      stripeWebhookSecret.value()
    );
  } catch (err: any) {
    res.status(400).send(`Webhook Error: ${err.message}`);
    return;
  }

  switch (event.type) {
    // -----------------------------------------------------------------------
    // Subscription events
    // -----------------------------------------------------------------------
    case "customer.subscription.created":
    case "customer.subscription.updated": {
      const subscription = event.data.object as Stripe.Subscription;
      const customerId = subscription.customer as string;
      const customer = await stripe.customers.retrieve(customerId);
      const firebaseUID = (customer as Stripe.Customer).metadata?.firebaseUID;

      if (firebaseUID) {
        await admin.firestore().collection("users").doc(firebaseUID).set(
          {
            stripeCustomerId: customerId,
            subscriptionId: subscription.id,
            subscriptionStatus: subscription.status,
            isSubscribed: subscription.status === "active",
          },
          { merge: true }
        );
      }
      break;
    }

    case "customer.subscription.deleted": {
      const deletedSubscription = event.data.object as Stripe.Subscription;
      const deletedCustomerId = deletedSubscription.customer as string;
      const deletedCustomer = await stripe.customers.retrieve(deletedCustomerId);
      const deletedFirebaseUID = (deletedCustomer as Stripe.Customer).metadata?.firebaseUID;

      if (deletedFirebaseUID) {
        await admin.firestore().collection("users").doc(deletedFirebaseUID).set(
          { subscriptionStatus: "canceled", isSubscribed: false },
          { merge: true }
        );
      }
      break;
    }

    case "invoice.payment_succeeded": {
      const invoice = event.data.object as Stripe.Invoice;
      const invoiceCustomerId = invoice.customer as string;
      const invoiceSubscriptionId = (invoice as any).subscription as string | null;

      if (invoiceSubscriptionId) {
        const invoiceCustomer = await stripe.customers.retrieve(invoiceCustomerId);
        const invoiceFirebaseUID = (invoiceCustomer as Stripe.Customer).metadata?.firebaseUID;

        if (invoiceFirebaseUID) {
          await admin.firestore().collection("users").doc(invoiceFirebaseUID).set(
            { subscriptionStatus: "active", isSubscribed: true },
            { merge: true }
          );
        }
      }
      break;
    }

    case "invoice.payment_failed": {
      const failedInvoice = event.data.object as Stripe.Invoice;
      const failedCustomerId = failedInvoice.customer as string;
      const failedCustomer = await stripe.customers.retrieve(failedCustomerId);
      const failedFirebaseUID = (failedCustomer as Stripe.Customer).metadata?.firebaseUID;

      if (failedFirebaseUID) {
        await admin.firestore().collection("users").doc(failedFirebaseUID).set(
          { subscriptionStatus: "past_due", isSubscribed: false },
          { merge: true }
        );
      }
      break;
    }

    // -----------------------------------------------------------------------
    // Stripe Identity events
    // -----------------------------------------------------------------------
    case "identity.verification_session.verified": {
      // The user's identity has been successfully verified.
      const session = event.data.object as Stripe.Identity.VerificationSession;
      const firebaseUID = session.metadata?.firebaseUID;

      if (firebaseUID) {
        const verifiedData = {
          verified: true,
          verificationStatus: "verified",
          verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
          stripeVerificationSessionId: session.id,
        };

        // Update both the users document and the stylists document (if it exists)
        const batch = admin.firestore().batch();
        batch.set(
          admin.firestore().collection("users").doc(firebaseUID),
          verifiedData,
          { merge: true }
        );
        batch.set(
          admin.firestore().collection("stylists").doc(firebaseUID),
          verifiedData,
          { merge: true }
        );
        await batch.commit();

        console.log(`Identity verified for Firebase UID: ${firebaseUID}`);
      }
      break;
    }

    case "identity.verification_session.requires_input": {
      // Verification failed or needs more input (e.g., document unreadable).
      const session = event.data.object as Stripe.Identity.VerificationSession;
      const firebaseUID = session.metadata?.firebaseUID;

      if (firebaseUID) {
        const failedData = {
          verified: false,
          verificationStatus: "failed",
          stripeVerificationSessionId: session.id,
        };

        const batch = admin.firestore().batch();
        batch.set(
          admin.firestore().collection("users").doc(firebaseUID),
          failedData,
          { merge: true }
        );
        batch.set(
          admin.firestore().collection("stylists").doc(firebaseUID),
          failedData,
          { merge: true }
        );
        await batch.commit();

        console.log(`Identity verification failed for Firebase UID: ${firebaseUID}`);
      }
      break;
    }

    case "identity.verification_session.canceled": {
      const session = event.data.object as Stripe.Identity.VerificationSession;
      const firebaseUID = session.metadata?.firebaseUID;

      if (firebaseUID) {
        const canceledData = {
          verified: false,
          verificationStatus: "canceled",
          stripeVerificationSessionId: session.id,
        };

        const batch = admin.firestore().batch();
        batch.set(
          admin.firestore().collection("users").doc(firebaseUID),
          canceledData,
          { merge: true }
        );
        batch.set(
          admin.firestore().collection("stylists").doc(firebaseUID),
          canceledData,
          { merge: true }
        );
        await batch.commit();
      }
      break;
    }

    default:
      console.log(`Unhandled event type: ${event.type}`);
  }

  res.json({ received: true, type: event.type });
});

// ---------------------------------------------------------------------------
// createIdentityVerificationSession – called from the Android app to start
// a Stripe Identity verification session for the current user.
// ---------------------------------------------------------------------------
export const createIdentityVerificationSession = onCall(async (request) => {
  if (!request.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated to start identity verification."
    );
  }

  const stripe = getStripe();
  const firebaseUID = request.auth.uid;

  try {
    const session = await stripe.identity.verificationSessions.create({
      type: "document",
      metadata: {
        // Store the Firebase UID so the webhook can look up the user
        firebaseUID: firebaseUID,
      },
      options: {
        document: {
          // Accept driving license, passport, and ID card
          allowed_types: ["driving_license", "passport", "id_card"],
          require_live_capture: true,
          require_matching_selfie: true,
        },
      },
    });

    // Mark verification as pending in Firestore immediately
    const pendingData = {
      verificationStatus: "pending",
      stripeVerificationSessionId: session.id,
    };

    const batch = admin.firestore().batch();
    batch.set(
      admin.firestore().collection("users").doc(firebaseUID),
      pendingData,
      { merge: true }
    );
    batch.set(
      admin.firestore().collection("stylists").doc(firebaseUID),
      pendingData,
      { merge: true }
    );
    await batch.commit();

    // Return the ephemeral key client secret to the Android SDK
    return {
      verificationSessionId: session.id,
      ephemeralKeySecret: session.client_secret,
    };
  } catch (error: any) {
    console.error("Error creating identity verification session:", error);
    throw new functions.https.HttpsError(
      "internal",
      `Failed to create verification session: ${error.message}`
    );
  }
});

// ---------------------------------------------------------------------------
// createSubscription – unchanged from original
// ---------------------------------------------------------------------------
export const createSubscription = onCall(async (request) => {
  if (!request.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated"
    );
  }

  const stripe = getStripe();
  const { userId, priceId } = request.data;

  if (!userId || !priceId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Missing required parameters: userId and priceId"
    );
  }

  try {
    const userEmail = request.auth.token.email;

    if (!userEmail) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "User email not found"
      );
    }

    let customer: Stripe.Customer;
    const existingCustomers = await stripe.customers.list({
      email: userEmail,
      limit: 1,
    });

    if (existingCustomers.data.length > 0) {
      customer = existingCustomers.data[0];
    } else {
      customer = await stripe.customers.create({
        email: userEmail,
        metadata: { firebaseUID: userId },
      });
    }

    const subscription = await stripe.subscriptions.create({
      customer: customer.id,
      items: [{ price: priceId }],
      payment_behavior: "default_incomplete",
      payment_settings: { save_default_payment_method: "on_subscription" },
      expand: ["latest_invoice.payment_intent"],
    });

    const invoice = subscription.latest_invoice as Stripe.Invoice;
    const confirmationSecret = (invoice as any).confirmation_secret as { client_secret?: string } | null;
    const paymentIntent = (invoice as any).payment_intent as Stripe.PaymentIntent | null;

    await admin.firestore().collection("users").doc(userId).set(
      {
        stripeCustomerId: customer.id,
        subscriptionId: subscription.id,
        subscriptionStatus: subscription.status,
      },
      { merge: true }
    );

    // Support both old payment_intent expand and new confirmation_secret pattern
    const clientSecret =
      paymentIntent?.client_secret ??
      confirmationSecret?.client_secret ??
      null;

    return {
      clientSecret,
      subscriptionId: subscription.id,
    };
  } catch (error: any) {
    console.error("Error creating subscription:", error);
    throw new functions.https.HttpsError(
      "internal",
      `Failed to create subscription: ${error.message}`
    );
  }
});

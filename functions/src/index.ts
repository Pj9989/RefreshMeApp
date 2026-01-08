import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import { onRequest } from "firebase-functions/v2/https";
import { onCall } from "firebase-functions/v2/https";
import { defineString } from "firebase-functions/params";
import Stripe from "stripe";

admin.initializeApp();

const stripeSecretKey = defineString("STRIPE_SECRET_KEY");
const stripeWebhookSecret = defineString("STRIPE_WEBHOOK_SECRET");

export const stripeWebhook = onRequest(async (req, res) => {
  const stripe = new Stripe(stripeSecretKey.value(), {
    apiVersion: "2024-06-20",
  });

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

  // Handle the event
  switch (event.type) {
    case "customer.subscription.created":
    case "customer.subscription.updated":
      const subscription = event.data.object as Stripe.Subscription;
      const customerId = subscription.customer as string;

      // Get customer to find Firebase UID
      const customer = await stripe.customers.retrieve(customerId);
      const firebaseUID = (customer as Stripe.Customer).metadata?.firebaseUID;

      if (firebaseUID) {
        await admin
          .firestore()
          .collection("users")
          .doc(firebaseUID)
          .set(
            {
              stripeCustomerId: customerId,
              subscriptionId: subscription.id,
              subscriptionStatus: subscription.status,
              isSubscribed: subscription.status === "active",
              currentPeriodEnd: new Date(subscription.current_period_end * 1000),
            },
            { merge: true }
          );
      }
      break;

    case "customer.subscription.deleted":
      const deletedSubscription = event.data.object as Stripe.Subscription;
      const deletedCustomerId = deletedSubscription.customer as string;

      const deletedCustomer = await stripe.customers.retrieve(deletedCustomerId);
      const deletedFirebaseUID = (deletedCustomer as Stripe.Customer).metadata?.firebaseUID;

      if (deletedFirebaseUID) {
        await admin
          .firestore()
          .collection("users")
          .doc(deletedFirebaseUID)
          .set(
            {
              subscriptionStatus: "canceled",
              isSubscribed: false,
            },
            { merge: true }
          );
      }
      break;

    case "invoice.payment_succeeded":
      const invoice = event.data.object as Stripe.Invoice;
      const invoiceCustomerId = invoice.customer as string;
      const invoiceSubscriptionId = invoice.subscription as string;

      if (invoiceSubscriptionId) {
        const invoiceCustomer = await stripe.customers.retrieve(invoiceCustomerId);
        const invoiceFirebaseUID = (invoiceCustomer as Stripe.Customer).metadata?.firebaseUID;

        if (invoiceFirebaseUID) {
          await admin
            .firestore()
            .collection("users")
            .doc(invoiceFirebaseUID)
            .set(
              {
                subscriptionStatus: "active",
                isSubscribed: true,
              },
              { merge: true }
            );
        }
      }
      break;

    case "invoice.payment_failed":
      const failedInvoice = event.data.object as Stripe.Invoice;
      const failedCustomerId = failedInvoice.customer as string;

      const failedCustomer = await stripe.customers.retrieve(failedCustomerId);
      const failedFirebaseUID = (failedCustomer as Stripe.Customer).metadata?.firebaseUID;

      if (failedFirebaseUID) {
        await admin
          .firestore()
          .collection("users")
          .doc(failedFirebaseUID)
          .set(
            {
              subscriptionStatus: "past_due",
              isSubscribed: false,
            },
            { merge: true }
          );
      }
      break;
  }

  res.json({ received: true, type: event.type });
});

export const createSubscription = onCall(async (request) => {
  if (!request.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated"
    );
  }

  const stripe = new Stripe(stripeSecretKey.value(), {
    apiVersion: "2024-06-20",
  });

  const { userId, priceId } = request.data;

  if (!userId || !priceId) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Missing required parameters: userId and priceId"
    );
  }

  try {
    // Get user email from Firebase Auth
    const userEmail = request.auth.token.email;

    if (!userEmail) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "User email not found"
      );
    }

    // Create or retrieve Stripe customer
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
        metadata: {
          firebaseUID: userId,
        },
      });
    }

    // Create subscription
    const subscription = await stripe.subscriptions.create({
      customer: customer.id,
      items: [{ price: priceId }],
      payment_behavior: "default_incomplete",
      payment_settings: { save_default_payment_method: "on_subscription" },
      expand: ["latest_invoice.payment_intent"],
    });

    const invoice = subscription.latest_invoice as Stripe.Invoice;
    const paymentIntent = invoice.payment_intent as Stripe.PaymentIntent;

    // Update Firestore with subscription info
    await admin.firestore().collection("users").doc(userId).set(
      {
        stripeCustomerId: customer.id,
        subscriptionId: subscription.id,
        subscriptionStatus: subscription.status,
      },
      { merge: true }
    );

    return {
      clientSecret: paymentIntent.client_secret,
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
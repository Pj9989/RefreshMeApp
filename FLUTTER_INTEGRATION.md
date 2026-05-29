# RefreshMe — Flutter iOS Integration Guide

Last updated by Claude (Windows session) — sync this file to get full backend context on Mac.
Firebase Project: `refreshme-74f79`

---

## Architecture Overview

RefreshMe is a multi-platform booking app:
- **Android** — Kotlin + Jetpack Compose (this repo)
- **iOS** — Flutter + Xcode (your Mac project)
- **Backend** — Firebase (Firestore + Cloud Functions Gen 2) + Stripe

Both Android and iOS share the **same Firebase project** and the **same Cloud Functions**.

---

## Firebase Setup (Flutter)

### 1. Install FlutterFire
```bash
dart pub global activate flutterfire_cli
flutterfire configure --project=refreshme-74f79
```
This generates `lib/firebase_options.dart` for iOS/Android.

### 2. Required pubspec.yaml dependencies
```yaml
dependencies:
  firebase_core: ^3.x.x
  firebase_auth: ^5.x.x
  cloud_firestore: ^5.x.x
  cloud_functions: ^5.x.x
  firebase_messaging: ^15.x.x
  stripe_checkout: ^0.x.x    # OR flutter_stripe for full payment sheet
  flutter_stripe: ^10.x.x    # Recommended — full PaymentSheet support
```

---

## Stripe Configuration

| Key | Value |
|-----|-------|
| Publishable Key (live) | Store outside source control and inject at build/runtime. |
| Mode | **Live** (not test) |
| Platform fee | 10% taken automatically via Stripe Connect |

Initialize Stripe in `main.dart`:
```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  Stripe.publishableKey = stripePublishableKey;
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  runApp(const RefreshMeApp());
}
```

---

## Cloud Functions Reference

All functions are deployed at:
`https://us-central1-refreshme-74f79.cloudfunctions.net/`

Call them via `FirebaseFunctions.instance.httpsCallable('functionName')`.

### `createBookingPaymentIntent`
Creates a Stripe PaymentIntent for a booking deposit (20% of service price).
Returns a `clientSecret` to present with `PaymentSheet`.

**Request:**
```dart
final callable = FirebaseFunctions.instance.httpsCallable('createBookingPaymentIntent');
final result = await callable.call({
  'userId': FirebaseAuth.instance.currentUser!.uid,
  'customerId': FirebaseAuth.instance.currentUser!.uid,
  'customerName': user.displayName ?? 'Client',
  'customerPhotoUrl': user.photoURL ?? '',
  'stylistId': stylist.id,
  'stylistName': stylist.name,
  'stylistPhotoUrl': stylist.profileImageUrl ?? '',
  'stripeAccountId': stylist.stripeAccountId ?? '',  // REQUIRED for platform fee routing
  'serviceName': service.name,
  'servicePrice': service.price,           // Double, full price in dollars
  'bookingDate': bookingDate.millisecondsSinceEpoch,
  'date': bookingDate.millisecondsSinceEpoch,
  'currency': 'usd',
  'isMobile': false,
  'isEvent': false,
  'groupSize': 1,
  'eventType': '',
  'isSilentAppointment': false,
  'isSensoryFriendly': false,
  'status': 'REQUESTED',
});
```

**Response:**
```dart
final data = result.data as Map<String, dynamic>;
final clientSecret = data['clientSecret'] as String;
final bookingId = data['bookingId'] as String;
final depositAmount = (data['depositAmount'] as num).toDouble(); // in cents
```

**Present PaymentSheet (flutter_stripe):**
```dart
await Stripe.instance.initPaymentSheet(
  paymentSheetData: SetupPaymentSheetParameters(
    paymentIntentClientSecret: clientSecret,
    merchantDisplayName: 'RefreshMe',
  ),
);
await Stripe.instance.presentPaymentSheet();
```

---

### `createConnectAccount`
Creates or resumes a Stripe Express onboarding URL for a stylist.
Call this when a stylist taps "Set Up Payout Account".

**Request:**
```dart
final callable = FirebaseFunctions.instance.httpsCallable('createConnectAccount');
final result = await callable.call({
  'userId': FirebaseAuth.instance.currentUser!.uid,
});
final data = result.data as Map<String, dynamic>;
final onboardingUrl = data['url'] as String;
// Open in browser:
await launchUrl(Uri.parse(onboardingUrl));
```

---

### `getConnectAccountStatus`
Checks if a stylist's Stripe account is fully set up and can receive payouts.

**Request:**
```dart
final callable = FirebaseFunctions.instance.httpsCallable('getConnectAccountStatus');
final result = await callable.call({
  'userId': FirebaseAuth.instance.currentUser!.uid,
});
final data = result.data as Map<String, dynamic>;
final chargesEnabled = data['chargesEnabled'] as bool;
final payoutsEnabled = data['payoutsEnabled'] as bool;
```

---

### `stripeWebhook`
**Server-side only** — do not call from app. Stripe calls this automatically.
URL: `https://stripewebhook-vldsq5kzxa-uc.a.run.app`

---

## Firestore Data Models

### `/stylists/{stylistId}`
```dart
class Stylist {
  String id;
  String name;
  String? profileImageUrl;
  double rating;
  bool? isAvailable;
  GeoPoint? location;
  List<Service> services;
  String? bio;
  String? address;
  String? specialty;
  String? stripeAccountId;       // Stripe Connect account ID (set after onboarding)
  String? stripeAccountStatus;   // "active" | "pending" | null
  bool? stripeChargesEnabled;
  bool? stripePayoutsEnabled;
  bool? stripeOnboardingComplete;
  // ... other fields
}
```

### `/bookings/{bookingId}`
```dart
class Booking {
  String id;
  String userId;           // customer
  String stylistId;
  String serviceName;
  double servicePrice;
  double depositAmount;    // 20% paid upfront
  String status;           // REQUESTED | CONFIRMED | IN_PROGRESS | COMPLETED | CANCELLED
  String? stripePaymentIntentId;
  DateTime bookingDate;
  bool isMobile;
  // ... other fields
}
```

### `/users/{userId}`
```dart
class User {
  String uid;
  String name;
  String email;
  String? photoUrl;
  String? stripeCustomerId;    // Created automatically on first booking
  String role;                 // "customer" | "stylist"
}
```

---

## Payment Flow (End to End)

```
Customer selects stylist + service
    ↓
Call createBookingPaymentIntent (passes stripeAccountId of stylist)
    ↓
Backend verifies the stylist's Connect account can receive transfers/payouts
    ↓
Firebase Function creates PaymentIntent with:
  - amount = service price × 20% (deposit)
  - application_fee_amount = deposit × 10% (RefreshMe cut)
  - transfer_data.destination = stylist's stripeAccountId
    ↓
App presents Stripe PaymentSheet with clientSecret
    ↓
Customer pays → Stripe routes:
  - 90% → stylist's Stripe account (automatic payout)
  - 10% → RefreshMe's Stripe account (platform fee)
    ↓
stripeWebhook handles payment_intent.succeeded → updates booking status to DEPOSIT_PAID
```

---

## Stylist Payout Onboarding Flow

```
Stylist taps "Payout Account" in profile
    ↓
Call createConnectAccount
    ↓
Open returned URL in browser (Stripe Express onboarding — ~5 min)
    ↓
Stripe calls account.updated webhook → Firebase saves stripeAccountId + stripeAccountStatus
    ↓
Stylist can now receive payouts automatically
```

---

## iOS-Specific Notes

### Info.plist — add Stripe return URL scheme
```xml
<key>LSApplicationQueriesSchemes</key>
<array>
  <string>stripe</string>
</array>
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>refreshme</string>  <!-- your custom URL scheme for Stripe redirect -->
    </array>
  </dict>
</array>
```

### AppDelegate.swift — handle Stripe URL
```swift
import Stripe
override func application(_ app: UIApplication, open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
  let stripeHandled = StripeAPI.handleURLCallback(with: url)
  return stripeHandled
}
```

Or in Flutter (AppDelegate.swift or via flutter_stripe plugin — usually handled automatically).

---

## How to Bring Claude on Mac Up to Speed

1. `git pull` this repo on your Mac
2. Open a new Claude session on your Mac
3. Drag this file (`FLUTTER_INTEGRATION.md`) into the chat
4. Say: "This is the RefreshMe backend. I need help building the iOS Flutter UI for [feature]."

Claude will have full context about all the Cloud Functions, data models, and payment flow.

---

## Key Firebase Project Info

| Setting | Value |
|---------|-------|
| Project ID | `refreshme-74f79` |
| Region | `us-central1` |
| Auth domain | `refreshme-74f79.firebaseapp.com` |
| Storage bucket | `refreshme-74f79.appspot.com` |

---

## Firebase App Check (iOS)

App Check is required on all callable Cloud Functions. `enforceAppCheck` is currently **soft** (tokens validated when present, but missing tokens are not rejected). This will be switched back to **hard enforcement** once iOS is confirmed working end-to-end.

### iOS App Registration

| App | Bundle ID | Provider | Status |
|-----|-----------|----------|--------|
| refreshme (ios) | `com.refreshmeapp.refreshme` | App Attest | Registered |

### Debug Tokens (registered in Firebase Console)

| Name | UUID | Purpose |
|------|------|---------|
| PJ iPhone real | `901BCBA6-CF7A-4114-BEB0-4967956873F3` | PJ's physical iPhone (printed by SDK on first launch) |
| PJ iPhone debug | `11915533-315D-43CF-A6D6-51959728DAC8` | Pre-generated fallback |
| Phill iPhone 17 simulator | (registered) | iOS Simulator |

### Required `pubspec.yaml` dependency

```yaml
dependencies:
  firebase_app_check: ^0.4.4
```

Install with:
```bash
flutter pub add firebase_app_check
```

### `lib/main.dart` — App Check activation

Add this **after** `Firebase.initializeApp()` and **before** `runApp()`:

```dart
import 'package:flutter/foundation.dart';
import 'package:firebase_app_check/firebase_app_check.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );

  // App Check — debug provider in dev, App Attest in production
  await FirebaseAppCheck.instance.activate(
    appleProvider: kDebugMode
        ? AppleProvider.debug
        : AppleProvider.appAttest,
  );

  runApp(const RefreshMeApp());
}
```

> **Note:** `AppleProvider.appAttest` is the correct production provider (not `deviceCheck`). The iOS app is registered in Firebase Console with App Attest.

### Re-enabling hard enforcement

Once iOS App Check is confirmed working in production:
1. In `functions/src/index.ts`, change `enforceAppCheck: false` → `enforceAppCheck: true`
2. Run `firebase deploy --only functions`
3. Verify both Android and iOS can call Cloud Functions without errors

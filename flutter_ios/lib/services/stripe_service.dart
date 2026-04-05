/// RefreshMe — Stripe Service
/// Place this file at: lib/services/stripe_service.dart
///
/// Handles all Stripe operations:
///   - Creating booking payment intents (calls Firebase Cloud Function)
///   - Presenting the Stripe PaymentSheet
///   - Stylist Connect onboarding
///   - Checking stylist payout account status

import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:url_launcher/url_launcher.dart';

class StripeService {
  static final StripeService _instance = StripeService._internal();
  factory StripeService() => _instance;
  StripeService._internal();

  final FirebaseFunctions _functions = FirebaseFunctions.instance;

  // ─────────────────────────────────────────────────
  // BOOKING PAYMENT
  // ─────────────────────────────────────────────────

  /// Creates a payment intent for a booking deposit (20% of service price)
  /// and presents the Stripe PaymentSheet.
  ///
  /// Returns the bookingId on success, throws on failure or cancellation.
  Future<String> chargeBookingDeposit({
    required String stylistId,
    required String stylistName,
    required String stylistPhotoUrl,
    required String? stylistStripeAccountId,
    required String serviceName,
    required double servicePrice,
    required DateTime bookingDate,
    bool isMobile = false,
    bool isEvent = false,
    int groupSize = 1,
    String eventType = '',
    bool isSilentAppointment = false,
    bool isSensoryFriendly = false,
  }) async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) throw Exception('User not signed in');

    // 1. Call the Cloud Function to create the PaymentIntent
    final callable = _functions.httpsCallable('createBookingPaymentIntent');
    final result = await callable.call(<String, dynamic>{
      'userId': user.uid,
      'customerId': user.uid,
      'customerName': user.displayName ?? 'Client',
      'customerPhotoUrl': user.photoURL ?? '',
      'stylistId': stylistId,
      'stylistName': stylistName,
      'stylistPhotoUrl': stylistPhotoUrl,
      'stripeAccountId': stylistStripeAccountId ?? '',
      'serviceName': serviceName,
      'servicePrice': servicePrice,
      'bookingDate': bookingDate.millisecondsSinceEpoch,
      'date': bookingDate.millisecondsSinceEpoch,
      'currency': 'usd',
      'isMobile': isMobile,
      'isEvent': isEvent,
      'groupSize': groupSize,
      'eventType': eventType,
      'isSilentAppointment': isSilentAppointment,
      'isSensoryFriendly': isSensoryFriendly,
      'status': 'REQUESTED',
    });

    final data = result.data as Map<String, dynamic>;
    final clientSecret = data['clientSecret'] as String;
    final bookingId = data['bookingId'] as String;

    // 2. Init and present Stripe PaymentSheet
    await Stripe.instance.initPaymentSheet(
      paymentSheetData: SetupPaymentSheetParameters(
        paymentIntentClientSecret: clientSecret,
        merchantDisplayName: 'RefreshMe',
        style: ThemeMode.system,
      ),
    );

    await Stripe.instance.presentPaymentSheet();

    // If we get here without exception, payment succeeded
    return bookingId;
  }

  // ─────────────────────────────────────────────────
  // STYLIST CONNECT ONBOARDING
  // ─────────────────────────────────────────────────

  /// Starts Stripe Express onboarding for the currently signed-in stylist.
  /// Opens the Stripe onboarding URL in the device browser.
  Future<void> startPayoutOnboarding() async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) throw Exception('User not signed in');

    final callable = _functions.httpsCallable('createConnectAccount');
    final result = await callable.call(<String, dynamic>{
      'userId': user.uid,
    });

    final data = result.data as Map<String, dynamic>;
    final onboardingUrl = data['url'] as String;

    final uri = Uri.parse(onboardingUrl);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else {
      throw Exception('Could not open Stripe onboarding URL');
    }
  }

  /// Checks if the current stylist's Stripe account can receive payouts.
  Future<ConnectAccountStatus> getPayoutAccountStatus() async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) throw Exception('User not signed in');

    final callable = _functions.httpsCallable('getConnectAccountStatus');
    final result = await callable.call(<String, dynamic>{
      'userId': user.uid,
    });

    final data = result.data as Map<String, dynamic>;
    return ConnectAccountStatus(
      chargesEnabled: data['chargesEnabled'] as bool? ?? false,
      payoutsEnabled: data['payoutsEnabled'] as bool? ?? false,
      detailsSubmitted: data['detailsSubmitted'] as bool? ?? false,
    );
  }
}

class ConnectAccountStatus {
  final bool chargesEnabled;
  final bool payoutsEnabled;
  final bool detailsSubmitted;

  const ConnectAccountStatus({
    required this.chargesEnabled,
    required this.payoutsEnabled,
    required this.detailsSubmitted,
  });

  bool get isFullyActive => chargesEnabled && payoutsEnabled;
}

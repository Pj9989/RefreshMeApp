/// RefreshMe — Stripe Service
/// Place this file at: lib/services/stripe_service.dart
///
/// Handles all Stripe operations:
///   - Creating booking payment intents (calls Firebase Cloud Function)
///   - Presenting the Stripe PaymentSheet
///   - Stylist Connect onboarding
///   - Checking stylist payout account status
///   - Confirming booking completion for payout release

import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart' show ThemeMode;
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
  /// Returns the server-confirmed booking payment details on success,
  /// throws on failure or cancellation.
  Future<BookingPaymentResult> chargeBookingDeposit({
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
    if (stylistStripeAccountId == null ||
        stylistStripeAccountId.trim().isEmpty) {
      throw Exception('Stylist payout account is not ready');
    }

    // 1. Call the Cloud Function to create the PaymentIntent
    final callable = _functions.httpsCallable('createBookingPaymentIntent');
    final result = await callable.call(<String, dynamic>{
      'stylistId': stylistId,
      'serviceName': serviceName,
      'bookingDate': bookingDate.millisecondsSinceEpoch,
      'date': bookingDate.millisecondsSinceEpoch,
      'isMobile': isMobile,
      'isEvent': isEvent,
      'groupSize': groupSize,
      'eventType': eventType,
      'isSilentAppointment': isSilentAppointment,
      'isSensoryFriendly': isSensoryFriendly,
    });

    final data = result.data as Map<String, dynamic>;
    final clientSecret = data['clientSecret'] as String;
    final bookingId = data['bookingId'] as String;
    final depositAmount = _readMoney(data['depositAmount']);
    final confirmedServicePrice = _readMoney(data['servicePrice']);

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
    return BookingPaymentResult(
      bookingId: bookingId,
      depositAmount: depositAmount,
      servicePrice: confirmedServicePrice,
    );
  }

  // ─────────────────────────────────────────────────
  // BOOKING COMPLETION / PAYOUT RELEASE
  // ─────────────────────────────────────────────────

  /// Stylist marks the session finished.
  ///
  /// This does not immediately complete the booking. The backend moves it to
  /// AWAITING_CUSTOMER_CONFIRMATION and gives the client 24 hours to confirm
  /// or report an issue before automatic confirmation.
  Future<BookingCompletionResult> requestBookingCompletion({
    required String bookingId,
  }) async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) throw Exception('User not signed in');

    final callable = _functions.httpsCallable('requestBookingCompletion');
    final result = await callable.call(<String, dynamic>{
      'bookingId': bookingId,
    });

    return BookingCompletionResult.fromFunctionResult(result.data);
  }

  /// Customer confirms the session was completed successfully.
  ///
  /// The backend marks the booking COMPLETED and payoutStatus eligible.
  Future<BookingCompletionResult> confirmBookingCompletion({
    required String bookingId,
  }) async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) throw Exception('User not signed in');

    final callable = _functions.httpsCallable('confirmBookingCompletion');
    final result = await callable.call(<String, dynamic>{
      'bookingId': bookingId,
    });

    return BookingCompletionResult.fromFunctionResult(result.data);
  }

  /// Customer reports a problem instead of confirming completion.
  ///
  /// The backend marks the booking COMPLETION_DISPUTED and pauses payout.
  Future<BookingCompletionResult> disputeBookingCompletion({
    required String bookingId,
    String reason =
        'Customer reported an issue after stylist marked the session complete',
  }) async {
    final user = FirebaseAuth.instance.currentUser;
    if (user == null) throw Exception('User not signed in');

    final callable = _functions.httpsCallable('disputeBookingCompletion');
    final result = await callable.call(<String, dynamic>{
      'bookingId': bookingId,
      'reason': reason,
    });

    return BookingCompletionResult.fromFunctionResult(result.data);
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
    final result = await callable.call(<String, dynamic>{'userId': user.uid});

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
    final result = await callable.call(<String, dynamic>{'userId': user.uid});

    final data = result.data as Map<String, dynamic>;
    return ConnectAccountStatus(
      status: data['status'] as String? ?? 'not_connected',
      chargesEnabled: data['chargesEnabled'] as bool? ?? false,
      payoutsEnabled: data['payoutsEnabled'] as bool? ?? false,
      detailsSubmitted: data['detailsSubmitted'] as bool? ?? false,
    );
  }
}

class BookingPaymentResult {
  final String bookingId;
  final double depositAmount;
  final double servicePrice;

  const BookingPaymentResult({
    required this.bookingId,
    required this.depositAmount,
    required this.servicePrice,
  });
}

class BookingCompletionResult {
  final String status;
  final DateTime? autoConfirmAt;

  const BookingCompletionResult({required this.status, this.autoConfirmAt});

  factory BookingCompletionResult.fromFunctionResult(dynamic value) {
    final data = Map<String, dynamic>.from(value as Map);
    final autoConfirmMillis = data['autoConfirmAt'];
    return BookingCompletionResult(
      status: data['status'] as String? ?? '',
      autoConfirmAt: autoConfirmMillis is num
          ? DateTime.fromMillisecondsSinceEpoch(autoConfirmMillis.toInt())
          : null,
    );
  }

  bool get isAwaitingCustomerConfirmation =>
      status == RefreshMeBookingStatus.awaitingCustomerConfirmation;
  bool get isCompleted => status == RefreshMeBookingStatus.completed;
  bool get isDisputed => status == RefreshMeBookingStatus.completionDisputed;
}

class RefreshMeBookingStatus {
  static const requested = 'REQUESTED';
  static const pending = 'PENDING';
  static const accepted = 'ACCEPTED';
  static const depositPaid = 'DEPOSIT_PAID';
  static const onTheWay = 'ON_THE_WAY';
  static const inProgress = 'IN_PROGRESS';
  static const awaitingCustomerConfirmation = 'AWAITING_CUSTOMER_CONFIRMATION';
  static const completionDisputed = 'COMPLETION_DISPUTED';
  static const completed = 'COMPLETED';
  static const cancelled = 'CANCELLED';
  static const declined = 'DECLINED';
}

class ConnectAccountStatus {
  final String status;
  final bool chargesEnabled;
  final bool payoutsEnabled;
  final bool detailsSubmitted;

  const ConnectAccountStatus({
    required this.status,
    required this.chargesEnabled,
    required this.payoutsEnabled,
    required this.detailsSubmitted,
  });

  bool get isFullyActive => chargesEnabled && payoutsEnabled;
  bool get isPending => status == 'pending' || detailsSubmitted;
}

double _readMoney(dynamic value) {
  if (value is num) return value.toDouble();
  return double.tryParse(value?.toString() ?? '') ?? 0;
}

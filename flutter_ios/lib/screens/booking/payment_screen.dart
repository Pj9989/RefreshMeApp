/// RefreshMe — Payment / Booking Confirmation Screen
/// Place this file at: lib/screens/booking/payment_screen.dart
///
/// Shows booking summary + deposit amount, then triggers Stripe PaymentSheet.
/// Navigate to this screen after the user has selected a stylist, service, and date.

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../services/stripe_service.dart';

class PaymentScreen extends StatefulWidget {
  final String stylistId;
  final String stylistName;
  final String stylistPhotoUrl;
  final String? stylistStripeAccountId;
  final String serviceName;
  final double servicePrice;
  final DateTime bookingDate;
  final bool isMobile;

  const PaymentScreen({
    super.key,
    required this.stylistId,
    required this.stylistName,
    required this.stylistPhotoUrl,
    this.stylistStripeAccountId,
    required this.serviceName,
    required this.servicePrice,
    required this.bookingDate,
    this.isMobile = false,
  });

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  bool _isLoading = false;
  String? _errorMessage;

  double get _depositAmount => widget.servicePrice * 0.20;

  Future<void> _handlePayment() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final bookingId = await StripeService().chargeBookingDeposit(
        stylistId: widget.stylistId,
        stylistName: widget.stylistName,
        stylistPhotoUrl: widget.stylistPhotoUrl,
        stylistStripeAccountId: widget.stylistStripeAccountId,
        serviceName: widget.serviceName,
        servicePrice: widget.servicePrice,
        bookingDate: widget.bookingDate,
        isMobile: widget.isMobile,
      );

      if (!mounted) return;

      // Navigate to success screen
      Navigator.of(context).pushReplacementNamed(
        '/booking-success',
        arguments: {'bookingId': bookingId},
      );
    } on Exception catch (e) {
      final msg = e.toString();
      // User cancelled — don't show error
      if (msg.contains('cancel') || msg.contains('Cancel')) {
        setState(() => _isLoading = false);
        return;
      }
      setState(() {
        _errorMessage = 'Payment failed. Please try again.';
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final dateFormatted =
        DateFormat('EEEE, MMMM d • h:mm a').format(widget.bookingDate);
    final currencyFormat =
        NumberFormat.currency(locale: 'en_US', symbol: '\$');

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: const BackButton(color: Colors.black),
        title: const Text(
          'Confirm Booking',
          style: TextStyle(color: Colors.black, fontWeight: FontWeight.w600),
        ),
        centerTitle: true,
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Stylist info card
              _StylistCard(
                name: widget.stylistName,
                photoUrl: widget.stylistPhotoUrl,
                serviceName: widget.serviceName,
                date: dateFormatted,
                isMobile: widget.isMobile,
              ),
              const SizedBox(height: 24),

              // Price breakdown
              _PriceBreakdown(
                servicePrice: widget.servicePrice,
                depositAmount: _depositAmount,
                currencyFormat: currencyFormat,
              ),
              const SizedBox(height: 24),

              // Info note
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFFF5F0FF),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.info_outline, color: Color(0xFF6B3FA0), size: 20),
                    SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'A 20% deposit is charged now to secure your appointment. '
                        'The remaining balance is paid directly to your stylist.',
                        style: TextStyle(fontSize: 13, color: Color(0xFF444444)),
                      ),
                    ),
                  ],
                ),
              ),

              if (_errorMessage != null) ...[
                const SizedBox(height: 16),
                Text(
                  _errorMessage!,
                  style: const TextStyle(color: Colors.red, fontSize: 14),
                  textAlign: TextAlign.center,
                ),
              ],

              const Spacer(),

              // Pay button
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _handlePayment,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF6B3FA0),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  child: _isLoading
                      ? const CircularProgressIndicator(color: Colors.white)
                      : Text(
                          'Pay Deposit  ${currencyFormat.format(_depositAmount)}',
                          style: const TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                          ),
                        ),
                ),
              ),
              const SizedBox(height: 12),
              const Center(
                child: Text(
                  'Secured by Stripe',
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────
// Sub-widgets
// ─────────────────────────────────────────────────

class _StylistCard extends StatelessWidget {
  final String name;
  final String photoUrl;
  final String serviceName;
  final String date;
  final bool isMobile;

  const _StylistCard({
    required this.name,
    required this.photoUrl,
    required this.serviceName,
    required this.date,
    required this.isMobile,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey[50],
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey[200]!),
      ),
      child: Row(
        children: [
          CircleAvatar(
            radius: 32,
            backgroundImage:
                photoUrl.isNotEmpty ? NetworkImage(photoUrl) : null,
            backgroundColor: const Color(0xFF6B3FA0),
            child: photoUrl.isEmpty
                ? Text(
                    name.isNotEmpty ? name[0].toUpperCase() : '?',
                    style: const TextStyle(color: Colors.white, fontSize: 20),
                  )
                : null,
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  style: const TextStyle(
                      fontWeight: FontWeight.w700, fontSize: 16),
                ),
                const SizedBox(height: 4),
                Text(serviceName,
                    style: const TextStyle(fontSize: 14, color: Colors.grey)),
                const SizedBox(height: 4),
                Text(date,
                    style: const TextStyle(fontSize: 13, color: Colors.grey)),
                if (isMobile) ...[
                  const SizedBox(height: 4),
                  const Row(
                    children: [
                      Icon(Icons.directions_car,
                          size: 14, color: Color(0xFF6B3FA0)),
                      SizedBox(width: 4),
                      Text('Mobile appointment',
                          style: TextStyle(
                              fontSize: 12, color: Color(0xFF6B3FA0))),
                    ],
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PriceBreakdown extends StatelessWidget {
  final double servicePrice;
  final double depositAmount;
  final NumberFormat currencyFormat;

  const _PriceBreakdown({
    required this.servicePrice,
    required this.depositAmount,
    required this.currencyFormat,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Price Breakdown',
          style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
        ),
        const SizedBox(height: 12),
        _Row(label: 'Service price', value: currencyFormat.format(servicePrice)),
        const SizedBox(height: 8),
        _Row(
          label: 'Deposit due now (20%)',
          value: currencyFormat.format(depositAmount),
          highlight: true,
        ),
        const SizedBox(height: 8),
        _Row(
          label: 'Remaining (pay at appointment)',
          value: currencyFormat.format(servicePrice - depositAmount),
          muted: true,
        ),
      ],
    );
  }
}

class _Row extends StatelessWidget {
  final String label;
  final String value;
  final bool highlight;
  final bool muted;

  const _Row({
    required this.label,
    required this.value,
    this.highlight = false,
    this.muted = false,
  });

  @override
  Widget build(BuildContext context) {
    final color = muted
        ? Colors.grey
        : highlight
            ? const Color(0xFF6B3FA0)
            : Colors.black;
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: TextStyle(fontSize: 14, color: color)),
        Text(value,
            style: TextStyle(
                fontSize: 14,
                fontWeight:
                    highlight ? FontWeight.w700 : FontWeight.normal,
                color: color)),
      ],
    );
  }
}

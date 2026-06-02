/// RefreshMe — Booking Completion Controls
/// Place this file at: lib/screens/booking/booking_completion_controls.dart
///
/// Drop this widget into the iOS/Flutter booking card or active session screen
/// where the app currently shows session actions.

import 'package:flutter/material.dart';

import '../../services/stripe_service.dart';

class BookingCompletionControls extends StatefulWidget {
  final String bookingId;
  final String status;
  final bool isStylist;
  final VoidCallback? onStatusChanged;

  const BookingCompletionControls({
    super.key,
    required this.bookingId,
    required this.status,
    required this.isStylist,
    this.onStatusChanged,
  });

  @override
  State<BookingCompletionControls> createState() =>
      _BookingCompletionControlsState();
}

class _BookingCompletionControlsState extends State<BookingCompletionControls> {
  bool _isLoading = false;
  String? _errorMessage;

  Future<void> _runAction(Future<void> Function() action) async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await action();
      if (!mounted) return;
      widget.onStatusChanged?.call();
    } on Exception catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = e.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _requestCompletion() async {
    await _runAction(() async {
      await StripeService().requestBookingCompletion(
        bookingId: widget.bookingId,
      );
    });
  }

  Future<void> _confirmCompletion() async {
    await _runAction(() async {
      await StripeService().confirmBookingCompletion(
        bookingId: widget.bookingId,
      );
    });
  }

  Future<void> _disputeCompletion() async {
    await _runAction(() async {
      await StripeService().disputeBookingCompletion(
        bookingId: widget.bookingId,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final content = _buildContent(context);
    if (content == null) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        content,
        if (_errorMessage != null) ...[
          const SizedBox(height: 8),
          Text(
            _errorMessage!,
            style: const TextStyle(color: Colors.red, fontSize: 13),
            textAlign: TextAlign.center,
          ),
        ],
      ],
    );
  }

  Widget? _buildContent(BuildContext context) {
    switch (widget.status) {
      case RefreshMeBookingStatus.inProgress:
        if (!widget.isStylist) return null;
        return _PrimaryActionButton(
          label: 'Complete Session',
          icon: Icons.check_circle_outline,
          isLoading: _isLoading,
          onPressed: _requestCompletion,
        );
      case RefreshMeBookingStatus.awaitingCustomerConfirmation:
        return widget.isStylist
            ? const _StatusNotice(
                icon: Icons.hourglass_top,
                title: 'Awaiting Client Confirmation',
                message:
                    'Payout will move forward after the client confirms or after 24 hours.',
              )
            : _CustomerConfirmationActions(
                isLoading: _isLoading,
                onConfirm: _confirmCompletion,
                onReportIssue: _disputeCompletion,
              );
      case RefreshMeBookingStatus.completionDisputed:
        return const _StatusNotice(
          icon: Icons.report_problem_outlined,
          title: 'In Review',
          message:
              'This session was reported for review. Payout is paused while the issue is handled.',
        );
      default:
        return null;
    }
  }
}

class _CustomerConfirmationActions extends StatelessWidget {
  final bool isLoading;
  final VoidCallback onConfirm;
  final VoidCallback onReportIssue;

  const _CustomerConfirmationActions({
    required this.isLoading,
    required this.onConfirm,
    required this.onReportIssue,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const _StatusNotice(
          icon: Icons.verified_outlined,
          title: 'Confirm Your Session',
          message:
              'Your stylist marked this session complete. Confirm it or report an issue before it auto-confirms in 24 hours.',
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: isLoading ? null : onReportIssue,
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.red.shade700,
                  side: BorderSide(color: Colors.red.shade200),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: const Text('Report Issue'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _PrimaryActionButton(
                label: 'Confirm',
                icon: Icons.check,
                isLoading: isLoading,
                onPressed: onConfirm,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _PrimaryActionButton extends StatelessWidget {
  final String label;
  final IconData icon;
  final bool isLoading;
  final VoidCallback onPressed;

  const _PrimaryActionButton({
    required this.label,
    required this.icon,
    required this.isLoading,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 52,
      child: ElevatedButton.icon(
        onPressed: isLoading ? null : onPressed,
        icon: isLoading
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              )
            : Icon(icon),
        label: Text(label),
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color(0xFF6B3FA0),
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
      ),
    );
  }
}

class _StatusNotice extends StatelessWidget {
  final IconData icon;
  final String title;
  final String message;

  const _StatusNotice({
    required this.icon,
    required this.title,
    required this.message,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF5F0FF),
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: const Color(0xFF6B3FA0), size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: Color(0xFF222222),
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  message,
                  style: const TextStyle(
                    fontSize: 13,
                    height: 1.35,
                    color: Color(0xFF555555),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

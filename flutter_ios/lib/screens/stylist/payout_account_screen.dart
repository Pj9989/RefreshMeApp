/// RefreshMe — Payout Account Screen (Stylist side)
/// Place this file at: lib/screens/stylist/payout_account_screen.dart
///
/// Shows the stylist their Stripe Connect status and lets them:
///   - Start onboarding if not connected
///   - See "Active" badge if fully set up
///   - Re-open onboarding to fix issues
///
/// Navigate here from the stylist profile menu (replaces Subscription screen).

import 'package:flutter/material.dart';
import '../../services/stripe_service.dart';

class PayoutAccountScreen extends StatefulWidget {
  const PayoutAccountScreen({super.key});

  @override
  State<PayoutAccountScreen> createState() => _PayoutAccountScreenState();
}

class _PayoutAccountScreenState extends State<PayoutAccountScreen>
    with WidgetsBindingObserver {
  ConnectAccountStatus? _status;
  bool _isLoading = true;
  bool _isActioning = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadStatus();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Refresh status when returning from the Stripe browser tab
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _loadStatus();
    }
  }

  Future<void> _loadStatus() async {
    setState(() => _isLoading = true);
    try {
      final status = await StripeService().getPayoutAccountStatus();
      setState(() {
        _status = status;
        _isLoading = false;
      });
    } catch (_) {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _handleOnboarding() async {
    setState(() {
      _isActioning = true;
      _errorMessage = null;
    });
    try {
      await StripeService().startPayoutOnboarding();
    } catch (e) {
      setState(() => _errorMessage = 'Could not open Stripe. Please try again.');
    } finally {
      setState(() => _isActioning = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        leading: const BackButton(color: Colors.black),
        title: const Text(
          'Payout Account',
          style: TextStyle(color: Colors.black, fontWeight: FontWeight.w600),
        ),
        centerTitle: true,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : SafeArea(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _StatusCard(status: _status),
                    const SizedBox(height: 32),
                    _HowItWorks(),
                    const SizedBox(height: 32),
                    if (_errorMessage != null) ...[
                      Text(
                        _errorMessage!,
                        style:
                            const TextStyle(color: Colors.red, fontSize: 14),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 16),
                    ],
                    _ActionButton(
                      status: _status,
                      isLoading: _isActioning,
                      onTap: _handleOnboarding,
                    ),
                    const SizedBox(height: 12),
                    const Center(
                      child: Text(
                        'Powered by Stripe',
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

class _StatusCard extends StatelessWidget {
  final ConnectAccountStatus? status;

  const _StatusCard({this.status});

  @override
  Widget build(BuildContext context) {
    final isActive = status?.isFullyActive ?? false;
    final isPending = status != null && status!.detailsSubmitted && !isActive;

    Color bgColor;
    Color iconColor;
    IconData icon;
    String title;
    String subtitle;

    if (isActive) {
      bgColor = const Color(0xFFEBF7F0);
      iconColor = const Color(0xFF2E7D52);
      icon = Icons.check_circle_rounded;
      title = 'Payout Account Active';
      subtitle =
          'You\'re all set! When clients book you, your earnings go straight to your bank account. RefreshMe keeps 10%.';
    } else if (isPending) {
      bgColor = const Color(0xFFFFF8E1);
      iconColor = const Color(0xFFF59E0B);
      icon = Icons.pending_rounded;
      title = 'Verification Pending';
      subtitle =
          'Stripe is reviewing your information. This usually takes 1–2 business days. You may need to complete additional steps.';
    } else {
      bgColor = const Color(0xFFF5F0FF);
      iconColor = const Color(0xFF6B3FA0);
      icon = Icons.account_balance_wallet_rounded;
      title = 'No Payout Account Yet';
      subtitle =
          'Connect a Stripe account to receive automatic payouts when clients book you. Takes about 5 minutes.';
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: iconColor, size: 36),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: iconColor,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  subtitle,
                  style:
                      const TextStyle(fontSize: 14, color: Color(0xFF444444)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _HowItWorks extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    const steps = [
      (
        icon: Icons.person_outline,
        title: 'Client books you',
        desc: 'They pay a 20% deposit to confirm the appointment.'
      ),
      (
        icon: Icons.payment,
        title: 'Stripe splits the payment',
        desc:
            '90% goes to your bank. 10% is RefreshMe\'s platform fee — automatically.'
      ),
      (
        icon: Icons.schedule,
        title: 'Payouts hit your bank',
        desc: 'Stripe pays out on your schedule — daily, weekly, or monthly.'
      ),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'How payouts work',
          style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16),
        ),
        const SizedBox(height: 16),
        ...steps.map((step) => Padding(
              padding: const EdgeInsets.only(bottom: 16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: const BoxDecoration(
                      color: Color(0xFFF5F0FF),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(step.icon,
                        color: const Color(0xFF6B3FA0), size: 20),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(step.title,
                            style: const TextStyle(
                                fontWeight: FontWeight.w600, fontSize: 14)),
                        const SizedBox(height: 2),
                        Text(step.desc,
                            style: const TextStyle(
                                fontSize: 13, color: Colors.grey)),
                      ],
                    ),
                  ),
                ],
              ),
            )),
      ],
    );
  }
}

class _ActionButton extends StatelessWidget {
  final ConnectAccountStatus? status;
  final bool isLoading;
  final VoidCallback onTap;

  const _ActionButton({
    required this.status,
    required this.isLoading,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isActive = status?.isFullyActive ?? false;
    final label = isActive
        ? 'Manage Payout Account'
        : status?.detailsSubmitted == true
            ? 'Complete Verification'
            : 'Set Up Payout Account';

    return SizedBox(
      width: double.infinity,
      height: 56,
      child: ElevatedButton(
        onPressed: isLoading ? null : onTap,
        style: ElevatedButton.styleFrom(
          backgroundColor: const Color(0xFF6B3FA0),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        ),
        child: isLoading
            ? const CircularProgressIndicator(color: Colors.white)
            : Text(
                label,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
      ),
    );
  }
}

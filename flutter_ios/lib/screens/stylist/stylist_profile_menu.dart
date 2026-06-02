/// RefreshMe — Stylist Profile Menu Items (No Subscription)
/// Place this file at: lib/screens/stylist/stylist_profile_menu.dart
///
/// Drop-in replacement for the subscription menu item in your stylist profile.
/// The "Subscription" row is replaced by "Payout Account".
///
/// Usage — in your existing StylistProfileScreen, replace the subscription
/// ListTile with the PayoutAccountTile widget below.

import 'package:flutter/material.dart';
import 'manage_portfolio_screen.dart';
import 'payout_account_screen.dart';

/// Replace your existing subscription tile with this widget.
/// It shows the current payout status badge and navigates to PayoutAccountScreen.
class PayoutAccountTile extends StatelessWidget {
  /// Pass the stylist's stripeAccountStatus from Firestore ("active", "pending", or null)
  final String? stripeAccountStatus;
  final bool? stripeChargesEnabled;
  final bool? stripePayoutsEnabled;
  final bool? stripeOnboardingComplete;

  const PayoutAccountTile({
    super.key,
    this.stripeAccountStatus,
    this.stripeChargesEnabled,
    this.stripePayoutsEnabled,
    this.stripeOnboardingComplete,
  });

  @override
  Widget build(BuildContext context) {
    final isActive =
        stripeAccountStatus == 'active' ||
        stripeOnboardingComplete == true ||
        (stripeChargesEnabled == true && stripePayoutsEnabled == true);
    final isPending =
        !isActive &&
        (stripeAccountStatus == 'pending' ||
            stripeChargesEnabled == true ||
            stripePayoutsEnabled == true);

    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: Container(
        width: 44,
        height: 44,
        decoration: const BoxDecoration(
          color: Color(0xFFF5F0FF),
          shape: BoxShape.circle,
        ),
        child: const Icon(
          Icons.account_balance_wallet_rounded,
          color: Color(0xFF6B3FA0),
          size: 22,
        ),
      ),
      title: const Text(
        'Payout Account',
        style: TextStyle(fontWeight: FontWeight.w500, fontSize: 15),
      ),
      subtitle: Text(
        isActive
            ? 'Active — earnings go to your bank'
            : isPending
            ? 'Pending — finish setup'
            : 'Not set up — tap to connect',
        style: TextStyle(
          fontSize: 12,
          color: isActive
              ? const Color(0xFF2E7D52)
              : isPending
              ? const Color(0xFFF59E0B)
              : Colors.grey,
        ),
      ),
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (isActive)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: const Color(0xFFEBF7F0),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Text(
                'Active',
                style: TextStyle(
                  fontSize: 11,
                  color: Color(0xFF2E7D52),
                  fontWeight: FontWeight.w600,
                ),
              ),
            )
          else if (isPending)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF8E1),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Text(
                'Pending',
                style: TextStyle(
                  fontSize: 11,
                  color: Color(0xFFF59E0B),
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          const SizedBox(width: 4),
          const Icon(Icons.chevron_right, color: Colors.grey),
        ],
      ),
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const PayoutAccountScreen()),
        );
      },
    );
  }
}

/// Opens the stylist portfolio manager. The manager includes photo/video upload
/// plus a visible Before & After entry point.
class PortfolioManagementTile extends StatelessWidget {
  const PortfolioManagementTile({super.key});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      leading: Container(
        width: 44,
        height: 44,
        decoration: const BoxDecoration(
          color: Color(0xFFF5F0FF),
          shape: BoxShape.circle,
        ),
        child: const Icon(
          Icons.photo_library_rounded,
          color: Color(0xFF6B3FA0),
          size: 22,
        ),
      ),
      title: const Text(
        'My Portfolio',
        style: TextStyle(fontWeight: FontWeight.w500, fontSize: 15),
      ),
      subtitle: const Text(
        'Upload photos, videos, and before & after work',
        style: TextStyle(fontSize: 12, color: Colors.grey),
      ),
      trailing: const Icon(Icons.chevron_right, color: Colors.grey),
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const ManagePortfolioScreen()),
        );
      },
    );
  }
}

// ─────────────────────────────────────────────────
// EXAMPLE: how to use in your existing profile screen
// ─────────────────────────────────────────────────
//
// In your StylistProfileScreen widget, find the subscription-related widget
// and replace it like this:
//
//   // REMOVE THIS:
//   ListTile(
//     title: Text('Subscription'),
//     onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => SubscriptionScreen())),
//   ),
//
//   // ADD THIS:
//   PayoutAccountTile(stripeAccountStatus: stylist.stripeAccountStatus),
//
// ─────────────────────────────────────────────────

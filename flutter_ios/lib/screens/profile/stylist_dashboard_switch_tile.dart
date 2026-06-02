/// RefreshMe — Stylist Dashboard Switch Tile
/// Place this file at: lib/screens/profile/stylist_dashboard_switch_tile.dart
///
/// Add this to the customer/profile menu so stylist accounts that are browsing
/// as customers can return to the stylist dashboard on iOS.

import 'package:flutter/material.dart';

import '../../services/role_navigation_service.dart';

class StylistDashboardSwitchTile extends StatelessWidget {
  final bool isStylistBrowseMode;
  final String stylistDashboardRouteName;
  final VoidCallback? onSwitchToStylistDashboard;
  final RoleNavigationService? roleNavigationService;

  const StylistDashboardSwitchTile({
    super.key,
    this.isStylistBrowseMode = false,
    this.stylistDashboardRouteName = '/stylist-dashboard',
    this.onSwitchToStylistDashboard,
    this.roleNavigationService,
  });

  @override
  Widget build(BuildContext context) {
    final roleService = roleNavigationService ?? RoleNavigationService();

    return StreamBuilder<bool>(
      stream: roleService.watchIsStylist(),
      initialData: isStylistBrowseMode,
      builder: (context, snapshot) {
        final canSwitch = isStylistBrowseMode || snapshot.data == true;
        if (!canSwitch) return const SizedBox.shrink();

        return ListTile(
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 16,
            vertical: 4,
          ),
          leading: Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(
              color: Color(0xFFF5F0FF),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.dashboard_rounded,
              color: Color(0xFF6B3FA0),
              size: 22,
            ),
          ),
          title: Text(
            isStylistBrowseMode
                ? 'Return to Stylist Dashboard'
                : 'Switch to Stylist Dashboard',
            style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 15),
          ),
          subtitle: const Text(
            'Manage bookings, payouts, services, and availability',
            style: TextStyle(fontSize: 12, color: Colors.grey),
          ),
          trailing: const Icon(Icons.chevron_right, color: Colors.grey),
          onTap: () {
            final onSwitch = onSwitchToStylistDashboard;
            if (onSwitch != null) {
              onSwitch();
              return;
            }

            Navigator.of(context).pushNamedAndRemoveUntil(
              stylistDashboardRouteName,
              (route) => false,
            );
          },
        );
      },
    );
  }
}

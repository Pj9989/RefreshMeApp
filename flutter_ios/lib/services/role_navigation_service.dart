/// RefreshMe — Role Navigation Service
/// Place this file at: lib/services/role_navigation_service.dart
///
/// Mirrors Android's role resolution:
/// 1. Prefer users/{uid}.role.
/// 2. If the user document is missing/ambiguous/customer, fall back to
///    stylists/{uid}.exists so a stylist can still return to their dashboard.

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

enum RefreshMeUserRole { customer, stylist, salonOwner, unknown }

class RoleNavigationService {
  RoleNavigationService({FirebaseAuth? auth, FirebaseFirestore? firestore})
    : _auth = auth ?? FirebaseAuth.instance,
      _firestore = firestore ?? FirebaseFirestore.instance;

  final FirebaseAuth _auth;
  final FirebaseFirestore _firestore;

  Future<RefreshMeUserRole> getCurrentUserRole() async {
    final uid = _auth.currentUser?.uid;
    if (uid == null || uid.trim().isEmpty) {
      return RefreshMeUserRole.unknown;
    }

    try {
      final userDoc = await _firestore.collection('users').doc(uid).get();
      final role = _parseRole(userDoc.data()?['role']);

      if (role == RefreshMeUserRole.stylist ||
          role == RefreshMeUserRole.salonOwner) {
        return role;
      }

      return _getStylistFallbackRole(uid, role);
    } catch (_) {
      return _getStylistFallbackRole(uid, RefreshMeUserRole.unknown);
    }
  }

  Future<bool> isStylist() async {
    final role = await getCurrentUserRole();
    return role == RefreshMeUserRole.stylist;
  }

  Stream<bool> watchIsStylist() async* {
    final uid = _auth.currentUser?.uid;
    if (uid == null || uid.trim().isEmpty) {
      yield false;
      return;
    }

    await for (final userDoc
        in _firestore.collection('users').doc(uid).snapshots()) {
      final role = _parseRole(userDoc.data()?['role']);
      if (role == RefreshMeUserRole.stylist) {
        yield true;
        continue;
      }

      final stylistDoc = await _firestore.collection('stylists').doc(uid).get();
      yield stylistDoc.exists;
    }
  }

  Future<RefreshMeUserRole> _getStylistFallbackRole(
    String uid,
    RefreshMeUserRole fallbackRole,
  ) async {
    try {
      final stylistDoc = await _firestore.collection('stylists').doc(uid).get();
      return stylistDoc.exists ? RefreshMeUserRole.stylist : fallbackRole;
    } catch (_) {
      return fallbackRole;
    }
  }

  RefreshMeUserRole _parseRole(dynamic rawRole) {
    final normalizedRole = rawRole?.toString().trim().toUpperCase().replaceAll(
      RegExp(r'[^A-Z0-9]'),
      '',
    );

    switch (normalizedRole) {
      case 'STYLIST':
      case 'STYLISTBARBER':
      case 'BARBER':
      case 'PROFESSIONAL':
        return RefreshMeUserRole.stylist;
      case 'SALONOWNER':
      case 'OWNER':
        return RefreshMeUserRole.salonOwner;
      case 'CUSTOMER':
      case 'CLIENT':
      case 'USER':
        return RefreshMeUserRole.customer;
      default:
        return RefreshMeUserRole.unknown;
    }
  }
}

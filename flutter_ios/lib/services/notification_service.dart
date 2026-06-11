import 'dart:io';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:cloud_firestore/cloud_firestore.dart';

/// Handles FCM token registration and saving to Firestore.
///
/// Call [NotificationService.init] once after the user signs in.
/// The token is saved to both users/{uid} and stylists/{uid} (if applicable)
/// so the backend sendPushNotification function can find it regardless of role.
class NotificationService {
  static final _messaging = FirebaseMessaging.instance;
  static final _firestore = FirebaseFirestore.instance;
  static final _auth = FirebaseAuth.instance;

  /// Request permission and save the FCM token to Firestore.
  /// Safe to call multiple times — it's idempotent.
  static Future<void> init({required String role}) async {
    // 1. Request permission (iOS shows the system dialog)
    final settings = await _messaging.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      provisional: false,
    );

    if (settings.authorizationStatus == AuthorizationStatus.denied) {
      print('[FCM] Permission denied — notifications will not work.');
      return;
    }

    // 2. On iOS, ensure APNs token is registered before getting FCM token
    if (Platform.isIOS) {
      await _messaging.getAPNSToken().timeout(
        const Duration(seconds: 5),
        onTimeout: () {
          print('[FCM] APNs token timed out — may not be registered yet.');
          return null;
        },
      );
    }

    // 3. Get FCM token
    final token = await _messaging.getToken();
    if (token == null) {
      print('[FCM] No FCM token returned — check APNs key in Firebase Console.');
      return;
    }

    print('[FCM] Token obtained: ${token.substring(0, 20)}...');
    await _saveToken(token: token, role: role);

    // 4. Listen for token refreshes (e.g. after reinstall)
    _messaging.onTokenRefresh.listen((newToken) async {
      print('[FCM] Token refreshed');
      await _saveToken(token: newToken, role: role);
    });
  }

  static Future<void> _saveToken({
    required String token,
    required String role,
  }) async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) {
      print('[FCM] No signed-in user — cannot save token.');
      return;
    }

    final platform = Platform.isIOS ? 'ios' : 'android';
    final update = {
      // Single-token field (read by backend sendPushNotification)
      'fcmToken': token,
      // Map field (also read by backend — supports multiple devices)
      'fcmTokens': {token: true},
      'fcmPlatform': platform,
      'fcmTokenUpdatedAt': FieldValue.serverTimestamp(),
    };

    // Always write to users doc
    await _firestore
        .collection('users')
        .doc(uid)
        .set(update, SetOptions(merge: true));

    // Also write to stylists doc if the user is a stylist
    if (role == 'STYLIST') {
      await _firestore
          .collection('stylists')
          .doc(uid)
          .set(update, SetOptions(merge: true));
    }

    print('[FCM] Token saved to Firestore for $uid ($platform)');
  }
}

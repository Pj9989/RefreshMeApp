// ─────────────────────────────────────────────────────────────────────────────
// HOW TO WIRE UP NotificationService IN YOUR FLUTTER APP
// ─────────────────────────────────────────────────────────────────────────────
//
// 1. Add to pubspec.yaml (if not already present):
//
//    firebase_messaging: ^15.0.0
//
// 2. In your main.dart, after Firebase.initializeApp() and AppCheck.activate():
//
//    void main() async {
//      WidgetsFlutterBinding.ensureInitialized();
//      await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
//      await FirebaseAppCheck.instance.activate(...);
//
//      // Handle background messages (required by firebase_messaging)
//      FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
//
//      runApp(const RefreshMeApp());
//    }
//
//    @pragma('vm:entry-point')
//    Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
//      // No-op is fine — iOS handles display automatically
//    }
//
// 3. After the user signs in and you know their role, call:
//
//    await NotificationService.init(role: userRole); // 'STYLIST' or 'CUSTOMER'
//
//    The best place is right after FirebaseAuth.instance.signInWith...() resolves,
//    OR in your AuthStateChanges listener when user != null:
//
//    FirebaseAuth.instance.authStateChanges().listen((user) async {
//      if (user != null) {
//        final role = await getUserRole(user.uid); // fetch from Firestore
//        await NotificationService.init(role: role);
//      }
//    });
//
// 4. iOS Info.plist — add these keys if not present:
//
//    <key>UIBackgroundModes</key>
//    <array>
//      <string>fetch</string>
//      <string>remote-notification</string>
//    </array>
//
// 5. ⚠️  CRITICAL: Upload APNs Auth Key to Firebase Console
//    Firebase Console → Project Settings → Cloud Messaging → Apple app configuration
//    → APNs Authentication Key → Upload
//    You need: .p8 key file, Key ID, Team ID (from developer.apple.com)
//    Without this, NO iOS notification will ever be delivered.
// ─────────────────────────────────────────────────────────────────────────────

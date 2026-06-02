# ⚠️ DEPRECATED — this is NOT the shipping codebase

The app that actually ships and deploys is the **Flutter project at `~/Desktop/refreshme`**.
Do your work there, not here.

## ⚠️ Android: do NOT build/install this repo's native app

`RefreshMeApp/app` (native Kotlin Android) is **retired**. Android now ships from the **Flutter**
build in `~/Desktop/refreshme` (it builds both iOS and Android). Both apps use the same
`applicationId` (`com.refreshmeapp.stylist`), so installing this native build over the Flutter
build (or vice-versa) causes `INSTALL_FAILED_VERSION_DOWNGRADE`. The Flutter app is ahead on
version (`pubspec 3.0.25+80` → versionCode 80; this native repo is stuck at versionCode 77).

- Build/run/ship Android from `~/Desktop/refreshme`: `flutter run` (debug) / `flutter build appbundle` (release).
- Do NOT run `./gradlew :app:installDebug` here.
- Migration caveat: the Flutter release build MUST be signed with the SAME keystore as the
  existing Play Store listing (the key that signed `RefreshMe-3.0.19-vc73.aab`). Verify
  `refreshme/android/key.properties` points to that same `.jks` + alias before shipping.

## What ships where

| Layer | Canonical location | Notes |
|---|---|---|
| Android + iOS app | `~/Desktop/refreshme` (Flutter) | Builds both platforms. Runs on devices via `flutter run`. |
| Cloud Functions (deployed backend) | `~/Desktop/refreshme/functions/index.js` | This is what `firebase deploy` ships (`firebase.json` → `"source": "functions"`). Runtime nodejs22. |
| Firestore / Storage rules (deployed) | `~/Desktop/refreshme/firestore.rules`, `~/Desktop/refreshme/storage.rules` | Deployed copies live in the Flutter project. |

## What this `RefreshMeApp` repo is

A legacy/parallel copy: native Kotlin + Compose Android app plus a **TypeScript** `functions/src/index.ts`
that is **not deployed**. The `flutter_ios/` folder here is an integration-guide copy, not the live iOS source.

## Why this matters (real example)

A virtual-try-on fix was once applied to `app/.../tryon/VirtualTryOnFragment.kt` and validated against
`functions/src/index.ts` in THIS repo — but neither ships. The live client is
`~/Desktop/refreshme/lib/screens/customer/virtual_tryon_screen.dart` and the live backend is
`~/Desktop/refreshme/functions/index.js`, which only requires `{ base64Image, prompt }`. Tracing the
wrong files wasted a full pass. Start from `~/Desktop/refreshme`.

## Virtual try-on contract (live, for reference)

- Client sends: `base64Image`, `prompt`.
- Deployed `runVirtualTryOn` requires only those two; it hardcodes `flux-kontext-pro` (no `modelVersion`).
- `runVirtualTryOn` sets `enforceAppCheck: false`; the other 6 callables enforce App Check via `callableOpts`.

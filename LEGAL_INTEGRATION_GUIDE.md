# RefreshMe Legal Module — Integration Guide

This module adds in-app legal protection: click-wrap Terms / Privacy at signup,
Stylist Contractor Agreement + FCRA background-check consent at stylist
onboarding, and an At-Home Liability Waiver + Allergy Disclosure at booking
checkout. Every acceptance is recorded to Firestore with a timestamp, version,
and typed-name signature (where applicable) as a legally defensible audit trail.

## Package contents

Location: `app/src/main/java/com/refreshme/legal/`

| File | Purpose |
|---|---|
| `LegalVersions.kt` | Central version registry for every legal doc |
| `LegalAcceptance.kt` | `LegalAcceptance` + `AcceptanceStatus` data classes |
| `LegalRepository.kt` | Reads/writes `/legalAcceptances/{uid}/acceptances/{docKey}` |
| `CustomerTermsAcceptanceScreen.kt` | Signup: 18+ gate + ToS + Privacy click-wrap |
| `StylistContractorAgreementScreen.kt` | Stylist onboarding: contract + typed signature |
| `FcraConsentScreen.kt` | Standalone FCRA background-check disclosure + authorization |
| `AtHomeWaiverDialog.kt` | Checkout: liability waiver for at-home bookings |
| `AllergyDisclosureDialog.kt` | Checkout: allergy/patch-test ack for chemical services |
| `WaiverCheckboxes.kt` | Reusable check-row composables |

## 1. Firestore rules (REQUIRED before shipping)

`firestore.rules` already has the new `/legalAcceptances` block appended.
After pulling this change, deploy from the repo root:

```bash
firebase deploy --only firestore:rules
```

Rule summary:
- Path: `/legalAcceptances/{uid}/acceptances/{docKey}`
- Read/write: only the authenticated owner (`request.auth.uid == uid`)
- Create enforces `userId == uid` and `docKey` matches path segment
- **Delete is denied** — acceptance records are immutable by design
- Update is allowed so re-acceptance of a new version overwrites prior record

## 2. Data written

### Per-acceptance document (`/legalAcceptances/{uid}/acceptances/{docKey}`)

```
userId:             String    (must equal {uid} path segment)
docKey:             String    (must equal {docKey} path segment)
docVersion:         String    (from LegalVersions — e.g., "2026-04-22")
signedName:         String    (typed signature, "" for checkbox-only flows)
acceptedOnPlatform: "android"
appVersion:         String    (BuildConfig.VERSION_NAME)
ipHint:             String    (optional — leave "" on mobile)
acceptedAt:         Timestamp (server-side)
```

### Per-booking snapshot (added to existing `/bookings/{id}` doc)

```
waiverAcceptedVersion:         String  ("" if not at-home)
waiverAcceptedAt:              Long    (ms epoch; 0 if not accepted)
allergyDisclosureVersion:      String  ("" if not a chemical service)
allergyDisclosureAcceptedAt:   Long    (ms epoch; 0 if not accepted)
```

The booking-level snapshot is what you'd hand to counsel in a dispute — it
captures the exact waiver version in force at the moment the user confirmed
THAT booking.

## 3. Wiring each screen into existing flows

### 3a. Customer signup — Terms + Privacy click-wrap

Built: `CustomerTermsAcceptanceScreen(onAccepted, onExit)`

Wire it into `com/refreshme/auth/CustomerOnboardingActivity.kt` (or
`CustomerOnboardingViewModel`). Recommended: after email/password entry
succeeds but BEFORE `createUserWithEmailAndPassword` commits the profile —
so no user can end up signed in without a recorded ToS + Privacy acceptance.

```kotlin
when (step) {
    Step.ENTER_CREDENTIALS -> EnterCredentialsScreen(
        onNext = { step = Step.ACCEPT_TERMS }
    )
    Step.ACCEPT_TERMS -> CustomerTermsAcceptanceScreen(
        onAccepted = {
            viewModel.finishSignup()   // creates auth user + /users doc
            step = Step.DONE
        },
        onExit = { finish() }          // declined → do NOT create account
    )
}
```

Existing users (signed up before this version) are handled automatically by
`LegalGateActivity` — see §3d. No per-caller code needed.

Bump the version strings in `LegalVersions.kt` whenever the ToS or Privacy
text materially changes — every user is then forced to re-accept on next launch.

### 3b. Stylist onboarding — Contractor Agreement + FCRA Consent

Onboarding lives in `com/refreshme/auth/StylistOnboardingActivity.kt` /
`StylistOnboardingViewModel.kt`. Insert two new steps AFTER identity
verification and BEFORE Stripe Connect:

```
Identity verification (existing)
  → StylistContractorAgreementScreen (NEW)
  → FcraConsentScreen                 (NEW — standalone, required by FCRA)
  → Stripe Connect onboarding (existing)
```

**Why this order:** the contractor agreement establishes that the stylist is
NOT an employee before they get paid. FCRA (15 USC 1681b(b)) requires the
background-check disclosure to be on a **standalone** screen with nothing
else on it — hence it is its own screen, not a checkbox on the contract.

```kotlin
Step.CONTRACTOR_AGREEMENT -> StylistContractorAgreementScreen(
    onAccepted = { step = Step.FCRA_CONSENT },
    onDeclined = { finish() }      // cannot proceed as stylist
)
Step.FCRA_CONSENT -> FcraConsentScreen(
    onConsented = { step = Step.STRIPE_ONBOARDING },
    onDeclined = { finish() }
)
```

Existing stylists are handled automatically by `LegalGateActivity` — see §3d.

### 3c. Booking checkout — At-Home Waiver + Allergy Disclosure

Already wired end-to-end in `BookingScreen.kt` + `NewBookingViewModel.kt`.
Behavior:

1. User taps **Confirm Booking** on `BookingScreen`.
2. If `isMobileBooking == true` AND the at-home waiver hasn't been accepted
   this session → show `AtHomeWaiverDialog`. User must check 3 boxes and tap
   "I Accept." Dialog records acceptance to `/legalAcceptances/{uid}/acceptances/AT_HOME_LIABILITY_WAIVER`.
3. If the service matches chemical-service keywords (color, bleach, relaxer,
   perm, keratin, etc. — see `serviceRequiresAllergyDisclosure(...)` in
   `AllergyDisclosureDialog.kt`) AND the allergy disclosure hasn't been
   accepted → show `AllergyDisclosureDialog`. Acceptance recorded under
   `docKey = ALLERGY_DISCLOSURE`.
4. Both dialogs pass their accepted version string back to the screen, which
   hands them to `viewModel.createBooking(waiverAcceptedVersion, allergyDisclosureVersion)`.
5. `NewBookingViewModel.createBooking` writes 4 snapshot fields onto the
   booking doc (see §2) and forwards to the `createBookingPaymentIntent`
   Cloud Function as part of the existing payload.

**No further wiring needed for booking** — shipping the new files + the
updated `BookingScreen` / `NewBookingViewModel` diffs is sufficient.

## 4. Legal doc versioning (how to update text)

Everything keys off `LegalVersions.kt`. Workflow to update any doc:

1. Edit the on-screen text (e.g., tweak the Contractor Agreement body in
   `StylistContractorAgreementScreen.kt`).
2. Bump that doc's version string in `LegalVersions.kt`
   (convention: ISO date of the change, e.g., `"2026-04-22"` → `"2026-06-01"`).
3. If the change is customer-facing (ToS / Privacy), also update the
   matching page at `refreshme-74f79.web.app/terms` / `/privacy` so the
   external web version matches the in-app version.
4. Next app launch, `needsAcceptance(...)` will return true for every user
   on the old version — they are forced to re-accept before they can use
   the affected feature.

**Do NOT change version strings without changing the text** — you'll force
meaningless re-acceptances and erode trust.

## 5. What's NOT in this module (and where to put it)

- **Payout / tax forms (W-9, 1099-NEC):** handled by Stripe Connect's own
  onboarding — don't reimplement.
- **COPPA (under-13):** the 18+ gate in `CustomerTermsAcceptanceScreen`
  covers this. Do not lower the age floor without legal review.
- **GDPR / CCPA data-deletion requests:** add a "Delete my account" button
  in Settings that triggers a Cloud Function to scrub `/users/{uid}`,
  `/stylists/{uid}`, and retained booking records per your retention policy.
  (The `/legalAcceptances` records should be retained even after account
  deletion — they're your defense evidence. Scrub PII fields only.)
- **Insurance:** see `RefreshMe_Insurance_Requirements_Checklist.docx` —
  stylists must upload proof of liability insurance during onboarding.
  That upload UI is a separate follow-up task.

## 6. Testing checklist

Before release:

- [ ] `firebase deploy --only firestore:rules` succeeds
- [ ] New customer signup writes both CUSTOMER_TOS and PRIVACY_POLICY
      acceptance docs in Firestore console
- [ ] Declining at the terms screen does NOT create an auth user
- [ ] New stylist onboarding writes STYLIST_CONTRACTOR_AGREEMENT and
      FCRA_CONSENT acceptance docs, each with `signedName` non-empty
- [ ] At-home booking triggers the waiver dialog; confirming writes
      AT_HOME_LIABILITY_WAIVER acceptance AND puts
      `waiverAcceptedVersion` on the booking doc
- [ ] Color/bleach service triggers the allergy dialog AFTER the at-home
      waiver (if both apply)
- [ ] Non-at-home, non-chemical booking confirms directly with both
      version fields as empty strings on the booking doc
- [ ] Bumping `LegalVersions.CUSTOMER_TOS` forces existing test user
      to re-accept on next launch
- [ ] Attempting to delete a legalAcceptances doc via Firestore client
      returns PERMISSION_DENIED

## 7. Companion DOCX files

The original offline legal documents live in the RefreshMe Legal project
folder and should be kept in sync with the in-app text:

- `RefreshMe_Stylist_Independent_Contractor_Agreement.docx`
- `RefreshMe_Background_Check_Verification_Consent.docx`
- `RefreshMe_Customer_Liability_Waiver_AtHome.docx`
- `RefreshMe_Insurance_Requirements_Checklist.docx`

Treat the DOCX files as the canonical "paper" version. If counsel edits
them, port the change into the matching Compose screen AND bump the
version string.

### 3d. Launch-time re-acceptance gate (existing users)

`LegalGateActivity` enforces current acceptances for anyone who was already
using the app before a given version shipped, and for anyone still on an
older version after `LegalVersions.*` is bumped.

How it works:

- `MainActivity.onCreate` launches `LegalGateActivity` (via activity-result
  contract) once per process, if the user is authenticated.
- `StylistDashboardActivity.onCreate` does the same for the stylist flow.
- The gate reads `users/{uid}.role` to know whether to also check
  `STYLIST_CONTRACTOR_AGREEMENT` and `FCRA_BACKGROUND_CHECK_CONSENT`.
- If every required version is current, the gate finishes immediately with
  `RESULT_OK` (fast path — user sees a brief spinner at most).
- If anything is outdated, the relevant screen(s) are shown in order:
  customer ToS + Privacy → contractor agreement → FCRA consent.
- **Accept path:** each screen records its acceptance via `LegalRepository`,
  then moves to the next. After the last, gate returns `RESULT_OK` and the
  hosting activity proceeds normally.
- **Decline path:** `FirebaseAuth.signOut()` is called, `RESULT_CANCELED` is
  returned, and the hosting activity clears its task stack back to the
  role-select / sign-in screen. The user cannot reach the authenticated
  part of the app without current acceptances on file.

Registered in `AndroidManifest.xml` as:

```xml
<activity
    android:name=".legal.LegalGateActivity"
    android:exported="false" />
```

Because the gate is process-scoped (`legalGateChecked` flag on each host
activity), navigating between tabs or fragments does not re-trigger it.
Relaunching the app from cold start does.

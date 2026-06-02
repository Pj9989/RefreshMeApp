#!/usr/bin/env python3
"""
Upload AAB to Google Play Store via the Android Publisher API.

SETUP (one-time, only needed when credentials expire or scopes change):
  gcloud auth application-default login \
    --scopes=https://www.googleapis.com/auth/androidpublisher,\
https://www.googleapis.com/auth/cloud-platform,\
https://www.googleapis.com/auth/userinfo.email

  gcloud auth application-default set-quota-project refreshme-app

  gcloud services enable androidpublisher.googleapis.com --project=refreshme-app

USAGE:
  python3 upload_to_play_store.py

REQUIREMENTS:
  pip3 install google-auth google-auth-httplib2 google-api-python-client
"""

import os
import sys
import json

# ── Config ────────────────────────────────────────────────────────────────────
PACKAGE_NAME = "com.refreshmeapp.stylist"
AAB_PATH     = "/Users/Phill/Desktop/RefreshMeApp/app/build/outputs/bundle/release/app-release.aab"
TRACK        = "production"   # "production" | "beta" | "alpha" | "internal"
# ─────────────────────────────────────────────────────────────────────────────

try:
    import google.auth
    import google.auth.transport.requests
    from googleapiclient.discovery import build
    from googleapiclient.http import MediaFileUpload
    from googleapiclient.errors import HttpError
except ImportError as e:
    print(f"Missing dependency: {e}")
    print("Run: pip3 install google-auth google-auth-httplib2 google-api-python-client")
    sys.exit(1)

if not os.path.exists(AAB_PATH):
    print(f"AAB not found at: {AAB_PATH}")
    print("Build it first: cd RefreshMeApp && ./gradlew bundleRelease")
    sys.exit(1)

print("Loading credentials...")
credentials, _ = google.auth.default(
    scopes=["https://www.googleapis.com/auth/androidpublisher"]
)
credentials.refresh(google.auth.transport.requests.Request())
print(f"  Credentials valid")

service = build("androidpublisher", "v3", credentials=credentials)

# Step 1 - Create an edit session
print("\nCreating edit...")
edit = service.edits().insert(packageName=PACKAGE_NAME, body={}).execute()
edit_id = edit["id"]
print(f"  Edit ID: {edit_id}")

try:
    # Step 2 - Upload the AAB
    size_mb = os.path.getsize(AAB_PATH) / 1024 / 1024
    print(f"\nUploading AAB ({size_mb:.1f} MB)...")
    media = MediaFileUpload(
        AAB_PATH,
        mimetype="application/octet-stream",
        resumable=True,
        chunksize=10 * 1024 * 1024,  # 10 MB chunks
    )
    bundle = service.edits().bundles().upload(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        media_body=media,
    ).execute()
    version_code = bundle["versionCode"]
    print(f"  Uploaded - versionCode: {version_code}")

    # Step 3 - Assign to the track
    print(f"\nAssigning to '{TRACK}' track...")
    service.edits().tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track=TRACK,
        body={
            "track": TRACK,
            "releases": [{
                "versionCodes": [version_code],
                "status": "completed",
            }],
        },
    ).execute()
    print(f"  Track updated")

    # Step 4 - Commit the edit
    print("\nCommitting edit...")
    service.edits().commit(
        packageName=PACKAGE_NAME,
        editId=edit_id,
    ).execute()
    print(f"  Committed")
    print(f"\nSuccessfully published versionCode {version_code} to the '{TRACK}' track!")

except HttpError as e:
    err = json.loads(e.content.decode()).get("error", {})
    print(f"\nAPI error {e.resp.status}: {err.get('message', str(e))}")
    try:
        service.edits().delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
        print("  (draft edit deleted)")
    except Exception:
        pass
    sys.exit(1)

except Exception as e:
    print(f"\nUnexpected error: {e}")
    try:
        service.edits().delete(packageName=PACKAGE_NAME, editId=edit_id).execute()
    except Exception:
        pass
    sys.exit(1)

#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Refusing to rewrite history while the working tree has pending changes."
  echo "Commit or stash the current work first, then rerun this script."
  exit 1
fi

if [[ "${CONFIRM_PURGE_SIGNING_HISTORY:-}" != "yes" ]]; then
  echo "This rewrites git history to remove old Android signing files."
  echo "Create a backup first. Then run:"
  echo "CONFIRM_PURGE_SIGNING_HISTORY=yes scripts/purge-signing-history.sh"
  exit 1
fi

git filter-branch --force \
  --index-filter 'git rm -r --cached --ignore-unmatch app/new-upload-keystore.jks app/upload.keystore app/signing.keystore app/signing_output.txt app/temp_debug_signing.txt app/temp_release_signing.txt app/sha256_out.txt app/release_sha1.txt' \
  --prune-empty \
  --tag-name-filter cat \
  -- --all

rm -rf .git/refs/original/
git reflog expire --expire=now --all
git gc --prune=now --aggressive

echo "Signing files were purged from local git history."
echo "Next: rotate the Play upload key, then force-push cleaned refs with lease."

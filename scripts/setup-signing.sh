#!/bin/sh
# One-off: creates the release keystore outside the repo and stores it as GitHub secrets.
set -e
d="$HOME/.android-keys"; f="$d/translate-selected-android.jks"
mkdir -p "$d"; chmod 700 "$d"
if [ -e "$f" ]; then echo "keystore already exists: $f" >&2; exit 1; fi
pw=$(openssl rand -base64 24 | tr -d '/+=')
keytool -genkeypair -keystore "$f" -storetype PKCS12 -storepass "$pw" -alias release \
    -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Conan Torres, O=translate-selected-android"
printf 'keystore: %s\nalias: release\npassword (store and key): %s\n' "$f" "$pw" > "$d/translate-selected-android.txt"
chmod 600 "$f" "$d/translate-selected-android.txt"
base64 -w0 "$f" | gh secret set KEYSTORE_BASE64 -R C0nanT/translate-selected-android
printf %s "$pw" | gh secret set KEYSTORE_PASSWORD -R C0nanT/translate-selected-android
gh secret list -R C0nanT/translate-selected-android
echo "ok. back up $d somewhere safe: without it, updates of the installed app are refused."

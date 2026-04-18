#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_DIR="$ROOT_DIR/.keystore"
KEYSTORE_PATH="$KEYSTORE_DIR/gemini-mobile.jks"

mkdir -p "$KEYSTORE_DIR"

: "${GEMINI_KEYSTORE_PASSWORD:=changeit123}"
: "${GEMINI_KEY_ALIAS:=gemini_mobile}"
: "${GEMINI_KEY_PASSWORD:=$GEMINI_KEYSTORE_PASSWORD}"

if [ ! -f "$KEYSTORE_PATH" ]; then
  keytool -genkeypair \
    -storetype JKS \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$GEMINI_KEYSTORE_PASSWORD" \
    -keypass "$GEMINI_KEY_PASSWORD" \
    -alias "$GEMINI_KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 3650 \
    -dname "CN=Gemini CLI Mobile,O=GeminiMobile,C=US"
fi

export GEMINI_KEYSTORE_PATH="$KEYSTORE_PATH"
export GEMINI_KEYSTORE_PASSWORD
export GEMINI_KEY_ALIAS
export GEMINI_KEY_PASSWORD

cd "$ROOT_DIR"
gradle assembleRelease

echo "Signed APK generated at app/build/outputs/apk/release/app-release.apk"

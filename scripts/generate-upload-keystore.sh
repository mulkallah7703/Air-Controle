#!/usr/bin/env bash
# Creates a local DEV/TEST upload keystore. Not for Play production App Signing.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

KEYSTORE="${ROOT}/aircontrole-upload.jks"
PROPS="${ROOT}/keystore.properties"

if [[ -f "$KEYSTORE" ]]; then
  echo "Already exists: $KEYSTORE"
  echo "Delete it first if you intend to rotate the DEV/TEST key."
  exit 1
fi

STORE_PASS="$(openssl rand -base64 24 | tr -d '/+=' | head -c 24)"
KEY_PASS="$STORE_PASS"
ALIAS="aircontrole"

keytool -genkeypair -v \
  -keystore "$KEYSTORE" \
  -storetype JKS \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias "$ALIAS" \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=Air Controle DEV TEST, OU=Air Controle, O=Mulkallah, L=Riyadh, ST=Riyadh, C=SA"

cat > "$PROPS" <<EOF
storeFile=aircontrole-upload.jks
storePassword=${STORE_PASS}
keyAlias=${ALIAS}
keyPassword=${KEY_PASS}
EOF

chmod 600 "$KEYSTORE" "$PROPS"
echo "Wrote $KEYSTORE and $PROPS (both gitignored)."
echo "Build with: ./gradlew assembleRelease"

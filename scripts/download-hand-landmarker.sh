#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/camera/src/main/assets/hand_landmarker.task"
URL="https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
mkdir -p "$(dirname "$DEST")"
if [[ -f "$DEST" ]] && [[ "$(wc -c < "$DEST")" -gt 1000000 ]]; then
  echo "Already present: $DEST"
  exit 0
fi
echo "Downloading $URL"
curl -L --fail --retry 3 -o "$DEST" "$URL"
echo "Saved $DEST ($(wc -c < "$DEST") bytes)"

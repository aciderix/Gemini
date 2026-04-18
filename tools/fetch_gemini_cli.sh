#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="$ROOT_DIR/third_party/gemini-cli"

if [ -d "$TARGET_DIR/.git" ]; then
  echo "gemini-cli already present in $TARGET_DIR"
  exit 0
fi

git clone --depth=1 https://github.com/google-gemini/gemini-cli "$TARGET_DIR"
echo "Cloned gemini-cli into $TARGET_DIR"

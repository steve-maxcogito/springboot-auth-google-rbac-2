#!/usr/bin/env bash
set -euo pipefail

# 1) What to package (default: current dir)
PROJECT_DIR="${1:-.}"
PROJECT_DIR="$(cd "$PROJECT_DIR" && pwd)"

# 2) Where to put the archive (default: project's parent)
OUT_DIR="${2:-"$(cd "$PROJECT_DIR/.." && pwd)"}"
mkdir -p "$OUT_DIR"

STAMP="$(date +%Y%m%d-%H%M)"
OUT_FILE="$OUT_DIR/$(basename "$PROJECT_DIR")-$STAMP.tgz"

tar -czf "$OUT_FILE" \
  --exclude='node_modules' \
  --exclude='.git' \
  --exclude='dist' \
  --exclude='build' \
  --exclude='.next' \
  --exclude='coverage' \
  --exclude='*.DS_Store' \
  --exclude='.env*' \
  -C "$PROJECT_DIR" .

echo "Wrote $OUT_FILE"


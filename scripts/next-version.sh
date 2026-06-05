#!/usr/bin/env bash
# Computes the next date-based version: YYYY.M.PATCH (no leading zero on the month).
# Finds the latest tag for the current YYYY.M and increments PATCH (starts at .0).
set -euo pipefail
TODAY="$(date +%Y.%-m)"
LATEST="$(git tag --list "${TODAY}.*" 2>/dev/null | sort -V | tail -1)"
if [ -z "$LATEST" ]; then
  echo "${TODAY}.0"
else
  PATCH="${LATEST##*.}"
  echo "${TODAY}.$((PATCH + 1))"
fi

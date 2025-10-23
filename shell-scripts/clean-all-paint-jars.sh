#!/bin/bash
set -e

# ============================================================
# 🧹 CLEAN PAINT JAR FILES
# ============================================================
# This script removes all compiled or deployed Paint-related JARs
# from:
#   1. Local project folders
#   2. Maven local repository (~/.m2)
#   3. Fiji installation (if present)
#   4. User Applications folder (Glyco-PAINT app bundles)
#
# Useful before rebuilding or recreating releases.
#
# Run this script from anywhere:
#   ./shell-scripts/clean-paint-jars.sh
# ============================================================

echo "🧹 Removing Paint JARs from project, Maven cache, Fiji, and app bundles..."

find /Users/Hans/JavaPaintProjects -type f -name "*paint*.jar" -exec rm -v {} \;
find /Users/Hans/.m2 -type f -name "*paint*.jar" -exec rm -v {} \;
find /Applications/Fiji.App -type f -name "*paint*.jar" -exec rm -v {} \;
find /Users/hans/Applications/Glyco-PAINT -type f -name "paint*.jar" -exec rm -v {} \;

echo "✅ All Paint JARs cleaned successfully!"
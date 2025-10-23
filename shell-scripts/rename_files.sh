#!/bin/bash

# Base directory — change this if needed
ROOT="/Users/hans/Paint Test Project/"

# Find and rename
find "$ROOT" -type f -name "All Recordings Java.csv" | while read -r file; do
    dir=$(dirname "$file")
    new="$dir/Recordings.csv"
    echo "Renaming: $file → $new"
    mv "$file" "$new"
done

echo "✅ Done."
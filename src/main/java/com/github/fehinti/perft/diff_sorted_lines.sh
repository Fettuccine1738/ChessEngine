#!/bin/bash

# Usage: ./diff_sorted_lines.sh file1.txt file2.txt

if [ $# -ne 2 ]; then
    echo "Usage: $0 file1 file2"
    exit 1
fi

file1="$1"
file2="$2"

# Create sorted temp files
sorted1=$(mktemp)
sorted2=$(mktemp)

sort "$file1" > "$sorted1"
sort "$file2" > "$sorted2"

# Use comm to compare and format output
echo "=== Lines only in $file1 ==="
comm -23 "$sorted1" "$sorted2"

echo
echo "=== Lines only in $file2 ==="
comm -13 "$sorted1" "$sorted2"

# Cleanup
rm "$sorted1" "$sorted2"


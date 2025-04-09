#!/bin/bash


# Parameters received from perftree
depth="$1"
fen="$2"
moves="$3"

#!/bin/bash
echo "[DEBUG] Bash script invoked"
echo "depth=$1"
echo "fen=$2"
echo "moves=$3"

@echo off
echo [DEBUG] Running bash with the following command:
"C:\Program Files\Git\bin\bash.exe" -c "/c/Users/favya/IdeaProjects/ChessEngine/p_debug_3.sh %*"


# Call your Java perft test class
# Adjust the class path and class name as needed
java -cp "target/classes" com.github.fehinti.perft.Perft "$depth" "$fen" "$moves"
#java -cp "C:/Users/favya/IdeaProjects/ChessEngine/src/main/jav/*.java" Perft.java

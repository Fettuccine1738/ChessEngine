# ChessEngine
A chess engine built using resources from the online wiki *chessprogrammingwiki.com*.

## Features

- **Mailbox Board Representation** — Square-centric 10×12 byte array encoding guard bands, piece data, and square info
- **Forsyth-Edwards Notation (FEN)** — Helper class for initializing board state from FEN strings, used for testing arbitrary positions
- **Move Generation** — Sliding and non-sliding piece movement based on directional orientation
- **Performance** — ~3.2 million nodes/second
- **Mate Solver** — Reliably solves mate-in-4 puzzles in under a minute

## Sample Output

```
Time limit reached at depth 9 — 85,339,517 nodes searched
Best eval: NaN
Best move: d7d1
```

```
 +---+---+---+---+---+---+---+---+
 |   |   |   |   |   |   |   |   | 8
 +---+---+---+---+---+---+---+---+
 |[k]|   |   | r |   |   |   |   | 7
 +---+---+---+---+---+---+---+---+
 | p |   |   |   |   |   |   |   | 6
 +---+---+---+---+---+---+---+---+
 |   |   | b |   | B | p |   |   | 5
 +---+---+---+---+---+---+---+---+
 | P |   |   |   | p |   |   |   | 4
 +---+---+---+---+---+---+---+---+
 | q | p |   |   |   |   | R |   | 3
 +---+---+---+---+---+---+---+---+
 |   |   |   |   | Q | P |   |   | 2
 +---+---+---+---+---+---+---+---+
 |   |[K]|   |   |   |   |   |   | 1
 +---+---+---+---+---+---+---+---+
   a   b   c   d   e   f   g   h

FEN: 8/k2r4/p7/2b1Bp2/P3p3/qp4R1/4QP2/1K6 b - - 0 1
```

**Principal variation:**
```
d7d1  e2d1  a3a2  b1c1  c5a3  e5b2  a2b2  c1d2
```

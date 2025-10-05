# ChessEngine *Ongoing*
Chess engine using Object Oriented concepts.

- Square centric Mailboard implemantaion using  10 x 12 array of bytes that encodes guard bands, piece and square info

- Forsyth Edward Notation: helper class that creates board from an FEN string, utilized for testing different positions.

- Move generation for pieces based on move orientation(sliding and non sliding).
- 3.2 million nodes / second. 
- ABILITY: Easily solves mate in 4  in  under a minute. 
- 
- ```java
     
Time limit reached with 9 and searched. 85339517
2025-10-05 19:38:33 INFO  best eval NaN
2025-10-05 19:38:33 INFO  Best Move d7d1
 +---+---+---+---+---+---+---+---+
 8	|   |   |   |   |   |   |   |   |
 +---+---+---+---+---+---+---+---+
 7	|[k]|   |   | r |   |   |   |   |
 +---+---+---+---+---+---+---+---+
 6	| p |   |   |   |   |   |   |   |
 +---+---+---+---+---+---+---+---+
 5	|   |   | b |   | B | p |   |   |
 +---+---+---+---+---+---+---+---+
 4	| P |   |   |   | p |   |   |   |
 +---+---+---+---+---+---+---+---+
 3	| q | p |   |   |   |   | R |   |
 +---+---+---+---+---+---+---+---+
 2	|   |   |   |   | Q | P |   |   |
 +---+---+---+---+---+---+---+---+
 1	|   |[K]|   |   |   |   |   |   |
 +---+---+---+---+---+---+---+---+
 a   b   c   d   e   f   g   h
 8/k2r4/p7/2b1Bp2/P3p3/qp4R1/4QP2/1K6 b - - 0 1
 d7d1
 e2d1
 a3a2
 b1c1
 c5a3
 e5b2
 a2b2
 c1d2
- ```

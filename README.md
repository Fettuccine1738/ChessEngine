# ChessEngine *Ongoing*
Chess engine using Object Oriented concepts.

- Square centric Mailboard implemantaion using  10 x 12 array of bytes that encodes guard bands, piece and square info

- Forsyth Edward Notation: helper class that creates board from an FEN string, utilized for testing different positions.

- Move generation for pieces based on move orientation(sliding and non sliding).

- Currently searches accurately up to depth 7 (~3 billion nodes from a start position in 15 minutes *perft result*).
- 3.2 million nodes / second. 

- GOAL: efficiently search up to depth 10, then alphabeta search will be implemented. 
- 
- ```java
  	+---+---+---+---+---+---+---+---+
  8	| r | n | b | q |[k]| b | n | r |
          +---+---+---+---+---+---+---+---+
  7	| p | p | p | p | p | p | p | p |
          +---+---+---+---+---+---+---+---+
  6	|   |   |   |   |   |   |   |   |
          +---+---+---+---+---+---+---+---+
  5	|   |   |   |   |   |   |   |   |
          +---+---+---+---+---+---+---+---+
  4	|   |   |   |   |   |   |   |   |
          +---+---+---+---+---+---+---+---+
  3	|   |   |   |   |   |   |   |   |
          +---+---+---+---+---+---+---+---+
  2	| P | P | P | P | P | P | P | P |
          +---+---+---+---+---+---+---+---+
  1	| R | N | B | Q |[K]| B | N | R |
          +---+---+---+---+---+---+---+---+
            a   b   c   d   e   f   g   h
  
  EnPassant:	-
  Castles: KQkq
  fullCount = 1
  halfMove = 0
  Side:	white
  
  go perft 7
  a2a3 :	106743106
  a2a4 :	137077337
  b2b3 :	133233975
  b2b4 :	134087476
  c2c3 :	144074944
  c2c4 :	157756443
  d2d3 :	227598692
  d2d4 :	269605599
  e2e3 :	306138410
  e2e4 :	309478263
  f2f3 :	102021008
  f2f4 :	119614841
  g2g3 :	135987651
  g2g4 :	130293018
  h2h3 :	106678423
  h2h4 :	138495290
  b1a3 :	120142144
  b1c3 :	148527161
  g1f3 :	147678554
  g1h3 :	120669525
  TOtal 3195901860
  Duration : 942 seconds
  0 seconds
  captures 108329926
  Checks: 33103848
  Enpassant 319617
  Castles: 883453
- ```

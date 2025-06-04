# ChessEngine *Ongoing*
Chess engine using Object Oriented concepts.

- Square centric board using  8 x 8 array of enums for each piece and color combination. Additional static 10 x 12 and 8 x 8 arrays for off board move generation. 

- Forsyth Edward Notation: helper class that creates board from an FEN string, utilized for testing different positions.

- Move generation for pieces based on move orientation(sliding and non sliding).

- Currently searches up to depth 6 (~119 million nodes from a start position). Depth 7 is possible but inefficient (~3 billion nodes in 20 minutes.)

- GOAL: efficiently search up to depth 10, then alphabeta search will be implemented. 

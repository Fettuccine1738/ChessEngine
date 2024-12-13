package piece;

import board.Board;
import board.Move;

import java.util.Collection;
import java.util.List;

public class Pawn implements Piece {
    @Override
    public Collection<Move> possibleMoves(int file, int rank, Board p) {
        return List.of();
    }


}

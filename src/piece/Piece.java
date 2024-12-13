package piece;

import board.Board;
import board.Move;
import java.util.Collection;

public interface Piece {
    public Collection<Move> possibleMoves(int file, int rank, Board b);
}

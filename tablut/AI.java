package tablut;
import java.util.List;

import static tablut.Piece.*;

/** A Player that automatically generates moves.
 *  @author Abel Yagubyan
 */
class AI extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A position-score magnitude indicating a forced win in a subsequent
     *  move.  This differs from WINNING_VALUE to avoid putting off wins. */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI with no piece or controller (intended to produce
     *  a template). */
    AI() {
        this(null, null);
    }

    /** A new AI playing PIECE under control of CONTROLLER. */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        Move mv = findMove();
        _controller.reportMove(mv);
        return mv.toString();
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board board = new Board(board());
        if (_myPiece == WHITE) {
            findMove(board, maxDepth(board), true, 1, -INFTY, INFTY);
        } else {
            findMove(board, maxDepth(board), true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        if (board.winner() != null || depth == 0) {
            return staticScore(board);
        }
        List<Move> moveit = board.legalMoves(board.turn());
        for (Move move: moveit) {
            board.makeMove(move);
            int val = findMove(board, depth - 1, false, -sense, alpha, beta);
            if (sense == -1) {
                if (beta > val) {
                    beta = val;
                    if (saveMove) {
                        _lastFoundMove = move;
                    }
                }
                if (alpha >= beta) {
                    board.undo();
                    return val;
                }
            } else if (sense == 1) {
                if (alpha < val) {
                    alpha = val;
                    if (saveMove) {
                        _lastFoundMove = move;
                    }
                }
                if (alpha >= beta) {
                    board.undo();
                    return val;
                }
            }
            board.undo();
        }
        if (sense == 1) {
            return alpha;
        }
        return beta;
    }

    /** Return a heuristically determined maximum search depth
     *  based on characteristics of BOARD. */
    private static int maxDepth(Board board) {
        int val = (2 * board.SIZE + 2) * 2;
        int val2 = val + (3 * board.SIZE);
        int movecount = board.moveCount();
        if (movecount <= val) {
            return 1;
        } else if (movecount >= val2) {
            return 5;
        } else {
            return 3;
        }
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        Piece win = board.winner();
        if (win == WHITE) {
            return WINNING_VALUE;
        } else if (win == BLACK) {
            return -WINNING_VALUE;
        } else {
            List<Move> movewhite = board.legalMoves(WHITE);
            List<Move> moveblack = board.legalMoves(BLACK);
            List<Move> moveking = board.legalMoves(KING);
            int whitenum = movewhite.size();
            int blacknum = moveblack.size();
            int kingnum = moveking.size();
            return whitenum + kingnum - blacknum;
        }
    }
}


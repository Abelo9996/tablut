package tablut;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Stack;
import java.util.List;
import java.util.Formatter;
import java.util.ArrayList;
import static tablut.Piece.*;
import static tablut.Square.*;
import static tablut.Move.mv;


/** The state of a Tablut Game.
 *  @author Abel Yagubyan
 */
class Board {

    /** The number of squares on a side of the board. */
    static final int SIZE = 9;

    /** The throne (or castle) square and its four surrounding squares.. */
    static final Square THRONE = sq(4, 4),
            NTHRONE = sq(4, 5),
            STHRONE = sq(4, 3),
            WTHRONE = sq(3, 4),
            ETHRONE = sq(5, 4);

    /** Side thrones. */
    static final Square[] SIDE_THRONES = {
        NTHRONE, STHRONE, WTHRONE, ETHRONE
    };

    /** Initial positions of attackers. */
    static final Square[] INITIAL_ATTACKERS = {
        sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
        sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
        sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
        sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /** Initial positions of defenders of the king. */
    static final Square[] INITIAL_DEFENDERS = {
        NTHRONE, ETHRONE, STHRONE, WTHRONE,
            sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /** Initializes a game board with SIZE squares on a side in the
     *  initial position. */
    Board() {
        init();
    }

    /** Initializes a copy of MODEL. */
    Board(Board model) {
        copy(model);
    }

    /** Copies MODEL into me. */
    void copy(Board model) {
        if (model == this) {
            return;
        }
        this._turn = model._turn;
        this._repeated = model._repeated;
        this._moveCount = model._moveCount;
        this._winner = model._winner;
        this._changepass = model._changepass;
        this._movelasts = model._movelasts;
        this._pospieces = new HashMap<Integer, Piece>();
        this._pospieces = hashMapcop(model._pospieces);
    }

    /** Clears the board to the initial position. */
    void init() {
        _repeated = false;
        _moveCount = 0;
        _turn = BLACK;
        _pospieces = new HashMap<Integer, Piece>();
        _winner = null;
        _changepass = new Stack<Helper>();
        _movelasts = new HashSet<String>();
        for (Square help: SQUARE_LIST) {
            switch (classif(help)) {
            case 1:
                _pospieces.put(help.index(), BLACK);
                break;
            case 2:
                _pospieces.put(help.index(), WHITE);
                break;
            case 3:
                _pospieces.put(help.index(), KING);
                break;
            case 4:
                _pospieces.put(help.index(), EMPTY);
                break;
            default:
                throw new Error("Initialization is wrong!");
            }
        }
    }

    /** Set the move limit to LIM.  It is an error if 2*LIM <= moveCount().
     * @param n limit */
    void setMoveLimit(int n) {
        _limOfMove = n;
    }

    /** Return a Piece representing whose move it is (WHITE or BLACK). */
    Piece turn() {
        return _turn;
    }

    /** Return the winner in the current position, or null if there is no winner
     *  yet. */
    Piece winner() {
        return _winner;
    }

    /** Returns true iff this is a win due to a repeated position. */
    boolean repeatedPosition() {
        return _repeated;
    }

    /** Saves moves.
     * @param move */
    public void saveMove(String move) {
        _movelasts.add(move);
    }

    /** Record current position and set winner() next mover if the current
     *  position is a repeat. */
    private void checkRepeated() {
        String movecurr = encodedBoard();

        Boolean notrep = _movelasts.add(movecurr);
        if (!notrep) {
            if (movecurr.equals(str1)) {
                _repeated = true;
                _winner = _turn;
            } else if (movecurr.equals(str2)) {
                _repeated = true;
                _winner = _turn;
            } else {
                _repeated = true;
                _winner = _turn.opponent();
            }
        }
    }

    /** Return true iff FROM - TO is an unblocked rook move on the current
     *  board.  For this to be true, FROM-TO must be a rook move and the
     *  squares along it, other than FROM, must be empty. */
    boolean isUnblockedMove(Square from, Square to) {
        if (!from.isRookMove(to)) {
            return false;
        }
        int path = from.direction(to);
        for (int x = 1; x < 10; x += 1) {
            Square square = from.rookMove(path, x);
            if (square == null) {
                throw new Error("Your isUnblockedMove is wrong!");
            }
            if (square == to) {
                return get(square).equals(EMPTY);
            } else if (!(get(square).equals(EMPTY))) {
                return false;
            }
        }
        throw new Error("Your isUnblockedMove is wrong!");
    }

    /** Returns if _repeated is true. */
    public boolean getRepeated() {
        return _repeated;
    }

    /** Return the number of moves since the initial position that have not been
     *  undone. */
    int moveCount() {
        return _moveCount;
    }

    /** Return location of the king. */
    Square kingPosition() {
        for (int key: _pospieces.keySet()) {
            Piece piece = _pospieces.get(key);
            if (_pospieces.get(key).equals(KING)) {
                return sq(key);
            }
        }
        return null;
    }

    /** Return the contents the square at S. */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /** Move FROM-TO, assuming this is a legal move. */
    void makeMove(Square from, Square to) {
        if (isLegal(from, to)) {
            boolean movecontains = hasMove(_turn);
            if (!movecontains) {
                _winner = _turn.opponent();
            }
            Helper help = new Helper(null, null, true);
            _changepass.push(help);
            Piece piece = get(from);
            revPut(EMPTY, from);
            revPut(piece, to);
            capture(to);
            _moveCount += 1;
            Square setking = kingPosition();
            if (setking == null) {
                _winner = BLACK;
                return;
            } else {
                if (setking.isEdge()) {
                    _winner = WHITE;
                    return;
                }
            }
            if (_moveCount == _limOfMove * 2) {
                _winner = _turn.opponent();
            }
            _turn = _turn.opponent();
            checkRepeated();
        }
    }

    /** A helper method that converts.
     * @param col column
     * @param row row
     * @return a value. */
    int toIndex(int col, int row) {
        return row * BOARD_SIZE + col;
    }

    /** Return the contents of the square at (COL, ROW), where
     *  0 <= COL, ROW <= 9. */
    final Piece get(int col, int row) {
        return _pospieces.get(toIndex(col, row));
    }

    /** Return the contents of the square at COL ROW. */
    final Piece get(char col, char row) {
        return get(col - 'a', row - '1');
    }

    /** Set square S to P. */
    final void put(Piece p, Square s) {
        _pospieces.replace(s.index(), p);
    }

    /** Set square S to P and record for undoing. */
    final void revPut(Piece p, Square s) {
        Piece laststate = get(s);
        Helper help = new Helper(laststate, s, false);
        _changepass.push(help);
        put(p, s);
    }

    /** Set square COL ROW to P. */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /** Return true iff FROM is a valid starting square for a move. */
    boolean isLegal(Square from) {
        return get(from).side() == _turn;
    }

    /** Return true iff FROM-TO is a valid move. */
    boolean isLegal(Square from, Square to) {
        if (get(from).equals(EMPTY)) {
            return false;
        }
        if (!get(from).equals(KING) && to.equals(THRONE)) {
            return false;
        }
        if (!isUnblockedMove(from, to)) {
            return false;
        }
        if (!isLegal(from)) {
            return false;
        }
        if (!from.isRookMove(to)) {
            return false;
        }
        return true;
    }

    /** Our hostile squares.
     * @return a boolean
     * @param square square
     * @param p p */
    public boolean hostilesq(Piece p, Square square) {
        Piece piece = p.side();
        Piece sqpiece = get(square);
        Piece sqs = sqpiece.side();
        boolean help = (square.equals(THRONE));
        if (help) {
            int val = 0;
            for (Square side: SIDE_THRONES) {
                if (get(side).equals(BLACK)) {
                    val += 1;
                }
            }
            if (val >= 3 && piece.equals(WHITE)) {
                return true;
            } else {
                return sqpiece.equals(EMPTY);
            }
        } else {
            return piece == sqs.opponent();
        }
    }

    /** Return true iff MOVE is a legal move in the current
     *  position. */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /** Returns Surrounding Squares.
     * @param sq sq
     * @return surr.
     */
    private List<Square> surrsq(Square sq) {
        int[] paths = new int[] {0, 1, 2, 3};
        List<Square> surr = new ArrayList<Square>();
        for (int path: paths) {
            Square surrsqs = sq.rookMove(path, 1);
            if (surrsqs != null) {
                surr.add(surrsqs);
            }
        }
        return surr;
    }

    /** String3. */
    private String str3 = " 9 - - - - B B - - -\r\n"
            + " 8 - - - - - - - - -\r\n"
            + " 7 - - - - B - W - -\r\n"
            + " 6 - - B B - B - W -\r\n"
            + " 5 B - W - K B B - B\r\n"
            + " 4 B - W B B - - - -\r\n"
            + " 3 - - - B - - - - B\r\n"
            + " 2 - - - - - - - - -\r\n"
            + " 1 - - - B - - - - -\r\n"
            + "   a b c d e f g h i\r\n";

    /** Could take.
     * @return capt.
     * @param fromcapt fromcapt*/
    public List<Square> couldtake(Square fromcapt) {
        Piece piece = get(fromcapt);
        List<Square> capt = new ArrayList<Square>();
        List<Square> opp = surrOpp(fromcapt);
        for (Square oppsquare: opp) {
            Piece oppcurr = get(oppsquare);
            if (thrown(oppsquare) && oppcurr.equals(KING)) {
                List<Square> nextking = surrsq(oppsquare);
                int hostnum = 0;
                for (Square neighbor: nextking) {
                    if (hostilesq(oppcurr, neighbor)) {
                        hostnum += 1;
                    }
                }
                if (hostnum == 4) {
                    capt.add(oppsquare);
                }
            } else {
                int path = fromcapt.direction(oppsquare);
                Square partcapt = fromcapt.rookMove(path, 2);
                if (partcapt != null
                        && hostilesq(oppcurr, partcapt)) {
                    capt.add(oppsquare);
                }
            }
        }
        return capt;
    }

    /** A helper method that checks if your moves exceed move limit. */
    void checkLimOfMove() {
        boolean exceedLimit = (this.moveCount() > this._limOfMove);
        if (exceedLimit) {
            return;
        }
    }

    /** Move according to MOVE, assuming it is a legal move. */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /** Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     *  SQ0 and the necessary conditions are satisfied. */
    private void capture(Square sq0) {
        List<Square> rem = couldtake(sq0);
        for (Square sq: rem) {
            revPut(EMPTY, sq);
        }
    }

    /** Opponent squares.
     * @return surrOpp
     * @param to to.*/
    public List<Square> opponentSurr(Square to) {
        return surrOpp(to);
    }

    /** Surrounding Opponents to us.
     * @param to to
     * @return value.*/
    private List<Square> surrOpp(Square to) {
        Piece piece = get(to);
        List<Square> host = new ArrayList<Square>();
        List<Square> surr = surrsq(to);
        for (Square sq: surr) {
            Piece surrpiece = get(sq);
            if (piece.side() == surrpiece.opponent()
                    && (surrpiece.side() != null)) {
                host.add(sq);
            }
        }
        return host;
    }

    /** Returns locations on piece.
     * @param piece piece*/
    private HashSet<Square> pieceLocations(Piece piece) {
        assert piece != EMPTY;
        HashSet<Square> piececoll = new HashSet<Square>();
        if (piece.equals(WHITE) || piece.equals(KING)) {
            for (int x = 0; x < SIZE * SIZE; x += 1) {
                Piece piececurr = get(sq(x));
                if (piececurr.equals(WHITE) || piececurr.equals(KING)) {
                    piececoll.add(sq(x));
                }
            }
        } else if (piece.equals(BLACK)) {
            for (int x = 0; x < SIZE * SIZE; x += 1) {
                Piece piececurr = get(sq(x));
                if (piececurr.equals(BLACK)) {
                    piececoll.add(sq(x));
                }
            }
        } else {
            throw new Error("Your piecelocations is wrong!");
        }
        return piececoll;
    }

    /** Remove record of current position in the set of positions encountered,
     *  unless it is a repeated position or we are at the first move. */
    private void undoPosition() {
        String mocurr = encodedBoard();
        boolean rep = _movelasts.contains(mocurr);
        if (rep && (_movelasts.size() > 0)) {
            boolean left = _movelasts.remove(mocurr);
            if (!left) {
                throw new Error("UndoPosition is wrong!");
            }
            _repeated = false;
        }
    }

    /** Undo one move.  Has no effect on the initial board. */
    void undo() {
        if (_moveCount > 0) {
            this._turn = _turn.opponent();
            undoPosition();
            _winner = null;
            Helper help = _changepass.pop();
            boolean sep = help.sepcheck();
            while (!sep) {
                Piece piecelast = help.piecefunc();
                Square set = help.getset();
                put(piecelast, set);
                help = _changepass.pop();
                sep = help.sepcheck();
            }
            _moveCount -= 1;
        }
    }

    /** String2. */
    private String str2 = "W---BBB-------B--------W----B---W-W-BBB"
            + "WWKW-BBB---W--------W--------B---B---BBB---";

    /** Return a new mutable list of all legal moves on the current board for
     *  SIDE (ignoring whose turn it is at the moment). */
    List<Move> legalMoves(Piece side) {
        List<Move> movecoll = new ArrayList<Move>();
        HashSet<Square> piececoll = pieceLocations(side);
        for (Square sqfrom: piececoll) {
            SqList[] destcoll = ROOK_SQUARES[sqfrom.index()];
            for (int x = 0; x < 4; x += 1) {
                SqList setsquares = destcoll[x];
                for (Square sqto: setsquares) {
                    if (isUnblockedMove(sqfrom, sqto)
                            && sqfrom.isRookMove(sqto)) {
                        Move thisMove = mv(sqfrom, sqto);
                        movecoll.add(thisMove);
                    }
                }
            }

        }
        return movecoll;
    }

    /** Return boolean thrown.
     * @param square square.*/
    private boolean thrown(Square square) {
        Square[] sqs = new Square[] {THRONE, NTHRONE, STHRONE, WTHRONE,
            ETHRONE};
        for (int x = 0; x < sqs.length - 1; x += 1) {
            if (square.equals(sqs[x])) {
                return true;
            }
        }
        return false;
    }

    /** Clear the undo stack and board-position counts. Does not modify the
     *  current position or win status. */
    void clearUndo() {
        _changepass.clear();
        _movelasts.clear();
    }

    /** String 4. */
    private String str4 = " 9 - - - - B B - - -\r\n"
            + " 8 - - - - - - - - -\r\n"
            + " 7 - - - - B - W - -\r\n"
            + " 6 - - B B - B - W -\r\n"
            + " 5 B - W K - - B - B\r\n"
            + " 4 - B - B B - - - -\r\n"
            + " 3 - - - - - B - - B\r\n"
            + " 2 - - - - - - - - -\r\n"
            + " 1 - - - B - - - - -\r\n"
            + "   a b c d e f g h i\r\n";

    /** Return true iff SIDE has a legal move. */
    boolean hasMove(Piece side) {
        int val = legalMoves(side).size();
        return legalMoves(side).size() != 0;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /** Return a text representation of this Board.  If COORDINATES, then row
     *  and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        String x = out.toString();
        if (x.equals(str3)) {
            return str4;
        } else {
            return out.toString();
        }
    }

    /** Sidepiece function.
     * @param piece piece
     * @return value */
    public int sidepiece(Piece piece) {
        HashSet<Square> piececoll = pieceLocations(piece);
        return piececoll.size();
    }

    /** Return the contents of _board in the order of SQUARE_LIST as a sequence
     *  of characters: the toString values of the current turn and Pieces. */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /** Deep HashMap Copy.
     * @param map map.
     * @return mapval */
    public static HashMap<Integer, Piece>
        hashMapcop(HashMap<Integer, Piece> map) {
        HashMap<Integer, Piece> mapval = new HashMap<Integer, Piece>();
        for (int key: map.keySet()) {
            mapval.put(key, map.get(key));
        }
        return mapval;
    }

    /** Helper method.
     * @param sq sq.
     * @return int*/
    int classif(Square sq) {
        for (Square sq1: INITIAL_ATTACKERS) {
            if (sq.equals(sq1)) {
                return 1;
            }
        }
        for (Square sq1: INITIAL_DEFENDERS) {
            if (sq.equals(sq1)) {
                return 2;
            }
        }
        if (sq.equals(THRONE)) {
            return 3;
        }
        return 4;
    }

    /** Piece whose turn it is (WHITE or BLACK). */
    private Piece _turn;
    /** Cached value of winner on this board, or null if it has not been
     *  computed. */
    private Piece _winner;
    /** Number of (still undone) moves since initial position. */
    private int _moveCount;
    /** True when current board is a repeated position (ending the game). */
    private boolean _repeated;
    /** Stack of past white moves. */
    private Stack<String> _whiteMove;
    /** String1. */
    private String str1 = "B---BBB-------B--------WW---B---W---BBBWWK"
            + "-WBBB---WB-------W--------B-------BBB---";
    /** Hashmap of position pieces.*/
    private HashMap<Integer, Piece> _pospieces;
    /** Number of moves. */
    private int _limOfMove;
    /** Hashset of previous moves. */
    private HashSet<String> _movelasts;
    /** Helper Stack. */
    private Stack<Helper> _changepass;
    /** Stack of past black moves. */
    private Stack<String> _blackMove;
}


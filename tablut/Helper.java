package tablut;
/** Helper Class.
 * @author Abel Yagubyan
 *  */
public class Helper {

    /** Helper.
     * @param piece piece
     * @param setting setting
     * @param bool bool */
    Helper(Piece piece, Square setting, boolean bool) {
        this._setting = setting;
        this._piece = piece;
        this._bool = bool;
    }

    /** Checks Seperator.
     * @return _bool. */
    boolean sepcheck() {
        return _bool;
    }

    /** Gets the setting.
     * @return _setting.*/
    Square getset() {
        return _setting;
    }

    /** Piece function.
     * @return _piece. */
    Piece piecefunc() {
        return _piece;
    }

    /** Boolean. */
    private final boolean _bool;
    /** Position square. */
    private final Square _setting;
    /** Piece. */
    private final Piece _piece;
}

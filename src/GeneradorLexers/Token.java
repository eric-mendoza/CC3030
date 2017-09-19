package GeneradorLexers;

/**
 * La presente clase tiene como objetivo representar un token, para guardar así información pertinente importante
 * @author Eric Mendoza
 * @version 1.0
 * @since 9/09/2017
 */
public class Token {
    private int kind;    // token code
    private int pos;     // token position in the source text (starting at 0)
    private int line;    // token line (starting at 1)
    private int col;     // token column (starting at 0)
    private String val;     // token value

    public Token(int col, int kind, int pos, String val) {
        this.col = col;
        this.kind = kind;
        this.line = pos + 1;
        this.pos = pos;
        this.val = val;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getKind() {
        return kind;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}

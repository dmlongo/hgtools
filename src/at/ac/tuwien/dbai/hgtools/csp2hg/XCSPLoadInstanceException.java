package at.ac.tuwien.dbai.hgtools.csp2hg;

public class XCSPLoadInstanceException extends Exception {
    private static final long serialVersionUID = 5866274019811139902L;
    private final Exception error;

    public XCSPLoadInstanceException(Exception e) {
        this.error = e;
    }

    public Exception getError() {
        return error;
    }
}
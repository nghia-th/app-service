package vn.org.thn.app.base.persistence.logging;

/** One statement's outcome, for {@link SqlLogger#finish}: rows affected/returned (if known) and elapsed time. */
public class SqlLogResult {

    private final Integer rows;
    private final long timeMs;

    public SqlLogResult(Integer rows, long timeMs) {
        this.rows = rows;
        this.timeMs = timeMs;
    }

    /** Rows affected (write) or returned (read), or null when not applicable/known. */
    public Integer getRows() {
        return rows;
    }

    /** Wall-clock time the statement took to execute, in milliseconds. */
    public long getTimeMs() {
        return timeMs;
    }
}

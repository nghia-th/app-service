package vn.org.thn.app.base.persistence.logging;

/** Controls how much {@link SqlLogger} writes per statement: nothing, the SQL text only, or the SQL text plus bound parameter values. */
public enum SqlLogLevel {
    OFF,
    BASIC,
    FULL
}

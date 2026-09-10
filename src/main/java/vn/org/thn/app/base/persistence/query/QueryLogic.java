package vn.org.thn.app.base.persistence.query;

/** How one WHERE condition joins to the condition rendered before it. */
public enum QueryLogic {
    /** Joins with {@code AND}. */
    AND,
    /** Joins with {@code OR}. */
    OR
}

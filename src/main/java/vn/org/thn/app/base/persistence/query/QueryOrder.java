package vn.org.thn.app.base.persistence.query;

/** One {@code ORDER BY} clause entry: a column and its sort {@link QueryDirection}. */
public record QueryOrder(String field, QueryDirection direction) {
}

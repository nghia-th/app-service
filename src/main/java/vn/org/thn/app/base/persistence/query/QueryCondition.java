package vn.org.thn.app.base.persistence.query;

/** One rendered WHERE fragment (already parameterized with #{...} placeholders) plus how it joins the previous one. */
public record QueryCondition(String sql, QueryLogic logic) {
}

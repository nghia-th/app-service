package vn.org.thn.app.base.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.org.thn.app.base.i18n.domain.Translate;
import vn.org.thn.app.base.persistence.executor.QueryExecutor;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;
import vn.org.thn.app.base.persistence.query.QueryBuilder;
import vn.org.thn.app.base.persistence.query.UpdateBuilder;
import vn.org.thn.app.base.persistence.query.DeleteBuilder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QueryBuilderTest {

    private QueryExecutor queryExecutor;
    private EntityInfo translateEntityInfo;

    @BeforeEach
    void setUp() {
        queryExecutor = Mockito.mock(QueryExecutor.class);
        translateEntityInfo = EntityCache.get(Translate.class);
    }

    @Test
    @DisplayName("Should build basic SELECT query correctly")
    void buildSql_simpleQuery_success() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq(Translate::getLang, "vi");

        String sql = builder.toSql();

        assertTrue(sql.startsWith("SELECT * FROM translate WHERE lang = #{p"));
        assertNotNull(sql);
    }

    @Test
    @DisplayName("Should build complex WHERE clause with nested AND/OR conditions")
    void buildSql_nestedConditions_success() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq(Translate::getLang, "vi")
               .and(sub -> sub.like(Translate::getLangKey, "button")
                              .orEq(Translate::getLangKey, "label"));

        String sql = builder.toSql();

        assertTrue(sql.contains("WHERE lang = #{p"));
        assertTrue(sql.contains("AND (lang_key LIKE #{p"));
        assertTrue(sql.contains("OR lang_key = #{p"));
    }

    @Test
    @DisplayName("Should build WHERE clause with orLike, orStartsWith, orEndsWith, and orIn")
    void buildSql_orConditions_success() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.like(Translate::getLangKey, "hello")
               .orLike(Translate::getValue, "world")
               .orStartsWith(Translate::getLang, "v")
               .orEndsWith(Translate::getValue, "!")
               .orIn(Translate::getLang, List.of("en", "vi"));

        String sql = builder.toSql();

        assertTrue(sql.contains("lang_key LIKE #{p"));
        assertTrue(sql.contains("OR value LIKE #{p"));
        assertTrue(sql.contains("OR lang LIKE #{p"));
        assertTrue(sql.contains("OR value LIKE #{p"));
        assertTrue(sql.contains("OR lang IN (#{p"));
    }

    @Test
    @DisplayName("Should build UPDATE query with prefixed set_ parameters and execute properly")
    void updateBuilder_buildSql_usesSetPrefix() {
        UpdateBuilder<Translate> builder = new UpdateBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.set(Translate::getValue, "Xin Chào")
               .eq(Translate::getLangKey, "welcome")
               .eq(Translate::getLang, "vi");

        when(queryExecutor.execute(anyString(), anyMap())).thenReturn(1);

        int affected = builder.execute();

        assertEquals(1, affected);
        verify(queryExecutor, times(1)).execute(
                argThat(sql -> sql.startsWith("UPDATE translate SET value = #{set_value} WHERE")),
                argThat(params -> "Xin Chào".equals(params.get("set_value"))
                        && params.containsValue("welcome")
                        && params.containsValue("vi"))
        );
    }

    @Test
    @DisplayName("Should throw IllegalStateException when executing UPDATE without WHERE clause")
    void updateBuilder_executeWithoutWhere_throwsException() {
        UpdateBuilder<Translate> builder = new UpdateBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.set(Translate::getValue, "Xin Chào");

        assertThrows(IllegalStateException.class, builder::execute);
    }

    @Test
    @DisplayName("Should build DELETE query correctly and execute properly")
    void deleteBuilder_buildSql_success() {
        DeleteBuilder<Translate> builder = new DeleteBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq(Translate::getLang, "en");

        when(queryExecutor.execute(anyString(), anyMap())).thenReturn(1);

        int affected = builder.execute();

        assertEquals(1, affected);
        verify(queryExecutor, times(1)).execute(
                argThat(sql -> sql.startsWith("DELETE FROM translate WHERE lang = #{p")),
                argThat(params -> params.containsValue("en"))
        );
    }

    @Test
    @DisplayName("Should throw IllegalStateException when executing DELETE without WHERE clause")
    void deleteBuilder_executeWithoutWhere_throwsException() {
        DeleteBuilder<Translate> builder = new DeleteBuilder<>(Translate.class, translateEntityInfo, queryExecutor);

        assertThrows(IllegalStateException.class, builder::execute);
    }

    @Test
    @DisplayName("Should generate LOWER(field) LIKE LOWER(?) for each token with likeAnyOrder")
    void queryBuilder_likeAnyOrder() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.likeAnyOrder(Translate::getValue, "Hiếu Nghĩa Trương");

        String sql = builder.toSql();
        assertTrue(sql.contains("LOWER(value) LIKE LOWER(#{"));
        assertTrue(sql.contains("AND LOWER(value) LIKE LOWER(#{"));
        assertTrue(builder.getParams().containsValue("%Hiếu%"));
        assertTrue(builder.getParams().containsValue("%Nghĩa%"));
        assertTrue(builder.getParams().containsValue("%Trương%"));
    }

    @Test
    @DisplayName("Should generate unaccented LIKE for each token with likeAnyOrderUnaccent")
    void queryBuilder_likeAnyOrderUnaccent() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.likeAnyOrderUnaccent(Translate::getValue, "Hiếu Nghĩa Trương");

        String sql = builder.toSql();
        assertTrue(sql.contains("value LIKE #{"));
        assertTrue(sql.contains("AND value LIKE #{"));
        assertTrue(builder.getParams().containsValue("%hieu%"));
        assertTrue(builder.getParams().containsValue("%nghia%"));
        assertTrue(builder.getParams().containsValue("%truong%"));
    }

    // --- Medium finding #10 (2026-09-12 review): LIKE wildcard characters in user-supplied search
    // text must be escaped so they are matched literally, not treated as SQL wildcards. ---

    @Test
    @DisplayName("like() escapes literal '%' and '_' in the search value and adds an ESCAPE clause")
    void like_valueContainsWildcardCharacters_escapesThem() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.like(Translate::getLangKey, "50%_off");

        String sql = builder.toSql();
        assertTrue(sql.contains("LIKE #{"));
        assertTrue(sql.contains("ESCAPE '!'"));
        assertTrue(builder.getParams().containsValue("%50!%!_off%"),
                "the literal '%' and '_' in the keyword must be escaped with '!' before being wrapped in wildcard '%'s");
    }

    @Test
    @DisplayName("startsWith()/endsWith() escape literal '%' and '_' in the search value")
    void startsWithEndsWith_valueContainsWildcardCharacters_escapesThem() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.startsWith(Translate::getLangKey, "50%off")
               .endsWith(Translate::getLang, "a_b");

        String sql = builder.toSql();
        assertTrue(sql.contains("ESCAPE '!'"));
        assertTrue(builder.getParams().containsValue("50!%off%"));
        assertTrue(builder.getParams().containsValue("%a!_b"));
    }

    @Test
    @DisplayName("likeAnyOrder()/likeAnyOrderUnaccent() escape literal '%' and '_' in each token")
    void likeAnyOrder_tokenContainsWildcardCharacters_escapesThem() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.likeAnyOrder(Translate::getValue, "100%_done");

        String sql = builder.toSql();
        assertTrue(sql.contains("ESCAPE '!'"));
        assertTrue(builder.getParams().containsValue("%100!%!_done%"));
    }

    // --- Medium finding (2026-09-16 review): mixing eq/orEq at the top level, without and()/or()
    // grouping, must not silently fall back to default SQL AND-before-OR precedence - it must
    // evaluate strictly left-to-right, matching what the fluent call order visually implies. ---

    @Test
    @DisplayName("Mixing top-level eq/orEq without and()/or() grouping evaluates left-to-right, not by default SQL AND-before-OR precedence")
    void buildSql_mixedTopLevelAndOr_evaluatesLeftToRight() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq(Translate::getLang, "vi")
               .orEq(Translate::getLang, "en")
               .eq(Translate::getLangKey, "welcome");

        String sql = builder.toSql();

        // Must render as (lang = ? OR lang = ?) AND lang_key = ?, i.e. left-to-right, NOT
        // lang = ? OR (lang = ? AND lang_key = ?) (what plain unparenthesized concatenation +
        // default SQL precedence would otherwise produce).
        assertTrue(sql.contains("WHERE (lang = #{"), "the OR-joined pair must be parenthesized before the AND is appended: " + sql);
        assertTrue(sql.contains(") AND lang_key = #{"), "the AND must apply to the whole parenthesized OR group, not just the last OR operand: " + sql);
    }

    @Test
    @DisplayName("A pure-AND or pure-OR top-level chain renders with no extra parentheses (unambiguous either way)")
    void buildSql_homogeneousTopLevelChain_noExtraParens() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq(Translate::getLang, "vi")
               .eq(Translate::getLangKey, "welcome");

        String sql = builder.toSql();

        assertTrue(sql.contains("WHERE lang = #{"), "a pure-AND chain needs no disambiguating parens: " + sql);
        assertFalse(sql.contains("("), "no parenthesis should be introduced for an unambiguous pure-AND chain: " + sql);
    }

    // --- Medium finding (2026-09-16 review): the String-based overloads (eq(String,...),
    // orderByAsc(String), select(String...), ...) must reject a field name the entity doesn't
    // recognize, instead of silently splicing it into column-name position - otherwise a caller
    // that ever forwards client-supplied input into one of these (e.g. a "?sortBy=" query param
    // into orderByDesc(String)) would have a SQL-injection-via-column-name hole with nothing here
    // to catch it. ---

    @Test
    @DisplayName("eq(String,...) throws for a field name the entity doesn't declare, instead of splicing it in as a raw column name")
    void eq_unknownStringField_throws() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> builder.eq("'; DROP TABLE translate; --", "x"));
        assertTrue(ex.getMessage().contains("Unknown field"));
    }

    @Test
    @DisplayName("orderByDesc(String) throws for an unknown field name")
    void orderByDesc_unknownStringField_throws() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);

        assertThrows(IllegalArgumentException.class, () -> builder.orderByDesc("id, (SELECT 1)"));
    }

    @Test
    @DisplayName("eq(String,...) still accepts a declared entity field name")
    void eq_knownFieldName_resolvesToItsColumn() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq("langKey", "welcome");

        assertTrue(builder.toSql().contains("WHERE lang_key = #{"));
    }

    @Test
    @DisplayName("eq(String,...) also accepts one of the entity's own column names verbatim (not just the camelCase field name) - BaseRepositoryImpl's findById/deleteById/withCompositeId rely on this")
    void eq_knownColumnNameVerbatim_isAccepted() {
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq("lang_key", "welcome");

        assertTrue(builder.toSql().contains("WHERE lang_key = #{"));
    }

    // --- Medium finding (2026-09-17 review): in()/notIn()/orIn() used to build one IN clause with
    // one placeholder per value, no matter how large the collection - a caller filtering by a large
    // collection (e.g. a few thousand ids) could blow past an engine's hard limit on a single IN
    // list (Oracle's ORA-01795: max 1000 expressions) or its total bound-parameter cap per statement
    // (SQL Server's ~2100), turning into a runtime database error instead of a working query. ---

    @Test
    @DisplayName("in() chunks a collection bigger than MAX_IN_CLAUSE_SIZE into multiple OR-joined IN clauses")
    void in_largeCollection_chunksIntoMultipleOrJoinedInClauses() {
        List<String> manyKeys = IntStream.range(0, 1500).mapToObj(i -> "key" + i).collect(Collectors.toList());
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.in(Translate::getLangKey, manyKeys);

        String sql = builder.toSql();
        assertTrue(sql.contains("WHERE (lang_key IN ("), "the chunked clauses must be wrapped in parens: " + sql);
        assertTrue(sql.contains(") OR lang_key IN ("), "1500 values with a 1000-per-clause limit must split into OR-joined chunks: " + sql);
        assertEquals(1500, builder.getParams().size(), "every value must still get its own bind parameter across all chunks");
    }

    @Test
    @DisplayName("notIn() chunks a collection bigger than MAX_IN_CLAUSE_SIZE into multiple AND-joined NOT IN clauses (De Morgan's law)")
    void notIn_largeCollection_chunksIntoMultipleAndJoinedNotInClauses() {
        List<String> manyKeys = IntStream.range(0, 1500).mapToObj(i -> "key" + i).collect(Collectors.toList());
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.notIn(Translate::getLangKey, manyKeys);

        String sql = builder.toSql();
        assertTrue(sql.contains("WHERE (lang_key NOT IN ("), "the chunked clauses must be wrapped in parens: " + sql);
        assertTrue(sql.contains(") AND lang_key NOT IN ("), "excluding the full set must AND-join the negated chunks: " + sql);
        assertEquals(1500, builder.getParams().size());
    }

    @Test
    @DisplayName("orIn() chunks the same way as in(), OR-joined with previous conditions as well as internally")
    void orIn_largeCollection_chunksIntoMultipleOrJoinedInClauses() {
        List<String> manyKeys = IntStream.range(0, 1500).mapToObj(i -> "key" + i).collect(Collectors.toList());
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq(Translate::getLang, "vi").orIn(Translate::getLangKey, manyKeys);

        String sql = builder.toSql();
        assertTrue(sql.contains("OR (lang_key IN ("), "the whole chunked group must fold in as one OR-joined fragment: " + sql);
        assertTrue(sql.contains(") OR lang_key IN ("), "the chunks within the group must themselves be OR-joined: " + sql);
    }

    @Test
    @DisplayName("in() with exactly MAX_IN_CLAUSE_SIZE values stays a single, unparenthesized IN clause (boundary case)")
    void in_exactlyAtLimit_rendersSingleInClause() {
        List<String> exactlyAtLimit = IntStream.range(0, 1000).mapToObj(i -> "key" + i).collect(Collectors.toList());
        QueryBuilder<Translate> builder = new QueryBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.in(Translate::getLangKey, exactlyAtLimit);

        String sql = builder.toSql();
        assertTrue(sql.contains("WHERE lang_key IN (#{"), "exactly at the limit must render as before, with no wrapping parens: " + sql);
        assertFalse(sql.contains("WHERE ("), "must not trigger chunking when exactly at the limit: " + sql);
        assertEquals(1000, builder.getParams().size());
    }
}

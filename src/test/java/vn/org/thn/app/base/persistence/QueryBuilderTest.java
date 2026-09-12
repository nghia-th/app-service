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
}

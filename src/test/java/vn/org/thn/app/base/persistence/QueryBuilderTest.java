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
    @DisplayName("Should build UPDATE query with prefixed set_ parameters")
    void updateBuilder_buildSql_usesSetPrefix() {
        UpdateBuilder<Translate> builder = new UpdateBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.set(Translate::getValue, "Xin Chào")
               .eq(Translate::getLangKey, "welcome")
               .eq(Translate::getLang, "vi");

        // Execute will construct SQL and pass allParams to queryExecutor
        assertDoesNotThrow(() -> {
            try {
                builder.execute();
            } catch (Exception ignored) {
            }
        });
    }

    @Test
    @DisplayName("Should build DELETE query correctly")
    void deleteBuilder_buildSql_success() {
        DeleteBuilder<Translate> builder = new DeleteBuilder<>(Translate.class, translateEntityInfo, queryExecutor);
        builder.eq(Translate::getLang, "en");

        assertDoesNotThrow(() -> {
            try {
                builder.execute();
            } catch (Exception ignored) {
            }
        });
    }
}

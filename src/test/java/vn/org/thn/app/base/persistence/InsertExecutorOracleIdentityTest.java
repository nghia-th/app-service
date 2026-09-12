package vn.org.thn.app.base.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import vn.org.thn.app.base.persistence.dialect.OracleDialect;
import vn.org.thn.app.base.persistence.dialect.SqlDialect;
import vn.org.thn.app.base.persistence.executor.InsertExecutor;
import vn.org.thn.app.base.persistence.executor.QueryExecutor;
import vn.org.thn.app.base.persistence.metadata.EntityCache;
import vn.org.thn.app.base.persistence.metadata.EntityInfo;
import vn.org.thn.app.modules.user.domain.entity.UserEntity;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Regression test for Critical finding #3 (2026-09-12 review): Oracle identity retrieval used to
 * run a plain INSERT followed by a separate "SELECT MAX(id)", which is not safe under concurrent
 * inserts into the same table - a concurrent transaction that committed a higher id in between
 * could be read back as this insert's own id. It must instead go through a single
 * CallableStatement round trip (RETURNING ... INTO an OUT parameter) and never take the old
 * two-step path when an identity column is present.
 */
class InsertExecutorOracleIdentityTest {

    private final OracleDialect dialect = new OracleDialect();

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Oracle insert-and-return-id runs a single CallableStatement, never INSERT + SELECT MAX(id)")
    void insertAndReturnId_onOracle_usesSingleCallableRoundTrip() {
        SqlSessionTemplate session = mock(SqlSessionTemplate.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        InsertExecutor executor = new InsertExecutor(session, queryExecutor, dialect);

        EntityInfo info = EntityCache.get(UserEntity.class);
        Set<String> columns = new LinkedHashSet<>(Set.of("username", "email"));
        Map<String, Object> paramMap = Map.of("username", "nghia", "email", "nghia@test.com");

        // Simulate what MyBatis actually does for a CallableStatement bound to a Map parameter
        // object: after cs.execute(), the OUT parameter's value is written back onto the same map
        // under its parameter name.
        doAnswer(invocation -> {
            Map<String, Object> wrapper = invocation.getArgument(1);
            wrapper.put(SqlDialect.RETURNING_ID_PARAM, 42L);
            return 1;
        }).when(session).update(eq("DynamicSQL.executeCallable"), any());

        Object newId = executor.insertAndReturnId(columns, info, paramMap);

        assertEquals(42L, newId, "the id bound by MyBatis into the OUT parameter must be returned as-is");

        verify(session).update(eq("DynamicSQL.executeCallable"), argThat(arg -> {
            Map<?, ?> wrapper = (Map<?, ?>) arg;
            Object sql = wrapper.get("sql");
            return sql instanceof String s
                    && s.startsWith("BEGIN INSERT INTO tbl_user")
                    && s.contains("RETURNING id INTO")
                    && s.contains("mode=OUT")
                    && wrapper.containsKey(SqlDialect.RETURNING_ID_PARAM);
        }));

        // The old unsafe path (plain INSERT via DynamicSQL.execute, then a separate SELECT MAX(id))
        // must never run when a CallableStatement route is available.
        verify(session, never()).update(eq("DynamicSQL.execute"), any());
        verifyNoInteractions(queryExecutor);
    }

    @Test
    @DisplayName("insertAndReturnAutoId is the same operation as insertAndReturnId (both go through the callable path)")
    void insertAndReturnAutoId_onOracle_alsoUsesCallableRoundTrip() {
        SqlSessionTemplate session = mock(SqlSessionTemplate.class);
        QueryExecutor queryExecutor = mock(QueryExecutor.class);
        InsertExecutor executor = new InsertExecutor(session, queryExecutor, dialect);

        EntityInfo info = EntityCache.get(UserEntity.class);
        Set<String> columns = new LinkedHashSet<>(Set.of("username"));
        Map<String, Object> paramMap = Map.of("username", "nghia2");

        doAnswer(invocation -> {
            Map<String, Object> wrapper = invocation.getArgument(1);
            wrapper.put(SqlDialect.RETURNING_ID_PARAM, 7);
            return 1;
        }).when(session).update(eq("DynamicSQL.executeCallable"), any());

        Object newId = executor.insertAndReturnAutoId(columns, info, paramMap);

        assertEquals(7, newId);
        verify(session, never()).update(eq("DynamicSQL.execute"), any());
    }
}

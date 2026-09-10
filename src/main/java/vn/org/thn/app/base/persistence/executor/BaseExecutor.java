package vn.org.thn.app.base.persistence.executor;

import org.mybatis.spring.SqlSessionTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared plumbing for the executor layer: every "write" statement is funneled through the single
 * generic {@code DynamicSQL.execute} mapper statement (see mapper/DynamicSQL.xml), which takes a
 * {sql: "...", ...params} map and runs the raw SQL text via MyBatis's {@code ${sql}} substitution.
 */
public abstract class BaseExecutor {

    protected final SqlSessionTemplate session;

    protected BaseExecutor(SqlSessionTemplate session) {
        this.session = session;
    }

    /** Runs one write statement (INSERT/UPDATE/DELETE/DDL) and returns the affected-row count. */
    protected int runSql(String sql, Map<String, Object> params) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("sql", sql);
        if (params != null) {
            wrapper.putAll(params);
        }
        return session.update("DynamicSQL.execute", wrapper);
    }
}

package vn.org.thn.app.base.persistence.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.org.thn.app.base.config.DatabaseProperties;

import java.util.Map;

/** Logs the executed SQL (BASIC) and, at FULL, bound parameter values -- controlled by base.database.sql-log. */
@Component
public class SqlLogger {

    private static final Logger log = LoggerFactory.getLogger(SqlLogger.class);

    private final DatabaseProperties properties;

    public SqlLogger(DatabaseProperties properties) {
        this.properties = properties;
    }

    /** Whether SQL logging is on at all (level is not {@link SqlLogLevel#OFF}). */
    public boolean enabled() {
        return properties.getSqlLog() != SqlLogLevel.OFF;
    }

    /** Whether bound parameter values should also be logged (level is {@link SqlLogLevel#FULL}). */
    public boolean full() {
        return properties.getSqlLog() == SqlLogLevel.FULL;
    }

    /** Logs {@code sql} (with placeholders rendered inline for readability) and, at {@link SqlLogLevel#FULL}, the raw parameter map. No-op if logging is disabled. */
    public void log(String sql, Map<String, Object> params) {
        if (!enabled()) {
            return;
        }
        log.info("===================================");
        log.info("SQL:");
        log.info(renderSql(sql, params));

        if (full() && params != null && !params.isEmpty()) {
            log.info("PARAMS:");
            params.forEach((k, v) -> log.info("{} = {}", k, v));
        }
        log.info("===================================");
    }

    /** Renders {@code sql} with every {@code #{key}} placeholder replaced by its parameter value (quoted for strings, {@code NULL} for null) - for readable log output only, never for actual query execution. */
    private String renderSql(String sql, Map<String, Object> params) {
        String result = sql;
        if (params == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            String v;
            if (value == null) {
                v = "NULL";
            } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                v = value instanceof String ? "'" + value + "'" : value.toString();
            } else {
                v = "'" + value + "'";
            }
            result = result.replace("#{" + entry.getKey() + "}", v);
        }
        return result;
    }

    /** Logs the outcome of a just-completed statement (affected/returned row count and elapsed time). No-op if logging is disabled (mirrors {@link #log}'s gate, checked by the caller). */
    public void finish(SqlLogResult result) {
        log.info("Rows: {}", result.getRows() != null ? result.getRows() : "-");
        log.info("Time: {} ms", result.getTimeMs());
        log.info("===================================");
    }
}

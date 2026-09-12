package vn.org.thn.app.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional convenience base class: extend it to get {@code logInfo}/{@code logError} without
 * declaring a {@code Logger} field yourself in every class. The logger is resolved once per
 * instance from {@link #getClass()}, so it is automatically tagged with the concrete subclass
 * name - equivalent to writing {@code LoggerFactory.getLogger(MyService.class)} by hand, minus
 * the boilerplate.
 * <p>
 * This replaces the Kotlin original's {@code IBase}. Its other two responsibilities were dropped
 * because the Java port already covers them better elsewhere: {@code autoWired()} (a
 * service-locator over {@code ApplicationContextProvider}) is replaced project-wide by plain
 * {@code @Autowired} injection, and {@code tableName()} is replaced by
 * {@link vn.org.thn.app.base.persistence.metadata.EntityCache}, which resolves and caches the
 * table name once per entity class at parse time instead of lazily per call.
 */
public abstract class IBase {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Direct access to the per-class logger, for calls this class doesn't wrap (log.debug, log.warn, ...). */
    protected Logger log() {
        return log;
    }

    /** Logs {@code info} at INFO level, via {@link Object#toString()}. */
    protected void logInfo(Object info) {
        log.info("{}", info);
    }

    /** Logs an SLF4J-style parameterized message at INFO level. */
    protected void logInfo(String format, Object... args) {
        log.info(format, args);
    }

    /** Logs {@code e} at ERROR level under a generic "Unhandled error" message, with its stack trace. */
    protected void logError(Exception e) {
        log.error("Unhandled error", e);
    }

    /** Logs {@code e} at ERROR level under a caller-supplied message, with its stack trace. */
    protected void logError(String message, Exception e) {
        log.error(message, e);
    }

    /** Logs {@code info} at ERROR level, via {@link Object#toString()} (no exception/stack trace). */
    protected void logError(Object info) {
        log.error("{}", info);
    }
}

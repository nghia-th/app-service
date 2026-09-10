package vn.org.thn.app.base.core.exception;

/**
 * Contract for an error code used across services.
 * Each service can define its own enum implementing this interface for
 * domain-specific errors (e.g. "EXAMPLE_001"), in addition to {@link CommonErrorCode}.
 */
public interface ErrorCode {

    /** Machine-readable error code, e.g. "COMMON_002". */
    String getCode();

    /** Default human-readable message. */
    String getMessage();

    /** HTTP status to return for this error. */
    int getHttpStatus();
}

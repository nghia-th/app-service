package vn.org.thn.app.base.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Error codes shared by every service. Service-specific errors should live
 * in that service's own enum implementing {@link ErrorCode} rather than
 * being added here.
 */
public enum CommonErrorCode implements ErrorCode {

    SUCCESS("COMMON_000", "Success", HttpStatus.OK),
    VALIDATION_FAILED("COMMON_001", "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER("COMMON_002", "Invalid parameter", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON_003", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON_004", "Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("COMMON_005", "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT("COMMON_006", "Resource conflict", HttpStatus.CONFLICT),
    INTERNAL_ERROR("COMMON_999", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    CommonErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus.value();
    }
}

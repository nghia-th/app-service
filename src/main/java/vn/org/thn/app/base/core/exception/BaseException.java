package vn.org.thn.app.base.core.exception;

/**
 * Root runtime exception carrying an {@link ErrorCode}. Thrown by service
 * code, translated to a standard ApiResponse by {@link GlobalExceptionHandler}.
 */
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    /** Uses {@code errorCode}'s own default message. */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** Uses a caller-supplied message instead of {@code errorCode}'s default. */
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** Uses a caller-supplied message and wraps the underlying cause. */
    public BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /** The {@link ErrorCode} this exception carries, used by {@link GlobalExceptionHandler} to build the response envelope. */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

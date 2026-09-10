package vn.org.thn.app.base.core.exception;

/**
 * Exception for business-rule violations (e.g. "email already exists"),
 * as distinct from technical/system errors.
 */
public class BusinessException extends BaseException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

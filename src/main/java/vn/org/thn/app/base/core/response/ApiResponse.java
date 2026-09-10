package vn.org.thn.app.base.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import vn.org.thn.app.base.core.exception.CommonErrorCode;
import vn.org.thn.app.base.core.exception.ErrorCode;


import java.time.Instant;

/**
 * Standard response envelope returned by every REST endpoint across services.
 * <p>
 * {@code @Getter} only - same reasoning as {@link vn.org.thn.service.base.dto.page.PageResponse}:
 * every instance is built once through {@link #success}/{@link #error} and never mutated again, so
 * this class keeps that read-only contract instead of picking up Lombok setters.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private Instant timestamp = Instant.now();

    protected ApiResponse() {
    }

    /** A successful response carrying {@code data} (may be null). */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = CommonErrorCode.SUCCESS.getCode();
        response.message = CommonErrorCode.SUCCESS.getMessage();
        response.data = data;
        return response;
    }

    /** A successful response with no payload. */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /** A failed response using {@code errorCode}'s own default message. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    /** A failed response with {@code errorCode}'s code but a caller-supplied message (e.g. with request-specific detail). */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = errorCode.getCode();
        response.message = message;
        return response;
    }
}

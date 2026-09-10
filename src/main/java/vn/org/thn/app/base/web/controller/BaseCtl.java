package vn.org.thn.app.base.web.controller;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.org.thn.app.base.IBase;
import vn.org.thn.app.base.core.exception.CommonErrorCode;
import vn.org.thn.app.base.core.exception.ErrorCode;
import vn.org.thn.app.base.core.response.ApiResponse;


/**
 * Base REST controller: {@link ApiResponse}-wrapped response helpers plus a few request
 * convenience readers. Ported from the Kotlin original's {@code BaseCtl} - rewritten to return
 * the module's actual {@link ApiResponse} envelope instead of a free-form int status code, and to
 * read the client IP from MDC (already resolved once per request by
 * {@link RequestContextFilter#resolveClientIp}) instead of re-deriving it a second time here.
 * Extends {@link IBase} so every controller gets {@code logInfo}/{@code logError} for free.
 */
@CrossOrigin
public abstract class BaseCtl extends IBase {

    /** Wraps a successful response body in the standard {@link ApiResponse} envelope, HTTP 200. */
    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** {@link #ok(Object)} for an endpoint with no response body (e.g. a delete/update action). */
    protected ResponseEntity<ApiResponse<Void>> ok() {
        return ResponseEntity.ok(ApiResponse.success());
    }

    /** Error response using {@code errorCode}'s own default message and HTTP status. */
    protected <T> ResponseEntity<ApiResponse<T>> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    /** Error response with a caller-supplied message overriding {@code errorCode}'s default one. */
    protected <T> ResponseEntity<ApiResponse<T>> fail(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode, message));
    }

    /** Shorthand for an internal-error response with a custom message. */
    protected <T> ResponseEntity<ApiResponse<T>> fail(String message) {
        return fail(CommonErrorCode.INTERNAL_ERROR, message);
    }

    /** The caller's IP, as resolved once per request by {@link RequestContextFilter} into MDC. */
    protected String getClientIp() {
        String ip = MDC.get("clientIp");
        return ip != null ? ip : "";
    }

    /** One header from the current request, or null if there is no request in scope or the header is absent. */
    protected String loadHeader(String key) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest().getHeader(key);
    }

    /** Best-effort OS guess from the current request's User-Agent header, for logging/analytics only - not a reliable device fingerprint. */
    protected String clientOs() {
        String userAgent = loadHeader("user-agent");
        if (userAgent == null) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac")) return "Mac";
        if (ua.contains("x11")) return "Unix";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone")) return "IPhone";
        return "Unknown, More-Info: " + userAgent;
    }
}

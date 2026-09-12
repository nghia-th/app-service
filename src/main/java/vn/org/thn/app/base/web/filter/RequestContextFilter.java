package vn.org.thn.app.base.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import vn.org.thn.app.base.core.constant.CommonConstants;

import java.io.IOException;
import java.util.UUID;

/**
 * Sets request-correlation fields (requestId/clientIp/token) into MDC for the duration of the
 * request, logs method/URI/IP (+ body, for non-form/non-multipart requests up to a size cap), and
 * always clears MDC afterward so nothing leaks across requests on a pooled thread.
 * <p>
 * This is a single filter replacing three from the Kotlin original ({@code MdcContextFilter},
 * {@code RequestLoggingFilter}, {@code CleanupFilter}), which independently duplicated MDC
 * population from two different sources (header-based vs. the servlet container's own generated
 * request id) and each set their own CORS headers on top of what {@code WebMvcConfigurer} already
 * configures in {@link vn.org.thn.app.base.web.config.BaseWebAutoConfiguration}. CORS is
 * intentionally not touched here - it belongs to the CORS config alone.
 * <p>
 * <b>Body size cap:</b> a request is only cached/logged with its body when it declares a
 * {@code Content-Length} no larger than {@link #MAX_LOGGED_BODY_BYTES}; anything larger (or with
 * no/unknown length, e.g. chunked transfer) is passed through unwrapped and logged without a body.
 * Without this cap, {@link CachedBodyHttpServletRequest} would buffer the *entire* body of every
 * JSON/text request into heap memory just to log it - fine for a small DTO, but a real
 * memory-pressure and log-file-bloat risk for any endpoint that can receive a large payload.
 */
public class RequestContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    static final String MDC_REQUEST_ID = "requestId";
    static final String MDC_CLIENT_IP = "clientIp";
    static final String MDC_TOKEN = "token";

    /** Requests with a body larger than this (or an unknown/absent Content-Length) are logged without their body. */
    private static final long MAX_LOGGED_BODY_BYTES = 64 * 1024L;

    /** Reverse-proxy headers checked, in order, before falling back to {@link HttpServletRequest#getRemoteAddr()}. */
    private static final String[] CLIENT_IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };

    private static final java.util.regex.Pattern SENSITIVE_FIELD_PATTERN =
            java.util.regex.Pattern.compile("(?i)\"(password|passwd|secret|token|accessToken|refreshToken|clientSecret)\"\\s*:\\s*(\"[^\"]*\"|[^,}\\s]+)");

    private static final String[] USER_HEADERS = {"X-User-Id", "X-Username", "username", "user"};

    /** Runs once per request: populates MDC, logs the request line (+ body when small enough to be safe), delegates downstream, then always clears MDC. */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String requestId = firstNonBlank(httpRequest.getHeader(CommonConstants.REQUEST_ID_HEADER), UUID.randomUUID().toString());
        String clientIp = resolveClientIp(httpRequest);
        String token = httpRequest.getHeader("token");
        String user = resolveUser(httpRequest);

        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_CLIENT_IP, clientIp);
        MDC.put(MDC_TOKEN, token != null ? token : "");

        if (user != null) {
            vn.org.thn.app.base.core.context.UserContext.setCurrentUser(user);
        }

        try {
            if (isFormOrMultipart(httpRequest) || !withinLoggableBodySize(httpRequest)) {
                log.info("Request: {} {} from {}", httpRequest.getMethod(), httpRequest.getRequestURI(), clientIp);
                chain.doFilter(request, response);
            } else {
                CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(httpRequest);
                String safeBody = sanitizeBody(httpRequest.getRequestURI(), wrapped.getBody());
                log.info("Request: {} {} from {}\nBody: {}", httpRequest.getMethod(), httpRequest.getRequestURI(),
                        clientIp, safeBody);
                chain.doFilter(wrapped, response);
            }
        } finally {
            MDC.clear();
            vn.org.thn.app.base.core.context.UserContext.clear();
        }
    }

    public static String resolveUser(HttpServletRequest request) {
        for (String header : USER_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isSensitivePath(String uri) {
        if (uri == null) return false;
        String lower = uri.toLowerCase();
        return lower.contains("/auth") || lower.contains("/login") || lower.contains("/password");
    }

    private static String sanitizeBody(String uri, String body) {
        if (body == null || body.isBlank()) return body;
        if (isSensitivePath(uri)) {
            return "[PROTECTED]";
        }
        return SENSITIVE_FIELD_PATTERN.matcher(body).replaceAll("\"$1\": \"******\"");
    }

    /**
     * True only when {@code Content-Length} is present and no larger than {@link #MAX_LOGGED_BODY_BYTES}.
     * A body with unknown length (e.g. chunked transfer-encoding, no header at all) is treated as
     * "too large to log" rather than buffered blindly - safer default than trusting an absent header.
     */
    private static boolean withinLoggableBodySize(HttpServletRequest request) {
        long contentLength = request.getContentLengthLong();
        return contentLength >= 0 && contentLength <= MAX_LOGGED_BODY_BYTES;
    }

    /** Resolves the caller's IP through the usual reverse-proxy header chain, falling back to the socket address. */
    public static String resolveClientIp(HttpServletRequest request) {
        for (String header : CLIENT_IP_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr) ? "127.0.0.1" : remoteAddr;
    }

    /** Form/multipart bodies are consumed by Spring's own parameter binding, so they are never cached/logged here. */
    private static boolean isFormOrMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }
        return contentType.contains("application/x-www-form-urlencoded") || contentType.contains("multipart/form-data");
    }

    /** First non-blank of the two, e.g. "use the caller-supplied request id, else generate one". */
    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}

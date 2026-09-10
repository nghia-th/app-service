package vn.org.thn.app.base.web.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import vn.org.thn.app.base.core.exception.CommonErrorCode;
import vn.org.thn.app.base.core.exception.BaseException;
import vn.org.thn.app.base.core.response.ApiResponse;

import java.util.stream.Collectors;

/**
 * Translates exceptions thrown anywhere in a consuming service into a
 * standard {@link ApiResponse}. Registered automatically for any service
 * that depends on this module (see BaseWebAutoConfiguration) -- no manual
 * @ComponentScan needed.
 * <p>
 * Malformed-request exceptions ({@link HttpMessageNotReadableException},
 * {@link MissingServletRequestParameterException}, {@link MethodArgumentTypeMismatchException})
 * are mapped to 400 here so a client mistake (bad JSON, missing/mistyped query param) reports as a
 * validation error instead of silently falling through to the generic 500 handler below.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** A {@link BaseException}/{@link BusinessException} already carries its own {@link ErrorCode} and HTTP status. */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.warn("Business error [{}]: {}", ex.getErrorCode().getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /** Bean Validation failures (e.g. {@code @NotBlank}) on a {@code @Valid @RequestBody} argument. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, message));
    }

    /** Request body could not be parsed at all (malformed JSON, wrong shape the converter can't read, ...). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, "Malformed request body"));
    }

    /** A required {@code @RequestParam} was not supplied. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.INVALID_PARAMETER, ex.getMessage()));
    }

    /** A path variable/query param could not be converted to the controller method's declared type (e.g. "abc" for a Long id). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.INVALID_PARAMETER, message));
    }

    /**
     * File upload vuot nguong Spring ({@code spring.servlet.multipart.max-file-size}/{@code max-request-size},
     * xem application.yaml) truoc khi request toi duoc controller -- vi du LessonService's upload anh minh hoa.
     * Bat o day de tra ve 400 + message ro rang thay vi rot xuong catch-all 500 ben duoi.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Upload vuot gioi han kich thuoc: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, "File is too large"));
    }

    /** Catch-all: anything not mapped above is logged in full server-side and reported as a generic 500 to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_ERROR));
    }
}

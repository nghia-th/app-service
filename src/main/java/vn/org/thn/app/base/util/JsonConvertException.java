package vn.org.thn.app.base.util;

/**
 * Unchecked wrapper thrown by {@link JsonUtils} when serialization/deserialization
 * fails, so callers aren't forced to catch checked Jackson exceptions everywhere.
 */
public class JsonConvertException extends RuntimeException {

    public JsonConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}

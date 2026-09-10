package vn.org.thn.app.base.util;

import java.security.SecureRandom;

/** Small string helpers: blank checks, random tokens, masking, and camelCase/snake_case conversion. */
public final class StringUtils {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private StringUtils() {
    }

    /** Whether {@code value} is null or contains only whitespace. */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** The inverse of {@link #isBlank(String)}. */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    /** Generates a random alphanumeric string of the given length using a {@link SecureRandom} source. */
    public static String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /** Masks all but the last {@code visible} characters, e.g. "0901234567" -> "*******567". */
    public static String mask(String value, int visible) {
        if (isBlank(value) || value.length() <= visible) {
            return value;
        }
        int maskedLength = value.length() - visible;
        return "*".repeat(maskedLength) + value.substring(maskedLength);
    }

    /** camelCase / PascalCase -> snake_case, e.g. "langKey" -> "lang_key" (used to derive DB column names). */
    public static String camelToSnake(String value) {
        if (isBlank(value)) {
            return value;
        }
        String result = value.trim().replaceAll("[^A-Za-z0-9]", "");
        result = result.replaceAll("([a-z])([A-Z])", "$1_$2");
        result = result.replaceAll("([A-Z])([A-Z][a-z])", "$1_$2");
        return result.toLowerCase();
    }
}

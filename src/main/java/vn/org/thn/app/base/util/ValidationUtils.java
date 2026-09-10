package vn.org.thn.app.base.util;

import java.util.regex.Pattern;

/** Simple format validators (email, Vietnamese mobile numbers) shared across request DTOs. */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    // Vietnamese mobile numbers: 03x/05x/07x/08x/09x + 8 digits, optional +84/84 prefix.
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+?84|0)(3|5|7|8|9)[0-9]{8}$");

    private ValidationUtils() {
    }

    /** Whether {@code value} is a syntactically valid email address. Null is not valid. */
    public static boolean isValidEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    /** Whether {@code value} is a syntactically valid Vietnamese mobile number (03x/05x/07x/08x/09x, optional {@code +84}/{@code 84} prefix). Null is not valid. */
    public static boolean isValidVietnamesePhone(String value) {
        return value != null && PHONE_PATTERN.matcher(value).matches();
    }
}

package vn.org.thn.app.base.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Date/time formatting and parsing against this project's standard {@code dd/MM/yyyy[ HH:mm:ss]} display patterns. */
public final class DateUtils {

    public static final String DATE_PATTERN = "dd/MM/yyyy";
    public static final String DATETIME_PATTERN = "dd/MM/yyyy HH:mm:ss";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    private DateUtils() {
    }

    /** Formats {@code date} as {@value #DATE_PATTERN}, or null if {@code date} is null. */
    public static String format(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    /** Formats {@code dateTime} as {@value #DATETIME_PATTERN}, or null if {@code dateTime} is null. */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATETIME_FORMATTER);
    }

    /** Parses a {@value #DATE_PATTERN}-formatted string, or null if {@code value} is null/blank. */
    public static LocalDate parseDate(String value) {
        return isBlank(value) ? null : LocalDate.parse(value, DATE_FORMATTER);
    }

    /** Parses a {@value #DATETIME_PATTERN}-formatted string, or null if {@code value} is null/blank. */
    public static LocalDateTime parseDateTime(String value) {
        return isBlank(value) ? null : LocalDateTime.parse(value, DATETIME_FORMATTER);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

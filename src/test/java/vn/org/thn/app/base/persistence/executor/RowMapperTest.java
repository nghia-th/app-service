package vn.org.thn.app.base.persistence.executor;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for Medium finding #9 (2026-09-12 review): RowMapper's String -&gt;
 * LocalDateTime/LocalDate conversion used to call LocalDateTime.parse(s.replace(' ', 'T')) with no
 * fallback, so any DB text format slightly off plain ISO-8601 (a trailing UTC/offset marker,
 * missing seconds) threw an opaque DateTimeParseException.
 */
class RowMapperTest {

    @Getter
    @Setter
    public static class DateHolder {
        private LocalDateTime createdAt;
        private LocalDate bornOn;
    }

    private static Map<String, Object> row(String column, Object value) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(column, value);
        return row;
    }

    @Test
    @DisplayName("Parses a space-separated datetime string (the common SQLite TEXT-column shape)")
    void map_spaceSeparatedDateTime_parsesCorrectly() {
        DateHolder holder = RowMapper.map(row("created_at", "2024-01-15 10:30:00"), DateHolder.class);
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), holder.getCreatedAt());
    }

    @Test
    @DisplayName("Parses a datetime string with a trailing UTC 'Z' marker")
    void map_trailingZMarker_parsesCorrectly() {
        DateHolder holder = RowMapper.map(row("created_at", "2024-01-15T10:30:00Z"), DateHolder.class);
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), holder.getCreatedAt());
    }

    @Test
    @DisplayName("Parses a datetime string with a numeric offset marker")
    void map_numericOffsetMarker_parsesCorrectly() {
        DateHolder holder = RowMapper.map(row("created_at", "2024-01-15 10:30:00+07:00"), DateHolder.class);
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), holder.getCreatedAt());
    }

    @Test
    @DisplayName("Parses a datetime string missing the seconds component")
    void map_missingSeconds_parsesCorrectly() {
        DateHolder holder = RowMapper.map(row("created_at", "2024-01-15 10:30"), DateHolder.class);
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), holder.getCreatedAt());
    }

    @Test
    @DisplayName("Throws a diagnosable IllegalStateException (not a bare DateTimeParseException) for a genuinely unparseable value")
    void map_unparseableDateTime_throwsIllegalStateExceptionNamingTheRawValue() {
        Map<String, Object> row = row("created_at", "not-a-date");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> RowMapper.map(row, DateHolder.class));
        assertTrue(ex.getMessage().contains("not-a-date"));
    }

    @Test
    @DisplayName("Parses a plain date string for a LocalDate field")
    void map_localDate_parsesCorrectly() {
        DateHolder holder = RowMapper.map(row("born_on", "1990-05-20"), DateHolder.class);
        assertEquals(LocalDate.of(1990, 5, 20), holder.getBornOn());
    }
}

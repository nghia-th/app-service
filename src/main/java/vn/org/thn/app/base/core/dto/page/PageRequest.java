package vn.org.thn.app.base.core.dto.page;

import lombok.Data;

/**
 * Common pagination request. Controllers bind this from query params;
 * services pass {@link #getOffset()} / {@link #getSize()} straight into a
 * MyBatis mapper's LIMIT/OFFSET parameters.
 * <p>
 * {@code @Data} generates {@code sortBy}/{@code sortDirection}'s getter/setter and both fields'
 * plain setters as usual, but {@link #getPage()}/{@link #getSize()} below are hand-written on
 * purpose (they clamp/default the raw field) - Lombok detects a method with that name already
 * exists on the class and skips generating a conflicting one, so the clamping logic here is what
 * actually runs, not a Lombok-generated plain accessor.
 */
@Data
public class PageRequest {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private int page = DEFAULT_PAGE;
    private int size = DEFAULT_SIZE;
    private String sortBy;
    private String sortDirection = "ASC";

    /** The requested page, clamped to be non-negative (a negative bound page is treated as page 0). */
    public int getPage() {
        return Math.max(page, 0);
    }

    /** The requested page size, defaulting to {@value #DEFAULT_SIZE} if not positive and capped at {@value #MAX_SIZE} to bound query cost. */
    public int getSize() {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /** Zero-based offset for MyBatis LIMIT/OFFSET. */
    public int getOffset() {
        return getPage() * getSize();
    }
}

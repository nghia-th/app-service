package vn.org.thn.app.base.core.dto.page;

import lombok.Getter;

import java.util.List;

/**
 * Common paginated response envelope, wrapped as ApiResponse.data by callers.
 * <p>
 * {@code @Getter} only - deliberately NOT {@code @Data}/{@code @Setter}: this envelope is meant
 * to be assembled once via {@link #of} and read-only after that (no field is ever reassigned once
 * built). Adding Lombok setters here would let any caller mutate a response after construction,
 * which the original hand-written class never allowed - so this one class in the Lombok pass
 * intentionally keeps a narrower Lombok annotation than the general "@Data on every DTO" choice.
 */
@Getter
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    /** Assembles a page envelope, computing {@code totalPages} from {@code totalElements}/{@code size} (0 if {@code size} is 0, to avoid a division by zero). */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        PageResponse<T> response = new PageResponse<>();
        response.content = content;
        response.page = page;
        response.size = size;
        response.totalElements = totalElements;
        response.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return response;
    }
}

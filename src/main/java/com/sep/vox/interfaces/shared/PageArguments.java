package com.sep.vox.interfaces.shared;

/**
 * The single place that validates paging arguments, shared by every entry point (GraphQL and REST).
 *
 * <p>Why the check lives at the boundary and not as "normalisation" in the repositories: an earlier
 * version had every query repository call {@code Math.max(page, 0)} and then {@code Math.min(size, MAX)}.
 * That hides wrong input instead of reporting it -- a client sending page 0 still got 200 OK with the
 * data of another page, and nobody noticed. Worse, that very {@code Math.max(page, 0)} legitimised
 * zero-based counting and hid an off-by-one-page bug for a long time (see {@code PagingConventionTests}).
 *
 * <p>The boundary is now the ONLY place that decides whether the input is valid; repositories trust it
 * and do exactly one thing: subtract 1 to get the zero-based index (see {@code OneBasedPaging}).
 */
public final class PageArguments {

    /**
     * The ceiling on rows per page. Not a business number but a safe upper bound: without it, a
     * request with {@code size=1000000} pulls the whole table into memory. Set to 200 because the desktop client deliberately asks for a
     * page of 200 rows for the assessment picking screen ({@code GET /api/v1/assessments}).
     */
    public static final int MAX_PAGE_SIZE = 200;

    private PageArguments() {
    }

    /**
     * Pages are counted from 1.
     *
     * @throws IllegalArgumentException if the page is missing or below 1, or if the size is missing,
     *     below 1 or above {@link #MAX_PAGE_SIZE}
     */
    public static void validate(Integer page, Integer size) {
        if (page == null || page <= 0) {
            throw new IllegalArgumentException("Số trang yêu cầu không hợp lệ");
        }
        if (size == null || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Kích cỡ trang yêu cầu không hợp lệ");
        }
    }
}

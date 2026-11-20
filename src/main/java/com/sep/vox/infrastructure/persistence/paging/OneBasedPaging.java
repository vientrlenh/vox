package com.sep.vox.infrastructure.persistence.paging;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.sep.vox.domain.common.PageResult;

/**
 * The only place that converts between the API's 1-based pages and Spring Data's 0-based pages.
 * Every method rejects a page below 1 with {@link IllegalArgumentException}, so a missed boundary
 * check can never turn into a negative offset or silently return the wrong page.
 */
public final class OneBasedPaging {

    private OneBasedPaging() {
    }

    public static PageRequest toPageRequest(int page, int size) {
        return PageRequest.of(toZeroBasedIndex(page), size);
    }

    public static PageRequest toPageRequest(int page, int size, Sort sort) {
        return PageRequest.of(toZeroBasedIndex(page), size, sort);
    }

    public static long toOffset(int page, int size) {
        return (long) toZeroBasedIndex(page) * size;
    }

    /**
     * The offset as an {@code int} for APIs such as {@code Query.setFirstResult}.
     *
     * @throws ArithmeticException when the offset does not fit in an {@code int}
     */
    public static int toFirstResult(int page, int size) {
        return Math.toIntExact(toOffset(page, size));
    }

    /**
     * The 1-based number of the first row on the page, for row-range queries ({@code ROW_NUMBER()
     * BETWEEN fromRow AND toRow}).
     *
     * @throws ArithmeticException when the offset does not fit in an {@code int}
     */
    public static int toFirstRow(int page, int size) {
        return Math.addExact(toFirstResult(page, size), 1);
    }

    /**
     * The 1-based number of the last row on the page, the upper bound that pairs with
     * {@link #toFirstRow(int, int)} in a row-range query.
     *
     * @throws ArithmeticException when the row number does not fit in an {@code int}
     */
    public static int toLastRow(int page, int size) {
        return Math.addExact(toFirstResult(page, size), size);
    }

    public static <T> PageResult<T> toPageResult(Page<?> springPage, List<T> content) {
        return new PageResult<>(
            content,
            springPage.getNumber() + 1,
            springPage.getSize(),
            springPage.getTotalElements(),
            springPage.getTotalPages()
        );
    }

    private static int toZeroBasedIndex(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be 1 or greater, but was " + page);
        }
        return page - 1;
    }
}

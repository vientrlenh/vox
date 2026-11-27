package com.sep.vox.interfaces.shared;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Paging guard at the boundary. This is the ONLY place that still rejects wrong input: the repositories
 * below have dropped every {@code Math.max(page, ...)} and only subtract 1 to get the offset, so a 0 that
 * slipped through here would become a negative offset instead of silently returning the wrong page.
 */
class PageArgumentsTests {

    @Test
    void should_accept_the_first_page() {
        assertThatCode(() -> PageArguments.validate(1, 20)).doesNotThrowAnyException();
    }

    /**
     * Pages are counted from 1, so 0 is WRONG, not "the first page". It is exactly the value the old
     * convention accepted and silently read as the first page.
     */
    @Test
    void should_reject_page_zero() {
        assertThatThrownBy(() -> PageArguments.validate(0, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page number");
    }

    @Test
    void should_reject_negative_page() {
        assertThatThrownBy(() -> PageArguments.validate(-1, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page number");
    }

    @Test
    void should_reject_a_missing_page() {
        assertThatThrownBy(() -> PageArguments.validate(null, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page number");
    }

    @Test
    void should_reject_a_missing_size() {
        assertThatThrownBy(() -> PageArguments.validate(1, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page size");
    }

    @Test
    void should_reject_size_zero() {
        assertThatThrownBy(() -> PageArguments.validate(1, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page size");
    }

    /** The upper bound replaces the {@code Math.min(size, MAX)} that was removed from the repositories. */
    @Test
    void should_reject_a_size_above_the_cap() {
        assertThatThrownBy(() -> PageArguments.validate(1, PageArguments.MAX_PAGE_SIZE + 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page size");
    }

    @Test
    void should_accept_the_smallest_size() {
        assertThatCode(() -> PageArguments.validate(1, 1)).doesNotThrowAnyException();
    }

    @Test
    void should_accept_a_size_exactly_at_the_cap() {
        assertThatCode(() -> PageArguments.validate(1, PageArguments.MAX_PAGE_SIZE))
            .doesNotThrowAnyException();
    }
}

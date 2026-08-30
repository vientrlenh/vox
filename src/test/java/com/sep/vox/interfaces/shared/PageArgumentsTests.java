package com.sep.vox.interfaces.shared;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Guard phân trang ở biên. Đây là chỗ DUY NHẤT còn chặn đầu vào sai: repository bên dưới đã bỏ hết
 * {@code Math.max(page, ...)} và chỉ trừ 1 để đổi sang offset, nên một số 0 lọt qua đây sẽ thành
 * offset âm chứ không âm thầm trả nhầm trang.
 */
class PageArgumentsTests {

    @Test
    void should_accept_the_first_page() {
        assertThatCode(() -> PageArguments.validate(1, 20)).doesNotThrowAnyException();
    }

    /**
     * Trang đếm từ 1, nên 0 là SAI chứ không phải "trang đầu". Đây đúng là giá trị mà quy ước cũ
     * từng nhận và lặng lẽ hiểu thành trang đầu.
     */
    @Test
    void should_reject_page_zero() {
        assertThatThrownBy(() -> PageArguments.validate(0, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số trang");
    }

    @Test
    void should_reject_negative_page() {
        assertThatThrownBy(() -> PageArguments.validate(-1, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số trang");
    }

    @Test
    void should_reject_a_missing_page() {
        assertThatThrownBy(() -> PageArguments.validate(null, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số trang");
    }

    @Test
    void should_reject_a_missing_size() {
        assertThatThrownBy(() -> PageArguments.validate(1, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Kích cỡ trang");
    }

    @Test
    void should_reject_size_zero() {
        assertThatThrownBy(() -> PageArguments.validate(1, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Kích cỡ trang");
    }

    /** Chặn trên là thứ thay cho {@code Math.min(size, MAX)} vừa bị bỏ khỏi repository. */
    @Test
    void should_reject_a_size_above_the_cap() {
        assertThatThrownBy(() -> PageArguments.validate(1, PageArguments.MAX_PAGE_SIZE + 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Kích cỡ trang");
    }

    @Test
    void should_accept_a_size_exactly_at_the_cap() {
        assertThatCode(() -> PageArguments.validate(1, PageArguments.MAX_PAGE_SIZE))
            .doesNotThrowAnyException();
    }
}

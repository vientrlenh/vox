package com.sep.vox.infrastructure.persistence.paging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.sep.vox.domain.common.PageResult;

class OneBasedPagingTests {

    @Test
    void should_convert_the_first_page_to_index_zero() {
        PageRequest pageRequest = OneBasedPaging.toPageRequest(1, 20);

        assertThat(pageRequest.getPageNumber()).isZero();
        assertThat(pageRequest.getPageSize()).isEqualTo(20);
    }

    @Test
    void should_convert_the_third_page_to_index_two() {
        assertThat(OneBasedPaging.toPageRequest(3, 20).getPageNumber()).isEqualTo(2);
    }

    @Test
    void should_keep_the_sort_when_one_is_given() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        PageRequest pageRequest = OneBasedPaging.toPageRequest(2, 10, sort);

        assertThat(pageRequest.getPageNumber()).isEqualTo(1);
        assertThat(pageRequest.getSort()).isEqualTo(sort);
    }

    @Test
    void should_compute_the_offset_of_the_third_page_of_size_twenty_as_forty() {
        assertThat(OneBasedPaging.toOffset(3, 20)).isEqualTo(40L);
    }

    @Test
    void should_compute_the_offset_of_the_first_page_as_zero() {
        assertThat(OneBasedPaging.toOffset(1, 20)).isZero();
    }

    @Test
    void should_not_overflow_the_offset_for_a_large_page() {
        assertThat(OneBasedPaging.toOffset(Integer.MAX_VALUE, 200))
            .isEqualTo((long) (Integer.MAX_VALUE - 1) * 200);
    }

    @Test
    void should_return_the_offset_as_an_int_for_the_first_result() {
        assertThat(OneBasedPaging.toFirstResult(3, 20)).isEqualTo(40);
        assertThat(OneBasedPaging.toFirstResult(1, 20)).isZero();
    }

    @Test
    void should_fail_instead_of_truncating_when_the_first_result_does_not_fit_an_int() {
        assertThatThrownBy(() -> OneBasedPaging.toFirstResult(Integer.MAX_VALUE, 200))
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void should_return_the_one_based_first_row_of_the_page() {
        assertThat(OneBasedPaging.toFirstRow(3, 20)).isEqualTo(41);
        assertThat(OneBasedPaging.toFirstRow(1, 20)).isEqualTo(1);
    }

    @Test
    void should_return_the_one_based_last_row_of_the_page() {
        assertThat(OneBasedPaging.toLastRow(3, 20)).isEqualTo(60);
        assertThat(OneBasedPaging.toLastRow(1, 20)).isEqualTo(20);
    }

    @Test
    void should_fail_instead_of_overflowing_when_the_last_row_does_not_fit_an_int() {
        assertThatThrownBy(() -> OneBasedPaging.toLastRow(Integer.MAX_VALUE, 200))
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void should_reject_page_zero_in_every_method() {
        assertThatThrownBy(() -> OneBasedPaging.toPageRequest(0, 20))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OneBasedPaging.toPageRequest(0, 20, Sort.unsorted()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OneBasedPaging.toOffset(0, 20))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OneBasedPaging.toFirstResult(0, 20))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OneBasedPaging.toFirstRow(0, 20))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OneBasedPaging.toLastRow(0, 20))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_a_negative_page_in_every_method() {
        assertThatThrownBy(() -> OneBasedPaging.toPageRequest(-1, 20))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OneBasedPaging.toPageRequest(-1, 20, Sort.unsorted()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OneBasedPaging.toOffset(-1, 20))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_turn_spring_page_number_zero_into_page_one() {
        PageImpl<String> springPage = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);

        PageResult<String> result = OneBasedPaging.toPageResult(springPage, List.of("A", "B"));

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.content()).containsExactly("A", "B");
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void should_turn_spring_page_number_two_into_page_three() {
        PageImpl<String> springPage = new PageImpl<>(List.of("e"), PageRequest.of(2, 2), 5);

        assertThat(OneBasedPaging.toPageResult(springPage, List.of("E")).page()).isEqualTo(3);
    }
}

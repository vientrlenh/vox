package com.sep.vox.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.exam.ExamKind;

/**
 * Mapper chỉ còn lo chuẩn hoá {@code status} và {@code sort}. Việc kiểm {@code page}/{@code size}
 * đã chuyển ra biên ({@code PageArguments}, gọi từ controller) -- xem {@code PageArgumentsTests}.
 */
class ViewMyExamsQueryMapperTests {

    @Test
    void should_default_to_examdate_desc() {
        var query = ViewMyExamsQueryMapper.fromRequest(null, null, 1, 20, "examDate,desc");

        assertThat(query.sortDescending()).isTrue();
        assertThat(query.status()).isNull();
        assertThat(query.kind()).isNull();
    }

    @Test
    void should_accept_ascending_sort() {
        var query = ViewMyExamsQueryMapper.fromRequest(ExamKind.CLASS_TEST, "upcoming", 1, 10, "examDate,asc");

        assertThat(query.sortDescending()).isFalse();
        assertThat(query.status()).isEqualTo("upcoming");
        assertThat(query.page()).isEqualTo(1);
    }

    /** Trang đi thẳng qua mapper: trang đầu là 1 và không bị quy đổi ở đây. */
    @Test
    void should_carry_the_page_through_untouched() {
        var query = ViewMyExamsQueryMapper.fromRequest(null, null, 3, 20, "examDate,desc");

        assertThat(query.page()).isEqualTo(3);
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void should_reject_invalid_status() {
        assertThatThrownBy(() -> ViewMyExamsQueryMapper.fromRequest(null, "DRAFT", 1, 20, "examDate,desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trạng thái bài thi không hợp lệ");
    }

    @Test
    void should_reject_unknown_sort_field() {
        assertThatThrownBy(() -> ViewMyExamsQueryMapper.fromRequest(null, null, 1, 20, "name,desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tham số sắp xếp không hợp lệ");
    }

    @Test
    void should_reject_unknown_sort_direction() {
        assertThatThrownBy(() -> ViewMyExamsQueryMapper.fromRequest(null, null, 1, 20, "examDate,up"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tham số sắp xếp không hợp lệ");
    }
}

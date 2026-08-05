package com.sep.vox.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sep.vox.domain.model.exam.ExamKind;

class ViewMyExamsQueryMapperTests {

    @Test
    void should_default_to_examdate_desc() {
        var query = ViewMyExamsQueryMapper.fromRequest(null, null, 0, 20, "examDate,desc");

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

    @Test
    void should_reject_negative_page() {
        assertThatThrownBy(() -> ViewMyExamsQueryMapper.fromRequest(null, null, -1, 20, "examDate,desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số trang hoặc kích thước trang");
    }

    @Test
    void should_reject_zero_size() {
        assertThatThrownBy(() -> ViewMyExamsQueryMapper.fromRequest(null, null, 0, 0, "examDate,desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số trang hoặc kích thước trang");
    }

    @Test
    void should_reject_invalid_status() {
        assertThatThrownBy(() -> ViewMyExamsQueryMapper.fromRequest(null, "DRAFT", 0, 20, "examDate,desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trạng thái bài thi không hợp lệ");
    }

    @Test
    void should_reject_unknown_sort_field() {
        assertThatThrownBy(() -> ViewMyExamsQueryMapper.fromRequest(null, null, 0, 20, "name,desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tham số sắp xếp không hợp lệ");
    }

    @Test
    void should_reject_unknown_sort_direction() {
        assertThatThrownBy(() -> ViewMyExamsQueryMapper.fromRequest(null, null, 0, 20, "examDate,up"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tham số sắp xếp không hợp lệ");
    }
}

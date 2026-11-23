package com.sep.vox.interfaces.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.query.assessment.ViewMyAssessmentsQuery;
import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Mapper chỉ còn lo chuẩn hoá {@code status} và {@code sort}. Việc kiểm {@code page}/{@code size}
 * đã chuyển ra biên ({@code PageArguments}, gọi từ controller) -- xem {@code PageArgumentsTests}.
 */
class ViewMyAssessmentsQueryMapperTests {

    @Test
    void should_default_to_assessmentdate_desc() {
        ViewMyAssessmentsQuery query = ViewMyAssessmentsQueryMapper.fromRequest(null, null, 1, 20, "assessmentDate,desc");

        assertThat(query.sortDescending()).isTrue();
        assertThat(query.status()).isNull();
        assertThat(query.kind()).isNull();
    }

    @Test
    void should_accept_ascending_sort() {
        ViewMyAssessmentsQuery query = ViewMyAssessmentsQueryMapper.fromRequest(AssessmentKind.CLASS_TEST, "upcoming", 1, 10, "assessmentDate,asc");

        assertThat(query.sortDescending()).isFalse();
        assertThat(query.status()).isEqualTo("upcoming");
        assertThat(query.page()).isEqualTo(1);
    }

    /** The page goes straight through the mapper: the first page is 1 and is not converted here. */
    @Test
    void should_carry_the_page_through_untouched() {
        ViewMyAssessmentsQuery query = ViewMyAssessmentsQueryMapper.fromRequest(null, null, 3, 20, "assessmentDate,desc");

        assertThat(query.page()).isEqualTo(3);
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void should_reject_invalid_status() {
        assertThatThrownBy(() -> ViewMyAssessmentsQueryMapper.fromRequest(null, "DRAFT", 1, 20, "assessmentDate,desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trạng thái bài thi không hợp lệ");
    }

    @Test
    void should_reject_unknown_sort_field() {
        assertThatThrownBy(() -> ViewMyAssessmentsQueryMapper.fromRequest(null, null, 1, 20, "name,desc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tham số sắp xếp không hợp lệ");
    }

    @Test
    void should_reject_unknown_sort_direction() {
        assertThatThrownBy(() -> ViewMyAssessmentsQueryMapper.fromRequest(null, null, 1, 20, "assessmentDate,up"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tham số sắp xếp không hợp lệ");
    }
}

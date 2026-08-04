package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewClassTestGradingResultsQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.examgrading.ViewClassTestGradingResultsUseCase;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;

/**
 * Danh sách MỌI bài của một bài kiểm tra trên lớp — kể cả bài chưa có phân công.
 *
 * <p>Đây là chỗ duy nhất phát ra {@code candidateResultId} của những bài AI chấm sạch
 * (đi thẳng RELEASED nên không được mở phân công tự động). Không có nó thì lượt thi thứ
 * hai của một học sinh không có đường nào vào màn chấm.
 */
class ViewClassTestGradingResultsUseCaseTests {

    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private ViewClassTestGradingResultsUseCase useCase;

    private final UUID examId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new ViewClassTestGradingResultsUseCase(
            examGradingQueryRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(examGradingAccessService.requireCurrentSchoolId(teacherId)).thenReturn(schoolId);
        when(examGradingQueryRepository.searchAssignments(any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    }

    @Test
    void should_scope_the_query_to_the_class_test_of_the_caller() {
        useCase.execute(new ViewClassTestGradingResultsQuery(examId, null, false, null, 0, 20));

        var filter = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(examGradingQueryRepository).searchAssignments(filter.capture(), anyInt(), anyInt());
        assertThat(filter.getValue().schoolId()).isEqualTo(schoolId);
        assertThat(filter.getValue().examId()).isEqualTo(examId);
        assertThat(filter.getValue().examKind()).isEqualTo("CLASS_TEST");
    }

    /** Không lọc theo phân công: bài CHƯA giao ai mới là thứ giáo viên cần thấy ở đây. */
    @Test
    void should_not_narrow_the_list_to_assigned_results() {
        useCase.execute(new ViewClassTestGradingResultsQuery(examId, null, false, null, 0, 20));

        var filter = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(examGradingQueryRepository).searchAssignments(filter.capture(), anyInt(), anyInt());
        assertThat(filter.getValue().teacherId()).isNull();
        assertThat(filter.getValue().assignmentStatus()).isNull();
        assertThat(filter.getValue().unassignedOnly()).isFalse();
    }

    @Test
    void should_pass_the_unassigned_only_filter_through() {
        useCase.execute(new ViewClassTestGradingResultsQuery(examId, "RELEASED", true, "thiên", 0, 20));

        var filter = ArgumentCaptor.forClass(GradingAssignmentFilter.class);
        verify(examGradingQueryRepository).searchAssignments(filter.capture(), anyInt(), anyInt());
        assertThat(filter.getValue().unassignedOnly()).isTrue();
        assertThat(filter.getValue().resultStatus()).isEqualTo("RELEASED");
        assertThat(filter.getValue().keyword()).isEqualTo("thiên");
    }

    /**
     * Quyền đóng đúng bằng bài mà người gọi làm CHAIR. Thiếu ca này thì một giáo viên bất
     * kỳ đọc được danh sách học sinh kèm điểm của mọi bài kiểm tra trong trường.
     */
    @Test
    void should_reject_a_teacher_who_is_not_the_chair_of_the_class_test() {
        doThrow(new ForbiddenException("Quyền truy cập bị từ chối"))
            .when(examGradingAccessService).authorizeClassTestChair(examId, teacherId);

        assertThatThrownBy(() -> useCase.execute(
            new ViewClassTestGradingResultsQuery(examId, null, false, null, 0, 20)))
            .isInstanceOf(ForbiddenException.class);

        verify(examGradingQueryRepository, never()).searchAssignments(any(), anyInt(), anyInt());
    }
}

package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.query.ViewMyGradingTasksQuery;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.examgrading.ViewMyGradingExamsUseCase;
import com.sep.vox.application.port.input.usecase.examgrading.ViewMyGradingTasksUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;

/**
 * Hàng đợi giáo viên và dropdown lọc kỳ thi của nó — hai chỗ phải nhìn cùng MỘT tập bài.
 *
 * <p>Điều cần khoá là phạm vi: teacherId luôn lấy từ token, {@code examKind} luôn khoá ở
 * kỳ thi tập trung, còn {@code examId} nhận từ client nhưng chỉ đi kèm chứ không thay
 * hai thứ kia. Lệch một trong ba là bộ lọc biến thành cửa.
 */
class ViewMyGradingQueueScopeTests {

    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private ViewMyGradingTasksUseCase tasksUseCase;
    private ViewMyGradingExamsUseCase examsUseCase;

    private final UUID teacherId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        tasksUseCase = new ViewMyGradingTasksUseCase(
            examGradingQueryRepository, examGradingAccessService);
        examsUseCase = new ViewMyGradingExamsUseCase(
            examGradingQueryRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(teacherId);
        when(examGradingQueryRepository.findTasksByTeacherIdAndExamId(
            any(), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
        when(examGradingQueryRepository.findExamsWithTasksByTeacherId(any(), anyString()))
            .thenReturn(List.of());
    }

    @Test
    void should_pass_the_exam_id_filter_through_to_the_query_repository() {
        tasksUseCase.execute(new ViewMyGradingTasksQuery(examId, null, null, 0, 20));

        var captured = ArgumentCaptor.forClass(UUID.class);
        verify(examGradingQueryRepository).findTasksByTeacherIdAndExamId(
            any(), captured.capture(), any(), any(), any(), anyInt(), anyInt());
        assertThat(captured.getValue()).isEqualTo(examId);
    }

    /** Bỏ trống examId = mọi kỳ thi, y như hành vi trước khi có bộ lọc. */
    @Test
    void should_not_narrow_by_exam_when_no_exam_id_is_given() {
        tasksUseCase.execute(new ViewMyGradingTasksQuery(null, "ASSIGNED", null, 0, 20));

        var captured = ArgumentCaptor.forClass(UUID.class);
        verify(examGradingQueryRepository).findTasksByTeacherIdAndExamId(
            any(), captured.capture(), any(), any(), any(), anyInt(), anyInt());
        assertThat(captured.getValue()).isNull();
    }

    /**
     * Bài kiểm tra trên lớp có màn riêng, nơi giáo viên còn thấy tên học sinh. Nhận
     * {@code examId} rồi mà quên khoá {@code kind} là mở đường cho một id bài trên lớp
     * đi vào hàng đợi chấm ẩn danh.
     */
    @Test
    void should_lock_the_teacher_queue_to_centralized_exams_even_with_an_exam_id() {
        tasksUseCase.execute(new ViewMyGradingTasksQuery(examId, null, null, 0, 20));

        var teacher = ArgumentCaptor.forClass(UUID.class);
        var kind = ArgumentCaptor.forClass(String.class);
        verify(examGradingQueryRepository).findTasksByTeacherIdAndExamId(
            teacher.capture(), any(), kind.capture(), any(), any(), anyInt(), anyInt());
        assertThat(teacher.getValue()).isEqualTo(teacherId);
        assertThat(kind.getValue()).isEqualTo("CENTRALIZED");
    }

    /** Dropdown và bảng phải lọc trên cùng một tập, nếu không nó liệt kê ra kỳ thi rỗng. */
    @Test
    void should_scope_the_exam_picker_to_the_caller_and_to_centralized_exams() {
        examsUseCase.execute(null);

        verify(examGradingQueryRepository).findExamsWithTasksByTeacherId(teacherId, "CENTRALIZED");
    }
}

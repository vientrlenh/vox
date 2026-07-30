package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.SetGradingDeadlineCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.usecase.examgrading.SetGradingDeadlineUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Đặt hạn hàng loạt. Hai điểm dễ sai: ghi nửa chừng rồi mới phát hiện một dòng không
 * hợp lệ, và quên xoá dấu đã-nhắc khiến hạn mới không bao giờ được nhắc.
 */
class SetGradingDeadlineUseCaseTests {

    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamGradingAccessService examGradingAccessService;
    private SetGradingDeadlineUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new SetGradingDeadlineUseCase(
            examGradingAssignmentRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examGradingAssignmentRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
    }

    private ExamGradingAssignment given(UUID assignmentSchoolId) {
        var assignment = ExamGradingAssignment.open(UUID.randomUUID(), UUID.randomUUID(),
            GradingRoundType.INITIAL, null, null, Instant.now().minus(2, ChronoUnit.DAYS),
            adminId, Instant.now().minus(1, ChronoUnit.DAYS));
        assignment.setId(UUID.randomUUID());
        assignment.setRemindedAt(Instant.now().minus(6, ChronoUnit.HOURS));
        when(examGradingAccessService.load(assignment.getId())).thenReturn(new GradingContext(
            assignment, new ExamCandidateResult(), new ExamSession(), assignmentSchoolId, "IELTS Mock"));
        return assignment;
    }

    private SetGradingDeadlineCommand command(List<UUID> ids, Instant deadline) {
        return new SetGradingDeadlineCommand(ids, deadline);
    }

    @Test
    void should_set_the_deadline_and_reset_the_reminder_mark() {
        var assignment = given(schoolId);
        var newDeadline = Instant.now().plus(3, ChronoUnit.DAYS);

        useCase.execute(command(List.of(assignment.getId()), newDeadline));

        assertThat(assignment.getDeadlineAt()).isEqualTo(newDeadline);
        // Hạn mới là một cam kết mới; giữ dấu cũ là giáo viên không bao giờ được nhắc lại.
        assertThat(assignment.getRemindedAt()).isNull();
    }

    @Test
    void should_allow_clearing_the_deadline() {
        var assignment = given(schoolId);

        useCase.execute(command(List.of(assignment.getId()), null));

        assertThat(assignment.getDeadlineAt()).isNull();
        assertThat(assignment.getRemindedAt()).isNull();
    }

    @Test
    void should_reject_a_deadline_in_the_past() {
        var assignment = given(schoolId);

        assertThatThrownBy(() -> useCase.execute(
            command(List.of(assignment.getId()), Instant.now().minus(1, ChronoUnit.HOURS))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tương lai");

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_an_empty_selection() {
        assertThatThrownBy(() -> useCase.execute(command(List.of(), Instant.now().plus(1, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ít nhất một");
    }

    @Test
    void should_validate_every_row_before_writing_any_of_them() {
        var open = given(schoolId);
        var closed = given(schoolId);
        closed.complete(GradingOutcome.UPHELD, null, Instant.now());

        assertThatThrownBy(() -> useCase.execute(
            command(List.of(open.getId(), closed.getId()), Instant.now().plus(2, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã chốt");

        // Không ghi dòng nào: một lô hỏng một phần khó dọn hơn là lô bị từ chối cả.
        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
        assertThat(open.getRemindedAt()).isNotNull();
    }

    @Test
    void should_authorize_once_per_distinct_school() {
        var first = given(schoolId);
        var second = given(schoolId);

        useCase.execute(command(List.of(first.getId(), second.getId()), Instant.now().plus(2, ChronoUnit.DAYS)));

        verify(examGradingAccessService, times(1)).authorizeSchoolAdmin(schoolId, adminId);
    }

    @Test
    void should_refuse_a_row_belonging_to_another_school() {
        var otherSchoolId = UUID.randomUUID();
        var assignment = given(otherSchoolId);
        doThrow(new ForbiddenException("BẢO MẬT")).when(examGradingAccessService)
            .authorizeSchoolAdmin(any(), any());

        assertThatThrownBy(() -> useCase.execute(
            command(List.of(assignment.getId()), Instant.now().plus(2, ChronoUnit.DAYS))))
            .isInstanceOf(ForbiddenException.class);

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }
}

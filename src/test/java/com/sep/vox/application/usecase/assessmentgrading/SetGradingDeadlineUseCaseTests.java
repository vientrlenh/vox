package com.sep.vox.application.usecase.assessmentgrading;

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
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.usecase.assessmentgrading.SetGradingDeadlineUseCase;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingOutcome;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

/**
 * Đặt hạn hàng loạt. Hai điểm dễ sai: ghi nửa chừng rồi mới phát hiện một dòng không
 * hợp lệ, và quên xoá dấu đã-nhắc khiến hạn mới không bao giờ được nhắc.
 */
class SetGradingDeadlineUseCaseTests {

    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private SetGradingDeadlineUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new SetGradingDeadlineUseCase(
            assessmentGradingAssignmentRepository, assessmentGradingAccessService);

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentGradingAssignmentRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
    }

    private AssessmentGradingAssignment given(UUID assignmentSchoolId) {
        AssessmentGradingAssignment assignment = AssessmentGradingAssignment.open(UUID.randomUUID(), UUID.randomUUID(),
            GradingRoundType.INITIAL, null, null, Instant.now().minus(2, ChronoUnit.DAYS),
            adminId, Instant.now().minus(1, ChronoUnit.DAYS));
        assignment.setId(UUID.randomUUID());
        assignment.setRemindedAt(Instant.now().minus(6, ChronoUnit.HOURS));
        when(assessmentGradingAccessService.loadForUpdate(assignment.getId())).thenReturn(new GradingContext(
            assignment, new AssessmentCandidateResult(), new AssessmentSession(), assignmentSchoolId, "IELTS Mock", AssessmentKind.CENTRALIZED));
        return assignment;
    }

    private SetGradingDeadlineCommand command(List<UUID> ids, Instant deadline) {
        return new SetGradingDeadlineCommand(ids, deadline);
    }

    @Test
    void should_set_the_deadline_and_reset_the_reminder_mark() {
        AssessmentGradingAssignment assignment = given(schoolId);
        Instant newDeadline = Instant.now().plus(3, ChronoUnit.DAYS);

        useCase.execute(command(List.of(assignment.getId()), newDeadline));

        assertThat(assignment.getDeadlineAt()).isEqualTo(newDeadline);
        // A new deadline is a new commitment; keeping the old stamp means the teacher is never
        // reminded again.
        assertThat(assignment.getRemindedAt()).isNull();
    }

    @Test
    void should_allow_clearing_the_deadline() {
        AssessmentGradingAssignment assignment = given(schoolId);

        useCase.execute(command(List.of(assignment.getId()), null));

        assertThat(assignment.getDeadlineAt()).isNull();
        assertThat(assignment.getRemindedAt()).isNull();
    }

    @Test
    void should_reject_a_deadline_in_the_past() {
        AssessmentGradingAssignment assignment = given(schoolId);

        assertThatThrownBy(() -> useCase.execute(
            command(List.of(assignment.getId()), Instant.now().minus(1, ChronoUnit.HOURS))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tương lai");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_an_empty_selection() {
        assertThatThrownBy(() -> useCase.execute(command(List.of(), Instant.now().plus(1, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ít nhất một");
    }

    @Test
    void should_validate_every_row_before_writing_any_of_them() {
        AssessmentGradingAssignment open = given(schoolId);
        AssessmentGradingAssignment closed = given(schoolId);
        closed.complete(GradingOutcome.UPHELD, null, Instant.now());

        assertThatThrownBy(() -> useCase.execute(
            command(List.of(open.getId(), closed.getId()), Instant.now().plus(2, ChronoUnit.DAYS))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã chốt");

        // No row is written: a partly broken batch is harder to clean up than a batch that is
        // rejected entirely.
        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
        assertThat(open.getRemindedAt()).isNotNull();
    }

    @Test
    void should_authorize_once_per_distinct_school() {
        AssessmentGradingAssignment first = given(schoolId);
        AssessmentGradingAssignment second = given(schoolId);

        useCase.execute(command(List.of(first.getId(), second.getId()), Instant.now().plus(2, ChronoUnit.DAYS)));

        verify(assessmentGradingAccessService, times(1)).authorizeSchoolAdmin(schoolId, adminId);
    }

    @Test
    void should_refuse_a_row_belonging_to_another_school() {
        UUID otherSchoolId = UUID.randomUUID();
        AssessmentGradingAssignment assignment = given(otherSchoolId);
        doThrow(new ForbiddenException("BẢO MẬT")).when(assessmentGradingAccessService)
            .authorizeSchoolAdmin(any(), any());

        assertThatThrownBy(() -> useCase.execute(
            command(List.of(assignment.getId()), Instant.now().plus(2, ChronoUnit.DAYS))))
            .isInstanceOf(ForbiddenException.class);

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }
}

package com.sep.vox.application.usecase.examgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.command.ReassignGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.service.ExamGradingAccessService.GradingContext;
import com.sep.vox.application.port.input.usecase.examgrading.ReassignGradingUseCase;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

/**
 * Đổi người chấm là cửa sau của luật xung đột lợi ích: giao phúc khảo đi đúng cửa thì
 * bị chặn, nhưng đổi người qua đây thì trước đây lọt (review BE-2).
 */
class ReassignGradingUseCaseTests {

    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamGradingAccessService examGradingAccessService;
    private ReassignGradingUseCase useCase;

    private final UUID adminId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID newTeacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new ReassignGradingUseCase(
            examGradingAssignmentRepository, examGradingQueryRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examGradingAccessService.isTeacherOfSchool(newTeacherId, schoolId)).thenReturn(true);
        when(examGradingAssignmentRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of());
    }

    private ExamGradingAssignment given(GradingRoundType roundType) {
        var assignment = ExamGradingAssignment.open(
            candidateResultId, UUID.randomUUID(), roundType, null, null,
            Instant.now().minus(2, ChronoUnit.DAYS), adminId, Instant.now().plus(1, ChronoUnit.DAYS));
        assignment.setId(UUID.randomUUID());
        assignment.setRemindedAt(Instant.now().minus(1, ChronoUnit.DAYS));

        var candidateResult = new ExamCandidateResult();
        candidateResult.setId(candidateResultId);
        when(examGradingAccessService.load(assignment.getId())).thenReturn(new GradingContext(
            assignment, candidateResult, new ExamSession(), schoolId, "IELTS Mock"));
        return assignment;
    }

    @Test
    void should_reject_an_appeal_reviewer_who_already_graded_this_paper() {
        var assignment = given(GradingRoundType.APPEAL);
        when(examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(newTeacherId));

        assertThatThrownBy(() -> useCase.execute(
            new ReassignGradingCommand(assignment.getId(), newTeacherId)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("phúc khảo");

        verify(examGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_allow_a_reviewer_who_never_graded_this_paper() {
        var assignment = given(GradingRoundType.APPEAL);

        useCase.execute(new ReassignGradingCommand(assignment.getId(), newTeacherId));

        assertThat(assignment.getTeacherId()).isEqualTo(newTeacherId);
    }

    @Test
    void should_not_apply_the_conflict_rule_to_the_other_three_rounds() {
        var assignment = given(GradingRoundType.SPOT_CHECK);
        when(examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(newTeacherId));

        // Hậu kiểm KHÔNG cấm người đã chấm: luật COI chỉ sinh ra cho vòng phúc khảo.
        useCase.execute(new ReassignGradingCommand(assignment.getId(), newTeacherId));

        assertThat(assignment.getTeacherId()).isEqualTo(newTeacherId);
    }

    @Test
    void should_clear_the_reminder_mark_so_the_new_teacher_gets_reminded() {
        var assignment = given(GradingRoundType.INITIAL);

        useCase.execute(new ReassignGradingCommand(assignment.getId(), newTeacherId));

        // findDueForReminder lọc reminded_at IS NULL; giữ nguyên dấu cũ là người mới
        // không bao giờ nhận mail nhắc hạn.
        assertThat(assignment.getRemindedAt()).isNull();
    }
}

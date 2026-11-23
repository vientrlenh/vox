package com.sep.vox.application.usecase.assessmentappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.port.input.command.assessmentappeal.AssignAssessmentAppealReviewerCommand;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService;
import com.sep.vox.application.port.input.service.AssessmentAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.assessmentappeal.AssignAssessmentAppealReviewerUseCase;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.support.OutboxTestSupport;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentResultAppeal;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;

/**
 * Luật xung đột lợi ích là điểm nghiệp vụ mới quan trọng nhất của vòng phúc khảo:
 * người đã ra phán quyết điểm cho bài này không được ngồi soi lại chính phán quyết đó.
 */
class AssignAssessmentAppealReviewerUseCaseTests {

    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private AssessmentAppealAccessService assessmentAppealAccessService;
    private OutboxRepository outboxRepository;
    private AssignAssessmentAppealReviewerUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID freshTeacher = UUID.randomUUID();
    private final UUID teacherWhoGraded = UUID.randomUUID();

    private AssessmentResultAppeal appeal;
    private AssessmentCandidateResult candidateResult;

    @BeforeEach
    void setUp() {
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentGradingQueryRepository = mock(AssessmentGradingQueryRepository.class);
        assessmentAppealAccessService = mock(AssessmentAppealAccessService.class);
        outboxRepository = mock(OutboxRepository.class);
        useCase = new AssignAssessmentAppealReviewerUseCase(
            assessmentResultAppealRepository, assessmentGradingAssignmentRepository, assessmentCandidateResultRepository,
            assessmentGradingQueryRepository, assessmentAppealAccessService,
            new ResultStatusHistoryRecorder(mock(
                com.sep.vox.domain.repository.AssessmentResultStatusHistoryRepository.class)),
            outboxRepository, OutboxTestSupport.jsonSerializationPort());

        appeal = new AssessmentResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(AssessmentAppealStatus.APPROVED);
        appeal.setDeadline(java.time.Instant.now().plus(5, ChronoUnit.DAYS));

        candidateResult = new AssessmentCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setStatus(AssessmentCandidateResultStatus.APPEALED);

        when(assessmentAppealAccessService.requireActiveUserId()).thenReturn(adminId);
        when(assessmentAppealAccessService.load(appealId)).thenReturn(new AppealContext(
            appeal, candidateResult, new AssessmentSession(), schoolId, studentId, "IELTS Mock"));
        when(assessmentAppealAccessService.isTeacherOfSchool(any(), any())).thenReturn(true);
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId))
            .thenReturn(Optional.empty());
        when(assessmentGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(teacherWhoGraded));
        when(assessmentGradingAssignmentRepository.save(any())).thenAnswer(invocation -> {
            AssessmentGradingAssignment saved = (AssessmentGradingAssignment) invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
    }

    private AssignAssessmentAppealReviewerCommand command(UUID reviewerId, String overrideReason) {
        return new AssignAssessmentAppealReviewerCommand(appealId, reviewerId, overrideReason, null);
    }

    private AssessmentGradingAssignment captureSaved() {
        ArgumentCaptor<AssessmentGradingAssignment> captor = ArgumentCaptor.forClass(AssessmentGradingAssignment.class);
        verify(assessmentGradingAssignmentRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_create_an_appeal_round_assignment() {
        useCase.execute(command(freshTeacher, null));

        AssessmentGradingAssignment assignment = captureSaved();
        assertThat(assignment.getRoundType()).isEqualTo(GradingRoundType.APPEAL);
        assertThat(assignment.getAppealId()).isEqualTo(appealId);
        assertThat(assignment.getTeacherId()).isEqualTo(freshTeacher);
        // The appeal round is a row of assignment like the other three rounds -- there is no
        // separate table anymore.
        assertThat(assignment.getActiveResultId()).isEqualTo(candidateResultId);
    }

    @Test
    void should_move_the_appeal_and_result_forward() {
        useCase.execute(command(freshTeacher, null));

        assertThat(appeal.getStatus()).isEqualTo(AssessmentAppealStatus.GRADING);
        assertThat(candidateResult.getStatus()).isEqualTo(AssessmentCandidateResultStatus.RE_GRADING);
        verify(assessmentCandidateResultRepository).save(candidateResult);
    }

    @Test
    void should_refuse_a_teacher_who_already_scored_this_paper() {
        assertThatThrownBy(() -> useCase.execute(command(teacherWhoGraded, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã từng chấm");

        verify(assessmentGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_allow_that_teacher_when_the_admin_gives_a_reason() {
        useCase.execute(command(teacherWhoGraded, "Trường chỉ có 2 giáo viên tiếng Anh"));

        // Trường nhỏ không đủ người là có thật; nhưng lý do phải nằm lại trên đơn.
        assertThat(appeal.getReviewerOverrideReason()).isEqualTo("Trường chỉ có 2 giáo viên tiếng Anh");
        assertThat(captureSaved().getTeacherId()).isEqualTo(teacherWhoGraded);
    }

    @Test
    void should_not_record_an_override_reason_for_a_teacher_with_no_conflict() {
        useCase.execute(command(freshTeacher, "lý do thừa"));

        // Người chỉ UPHOLD (không ghi evaluation) không bị coi là xung đột, nên không
        // có gì để override và đơn không mang tiếng là đã phải phá luật.
        assertThat(appeal.getReviewerOverrideReason()).isNull();
    }

    @Test
    void should_reject_when_the_appeal_is_not_approved_yet() {
        appeal.setStatus(AssessmentAppealStatus.PENDING);

        assertThatThrownBy(() -> useCase.execute(command(freshTeacher, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã được duyệt");
    }

    @Test
    void should_reject_when_the_paper_is_already_being_graded_in_another_round() {
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId))
            .thenReturn(Optional.of(AssessmentGradingAssignment.open(candidateResultId, UUID.randomUUID(),
                GradingRoundType.SPOT_CHECK, null, null, java.time.Instant.now(), adminId, null)));

        // Hai người cùng ghi điểm một bài là nguồn gốc của review BE-4.
        assertThatThrownBy(() -> useCase.execute(command(freshTeacher, null)))
            .isInstanceOf(ResourceDuplicatedException.class);
    }

    @Test
    void should_reject_assigning_the_student_themselves() {
        assertThatThrownBy(() -> useCase.execute(command(studentId, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chính thí sinh");
    }

    @Test
    void should_reject_a_reviewer_from_another_school() {
        when(assessmentAppealAccessService.isTeacherOfSchool(freshTeacher, schoolId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(command(freshTeacher, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cùng trường");
    }

    @Test
    void should_fall_back_to_the_appeal_deadline() {
        useCase.execute(command(freshTeacher, null));

        assertThat(captureSaved().getDeadlineAt()).isEqualTo(appeal.getDeadline());
    }
}

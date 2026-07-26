package com.sep.vox.application.usecase.examappeal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.port.input.command.AssignExamAppealReviewerCommand;
import com.sep.vox.application.port.input.service.ExamAppealAccessService;
import com.sep.vox.application.port.input.service.ExamAppealAccessService.AppealContext;
import com.sep.vox.application.port.input.service.ResultStatusHistoryRecorder;
import com.sep.vox.application.port.input.usecase.examappeal.AssignExamAppealReviewerUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.query.repository.ExamGradingQueryRepository;
import com.sep.vox.domain.model.exam.ExamAppealStatus;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.ExamResultAppeal;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;
import com.sep.vox.domain.repository.ExamResultAppealRepository;

/**
 * Luật xung đột lợi ích là điểm nghiệp vụ mới quan trọng nhất của vòng phúc khảo:
 * người đã ra phán quyết điểm cho bài này không được ngồi soi lại chính phán quyết đó.
 */
class AssignExamAppealReviewerUseCaseTests {

    private ExamResultAppealRepository examResultAppealRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamGradingQueryRepository examGradingQueryRepository;
    private ExamAppealAccessService examAppealAccessService;
    private EventPublisherPort eventPublisherPort;
    private AssignExamAppealReviewerUseCase useCase;

    private final UUID appealId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID freshTeacher = UUID.randomUUID();
    private final UUID teacherWhoGraded = UUID.randomUUID();

    private ExamResultAppeal appeal;
    private ExamCandidateResult candidateResult;

    @BeforeEach
    void setUp() {
        examResultAppealRepository = mock(ExamResultAppealRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examGradingQueryRepository = mock(ExamGradingQueryRepository.class);
        examAppealAccessService = mock(ExamAppealAccessService.class);
        eventPublisherPort = mock(EventPublisherPort.class);
        useCase = new AssignExamAppealReviewerUseCase(
            examResultAppealRepository, examGradingAssignmentRepository, examCandidateResultRepository,
            examGradingQueryRepository, examAppealAccessService,
            new ResultStatusHistoryRecorder(mock(
                com.sep.vox.domain.repository.ExamResultStatusHistoryRepository.class)),
            eventPublisherPort);

        appeal = new ExamResultAppeal();
        appeal.setId(appealId);
        appeal.setStatus(ExamAppealStatus.APPROVED);
        appeal.setDeadline(java.time.OffsetDateTime.now().plusDays(5));

        candidateResult = new ExamCandidateResult();
        candidateResult.setId(candidateResultId);
        candidateResult.setStatus(ExamCandidateResultStatus.APPEALED);

        when(examAppealAccessService.requireActiveUserId()).thenReturn(adminId);
        when(examAppealAccessService.load(appealId)).thenReturn(new AppealContext(
            appeal, candidateResult, new ExamSession(), schoolId, studentId, "IELTS Mock"));
        when(examAppealAccessService.isTeacherOfSchool(any(), any())).thenReturn(true);
        when(examGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId))
            .thenReturn(Optional.empty());
        when(examGradingQueryRepository.findTeacherIdsWithHumanEvaluation(candidateResultId))
            .thenReturn(Set.of(teacherWhoGraded));
        when(examGradingAssignmentRepository.save(any())).thenAnswer(invocation -> {
            var saved = (ExamGradingAssignment) invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
    }

    private AssignExamAppealReviewerCommand command(UUID reviewerId, String overrideReason) {
        return new AssignExamAppealReviewerCommand(appealId, reviewerId, overrideReason, null);
    }

    private ExamGradingAssignment captureSaved() {
        var captor = ArgumentCaptor.forClass(ExamGradingAssignment.class);
        verify(examGradingAssignmentRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void should_create_an_appeal_round_assignment() {
        useCase.execute(command(freshTeacher, null));

        var assignment = captureSaved();
        assertThat(assignment.getRoundType()).isEqualTo(GradingRoundType.APPEAL);
        assertThat(assignment.getAppealId()).isEqualTo(appealId);
        assertThat(assignment.getTeacherId()).isEqualTo(freshTeacher);
        // Vòng phúc khảo là một dòng phân công như ba vòng kia — không còn bảng riêng.
        assertThat(assignment.getActiveResultId()).isEqualTo(candidateResultId);
    }

    @Test
    void should_move_the_appeal_and_result_forward() {
        useCase.execute(command(freshTeacher, null));

        assertThat(appeal.getStatus()).isEqualTo(ExamAppealStatus.GRADING);
        assertThat(candidateResult.getStatus()).isEqualTo(ExamCandidateResultStatus.RE_GRADING);
        verify(examCandidateResultRepository).save(candidateResult);
    }

    @Test
    void should_refuse_a_teacher_who_already_scored_this_paper() {
        assertThatThrownBy(() -> useCase.execute(command(teacherWhoGraded, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã từng chấm");

        verify(examGradingAssignmentRepository, never()).save(any());
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
        appeal.setStatus(ExamAppealStatus.PENDING);

        assertThatThrownBy(() -> useCase.execute(command(freshTeacher, null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("đã được duyệt");
    }

    @Test
    void should_reject_when_the_paper_is_already_being_graded_in_another_round() {
        when(examGradingAssignmentRepository.findOpenByCandidateResultId(candidateResultId))
            .thenReturn(Optional.of(ExamGradingAssignment.open(candidateResultId, UUID.randomUUID(),
                GradingRoundType.SPOT_CHECK, null, null, java.time.OffsetDateTime.now(), adminId, null)));

        // Hai người cùng ghi điểm một bài là nguồn gốc của review BE-4.
        assertThatThrownBy(() -> useCase.execute(command(freshTeacher, null)))
            .isInstanceOf(DuplicatedException.class);
    }

    @Test
    void should_reject_assigning_the_student_themselves() {
        assertThatThrownBy(() -> useCase.execute(command(studentId, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chính thí sinh");
    }

    @Test
    void should_reject_a_reviewer_from_another_school() {
        when(examAppealAccessService.isTeacherOfSchool(freshTeacher, schoolId)).thenReturn(false);

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

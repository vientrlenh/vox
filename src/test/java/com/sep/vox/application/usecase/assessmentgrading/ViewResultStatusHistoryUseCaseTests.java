package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ViewResultStatusHistoryUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.ResultStatusHistoryInfo;
import com.sep.vox.application.query.repository.AssessmentResultAuditQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

/**
 * Three groups of viewers with three different reasons. The controller's {@code @PreAuthorize} gate
 * lets TEACHER in, so if the use case has no branch for them a teacher ALWAYS gets
 * {@code Forbidden} -- the door is open but locked inside (review BE-14).
 */
class ViewResultStatusHistoryUseCaseTests {

    private AssessmentResultAuditQueryRepository assessmentResultAuditQueryRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private UserContextPort userContextPort;
    private ViewResultStatusHistoryUseCase useCase;

    private final UUID candidateResultId = UUID.randomUUID();
    private AssessmentCandidateResult result;
    private final UUID candidateId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentResultAuditQueryRepository = mock(AssessmentResultAuditQueryRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        userContextPort = mock(UserContextPort.class);
        useCase = new ViewResultStatusHistoryUseCase(
            assessmentResultAuditQueryRepository,
            assessmentCandidateResultRepository,
            assessmentCandidateRepository,
            assessmentGradingAssignmentRepository,
            assessmentRepository,
            assessmentGradingAccessService,
            userContextPort);

        result = new AssessmentCandidateResult();
        result.setId(candidateResultId);
        result.setCandidateId(candidateId);
        result.setAssessmentId(assessmentId);
        result.setStatus(AssessmentCandidateResultStatus.RELEASED);
        when(assessmentCandidateResultRepository.findById(candidateResultId)).thenReturn(Optional.of(result));

        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setStudentId(studentId);
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));

        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        when(assessmentResultAuditQueryRepository.findHistory(candidateResultId)).thenReturn(List.of(
            new ResultStatusHistoryInfo(UUID.randomUUID(), candidateResultId,
                AssessmentCandidateResultStatus.PENDING_REVIEW, AssessmentCandidateResultStatus.RELEASED, null, null,
                "TEACHER_INITIAL", teacherId, "Cô Lan", "AI chấm đúng", Instant.now())));
    }

    private void loggedInAs(UUID userId) {
        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(userId);
    }

    /**
     * "Was ever assigned", not "currently holding": an already COMPLETED assignment counts too. The mock returns
     * {@code false} by default so the negative cases need no extra stubbing.
     */
    private void givenEverAssignedTo(UUID assignedTeacherId) {
        when(assessmentGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, assignedTeacherId))
            .thenReturn(true);
    }

    private void schoolAdminIsRefused() {
        doThrow(new ForbiddenException("SECURITY")).when(assessmentGradingAccessService)
            .authorizeSchoolAdmin(any(), any());
    }

    @Test
    void should_let_the_owning_student_see_their_own_timeline() {
        loggedInAs(studentId);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        // The owner of the test does not go through the admin authorization branch.
        verify(assessmentGradingAccessService, never()).authorizeSchoolAdmin(any(), any());
    }

    /**
     * The timeline carries scoreBefore/scoreAfter -- for the owner themselves that IS the score, so it
     * follows exactly the same publication rule as the result screen. Return empty instead of throwing: this is an auxiliary
     * block, and throwing would break the whole page over a side item.
     */
    @Test
    void should_hide_timeline_from_the_owning_student_until_the_result_is_concluded() {
        result.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);
        loggedInAs(studentId);

        assertThat(useCase.execute(candidateResultId)).isEmpty();
    }

    @Test
    void should_still_show_the_timeline_to_the_assigned_teacher_while_pending_review() {
        result.setStatus(AssessmentCandidateResultStatus.PENDING_REVIEW);
        loggedInAs(teacherId);
        givenEverAssignedTo(teacherId);
        schoolAdminIsRefused();

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
    }

    @Test
    void should_let_the_teacher_currently_holding_the_paper_see_it() {
        loggedInAs(teacherId);
        givenEverAssignedTo(teacherId);
        schoolAdminIsRefused();

        // The score history is exactly the context a teacher needs when grading an appeal.
        assertThat(useCase.execute(candidateResultId)).hasSize(1);
    }

    /**
     * Once the score is submitted the assignment closes -- but the teacher's queue still lists that attempt
     * on the "Graded" tab, and the Score History button there must be clickable. The same rule as
     * listening to the recording ({@code AssessmentRecordingAccessService}).
     */
    @Test
    void should_let_a_teacher_who_was_ever_assigned_see_it_after_the_assignment_closed() {
        loggedInAs(teacherId);
        givenEverAssignedTo(teacherId);
        schoolAdminIsRefused();

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
    }

    @Test
    void should_refuse_a_teacher_who_was_never_assigned_to_this_paper() {
        loggedInAs(teacherId);
        // Someone else holds this submission; the logged-in user was never assigned.
        givenEverAssignedTo(UUID.randomUUID());
        schoolAdminIsRefused();

        assertThatThrownBy(() -> useCase.execute(candidateResultId))
            .isInstanceOf(ForbiddenException.class);
    }

    /**
     * Locks in the intent: read access is derived from "was ever assigned", NOT from the currently
     * open assignment row -- going back to the old way means a teacher who has just submitted a score loses the right to view it immediately.
     */
    @Test
    void should_not_look_at_the_open_assignment_row_to_authorize_a_teacher() {
        loggedInAs(teacherId);
        givenEverAssignedTo(teacherId);
        schoolAdminIsRefused();

        useCase.execute(candidateResultId);

        verify(assessmentGradingAssignmentRepository, never()).findOpenByCandidateResultId(any());
    }

    @Test
    void should_let_a_school_admin_of_the_same_school_see_it() {
        UUID adminId = UUID.randomUUID();
        loggedInAs(adminId);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        // The scope of the check is the ASSESSMENT'S SCHOOL, not the logged-in user's school.
        verify(assessmentGradingAccessService).authorizeSchoolAdmin(schoolId, adminId);
    }

    @Test
    void should_let_a_system_admin_see_it_without_a_school_check() {
        loggedInAs(UUID.randomUUID());
        when(userContextPort.isSystemAdmin()).thenReturn(true);

        assertThat(useCase.execute(candidateResultId)).hasSize(1);
        verify(assessmentGradingAccessService, never()).authorizeSchoolAdmin(any(), any());
    }

    @Test
    void should_fail_when_the_result_does_not_exist() {
        loggedInAs(studentId);
        UUID unknownId = UUID.randomUUID();
        when(assessmentCandidateResultRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(unknownId)).isInstanceOf(ResourceNotFoundException.class);
    }
}

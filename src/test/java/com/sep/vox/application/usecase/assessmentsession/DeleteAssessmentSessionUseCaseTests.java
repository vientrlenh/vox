package com.sep.vox.application.usecase.assessmentsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.assessmentsession.DeleteAssessmentSessionCommand;
import com.sep.vox.application.port.input.usecase.assessmentsession.DeleteAssessmentSessionUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentResultAppealRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Deleting a submission is a SOFT delete: the answer data must stay intact for cross-checking when a student questions a score,
 * and when and why it was deleted must be recorded.
 *
 * <p>The previous version hard-deleted the whole data tree across more than ten tables; those tests were dropped together with the cascade.
 */
public class DeleteAssessmentSessionUseCaseTests {

    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentResultAppealRepository assessmentResultAppealRepository;
    private UserContextPort userContextPort;
    private SchoolUserRepository schoolUserRepository;
    private UserRoleQueryRepository userRoleQueryRepository;
    private DeleteAssessmentSessionUseCase useCase;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    private Assessment assessment;

    @BeforeEach
    void setUp() {
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentResultAppealRepository = mock(AssessmentResultAppealRepository.class);
        userContextPort = mock(UserContextPort.class);
        schoolUserRepository = mock(SchoolUserRepository.class);
        userRoleQueryRepository = mock(UserRoleQueryRepository.class);

        useCase = new DeleteAssessmentSessionUseCase(
            assessmentSessionRepository,
            assessmentRepository,
            assessmentMemberRepository,
            schoolUserRepository,
            userRoleQueryRepository,
            userContextPort,
            assessmentCandidateResultRepository,
            assessmentResultAppealRepository
        );

        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setAssessmentId(assessmentId);
        when(assessmentSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        assessment.setKind(AssessmentKind.CENTRALIZED);
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        // A SCHOOL_ADMIN of the same school as the assessment -- the branch that allows deletion.
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(adminId);
        when(schoolUserRepository.findByUserId(adminId))
            .thenReturn(Optional.of(SchoolUser.builder()
                .schoolId(schoolId)
                .userId(adminId)
                .startDate(null)
                .endDate(null)
                .build()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(adminId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), adminId, UUID.randomUUID(), null, "SCHOOL_ADMIN", "School Admin")));

        when(assessmentSessionRepository.softDelete(eq(sessionId), any(), any())).thenReturn(1);
    }

    private void delete(String reason) {
        useCase.execute(new DeleteAssessmentSessionCommand(sessionId, reason));
    }

    @Test
    void should_soft_delete_the_session_with_its_reason() {
        delete("Vào phòng thi lỗi, phải thi lại");

        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(assessmentSessionRepository).softDelete(eq(sessionId), any(Instant.class), reason.capture());
        assertThat(reason.getValue()).isEqualTo("Vào phòng thi lỗi, phải thi lại");
    }

    /** The score must disappear from the result table, the grading queue and the appeals at the same time as the session. */
    @Test
    void should_soft_delete_the_candidate_result_together_with_the_session() {
        delete("Chấm lỗi");

        verify(assessmentCandidateResultRepository).softDeleteBySessionId(eq(sessionId), any(Instant.class), eq("Chấm lỗi"));
    }

    @Test
    void should_reject_a_blank_reason() {
        assertThatThrownBy(() -> delete("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reason");

        verify(assessmentSessionRepository, never()).softDelete(any(), any(), any());
    }

    @Test
    void should_trim_the_reason_before_storing_it() {
        delete("  Thí sinh mất kết nối  ");

        verify(assessmentSessionRepository).softDelete(eq(sessionId), any(Instant.class), eq("Thí sinh mất kết nối"));
    }

    /**
     * Once the assessment has closed or its results are published, the score has reached (or is about to reach) the student -- deleting at this point
     * makes the score vanish from the result table without the student understanding why.
     */
    @Test
    void should_refuse_when_the_assessment_is_closed() {
        assessment.setStatus(AssessmentStatus.CLOSED);

        assertThatThrownBy(() -> delete("Chấm lỗi"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("closed");

        verify(assessmentSessionRepository, never()).softDelete(any(), any(), any());
    }

    @Test
    void should_refuse_when_results_are_already_published() {
        assessment.setStatus(AssessmentStatus.RESULTS_PUBLISHED);

        assertThatThrownBy(() -> delete("Chấm lỗi")).isInstanceOf(IllegalArgumentException.class);

        verify(assessmentSessionRepository, never()).softDelete(any(), any(), any());
    }

    /**
     * Deleting a broken session RIGHT DURING the assessment is the very reason this feature exists (a bad room entry, a bad
     * grading run), so IN_PROGRESS must still be deletable.
     */
    @Test
    void should_allow_deleting_while_the_assessment_is_in_progress() {
        assessment.setStatus(AssessmentStatus.IN_PROGRESS);

        delete("Vào phòng thi lỗi");

        verify(assessmentSessionRepository).softDelete(eq(sessionId), any(Instant.class), any());
    }

    /** Clicking delete twice must not overwrite the reason and the timestamp of the first time. */
    @Test
    void should_not_touch_the_result_when_the_session_was_already_deleted() {
        when(assessmentSessionRepository.softDelete(eq(sessionId), any(), any())).thenReturn(0);

        delete("Xoá lần hai");

        verify(assessmentCandidateResultRepository, never()).softDeleteBySessionId(any(), any(), any());
    }

    @Test
    void should_refuse_a_teacher_who_is_not_chair() {
        givenTeacherCaller();
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, teacherId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);

        assertThatThrownBy(() -> delete("Chấm lỗi")).isInstanceOf(ForbiddenException.class);
    }

    /**
     * The chair can delete a broken session of an assessment they are in charge of, including a CENTRALIZED assessment. The old line that blocked
     * here was self-contradictory: the same person could already remove a candidate from a centralized assessment
     * (DeleteAssessmentCandidateUseCase), i.e. remove a whole person, yet could not remove one broken attempt.
     */
    @Test
    void should_allow_a_chair_on_a_centralized_assessment() {
        givenTeacherCaller();
        assessment.setKind(AssessmentKind.CENTRALIZED);
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, teacherId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);

        delete("Vào phòng thi lỗi");

        verify(assessmentSessionRepository).softDelete(eq(sessionId), any(Instant.class), any());
    }

    @Test
    void should_allow_a_chair_on_a_class_test() {
        givenTeacherCaller();
        assessment.setKind(AssessmentKind.CLASS_TEST);
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, teacherId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);

        delete("Chấm lỗi");

        verify(assessmentSessionRepository).softDelete(eq(sessionId), any(Instant.class), any());
    }

    /**
     * An open appeal is a score dispute the student has formally raised -- deleting the submission
     * now would leave the pending appeal pointing at a hidden row.
     */
    @Test
    void should_refuse_when_the_result_has_an_open_appeal() {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(UUID.randomUUID());
        when(assessmentCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.of(result));
        when(assessmentResultAppealRepository.existsOpenByCandidateResultId(result.getId())).thenReturn(true);

        assertThatThrownBy(() -> delete("Chấm lỗi"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("appeal");

        verify(assessmentSessionRepository, never()).softDelete(any(), any(), any());
    }

    @Test
    void should_allow_when_the_result_has_no_open_appeal() {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(UUID.randomUUID());
        when(assessmentCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.of(result));
        when(assessmentResultAppealRepository.existsOpenByCandidateResultId(result.getId())).thenReturn(false);

        delete("Chấm lỗi");

        verify(assessmentSessionRepository).softDelete(eq(sessionId), any(Instant.class), any());
    }

    private void givenTeacherCaller() {
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(teacherId);
        when(schoolUserRepository.findByUserId(teacherId))
            .thenReturn(Optional.of(SchoolUser.builder()
                .schoolId(schoolId)
                .userId(teacherId)
                .startDate(null)
                .endDate(null)
                .build()));
        when(userRoleQueryRepository.findByUserIdWithRoleInfo(teacherId)).thenReturn(List.of(
            new UserRoleInfo(UUID.randomUUID(), teacherId, UUID.randomUUID(), null, "TEACHER", "Teacher")));
    }
}

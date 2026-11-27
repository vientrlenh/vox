package com.sep.vox.application.usecase.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.AssessmentRecordingAccessService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleProctorRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;

/**
 * The four layers of permission to view a recording, of which the "assigned grader" layer is the easiest to forget:
 * the post-review round and the appeal deliberately hand the submission to a teacher who has NOTHING to do with the
 * schedule, so if permission only followed the proctor role, the grader opening the grading screen would lose the
 * video to listen to.
 */
class AssessmentRecordingAccessServiceTests {

    private AssessmentSessionRepository assessmentSessionRepository;
    private AssessmentRepository assessmentRepository;
    private AssessmentCandidateRepository assessmentCandidateRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentScheduleProctorRepository assessmentScheduleProctorRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private UserContextPort userContextPort;
    private AssessmentRecordingAccessService accessService;

    private final UUID userId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID candidateId = UUID.randomUUID();
    private final UUID candidateResultId = UUID.randomUUID();
    private final UUID scheduleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentScheduleProctorRepository = mock(AssessmentScheduleProctorRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        userContextPort = mock(UserContextPort.class);
        accessService = new AssessmentRecordingAccessService(
            assessmentSessionRepository, assessmentRepository, assessmentCandidateRepository, assessmentMemberRepository,
            assessmentScheduleProctorRepository, assessmentCandidateResultRepository,
            assessmentGradingAssignmentRepository, userContextPort);

        // Default: a teacher of the same school, NOT the chair, NOT a proctor, NOT assigned.
        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(userId);
        when(userContextPort.getCurrentSchoolId()).thenReturn(schoolId);
        when(userContextPort.isSchoolAdmin()).thenReturn(false);
        when(userContextPort.isTeacher()).thenReturn(true);
        when(assessmentSessionRepository.findById(sessionId)).thenReturn(Optional.of(session()));
        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment()));
        when(assessmentCandidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate()));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(false);
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId))
            .thenReturn(false);
        when(assessmentCandidateResultRepository.findBySessionId(sessionId))
            .thenReturn(Optional.of(candidateResult()));
        when(assessmentGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(false);
    }

    @Test
    void should_allow_assigned_grader_who_is_not_proctor() {
        when(assessmentGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(true);

        AssessmentSession session = accessService.requireCanViewRecordings(sessionId);

        assertThat(session.getId()).isEqualTo(sessionId);
    }

    /**
     * The proctor table is not queried once an assignment row exists: the grader is the most common case on this
     * screen, and letting it fall all the way to the last branch costs two extra queries every time a submission is
     * opened.
     */
    @Test
    void should_not_query_proctor_table_when_caller_is_assigned_grader() {
        when(assessmentGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(true);

        accessService.requireCanViewRecordings(sessionId);

        verify(assessmentScheduleProctorRepository, never()).existsByScheduleIdAndTeacherId(scheduleId, userId);
    }

    /**
     * After grading is done the recording must still be viewable: a submission under appeal is where the grader of the
     * previous round is asked "why did you grade it like that", and by then their assignment row is COMPLETED.
     */
    @Test
    void should_allow_grader_of_a_completed_assignment() {
        when(assessmentGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(true);

        assertThat(accessService.requireCanViewRecordings(sessionId).getId()).isEqualTo(sessionId);
    }

    @Test
    void should_allow_school_admin() {
        when(userContextPort.isSchoolAdmin()).thenReturn(true);
        when(userContextPort.isTeacher()).thenReturn(false);

        assertThat(accessService.requireCanViewRecordings(sessionId).getId()).isEqualTo(sessionId);
    }

    @Test
    void should_allow_assessment_chair() {
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessmentId, userId, AssessmentMemberRole.CHAIR))
            .thenReturn(true);

        assertThat(accessService.requireCanViewRecordings(sessionId).getId()).isEqualTo(sessionId);
    }

    @Test
    void should_allow_schedule_proctor() {
        when(assessmentScheduleProctorRepository.existsByScheduleIdAndTeacherId(scheduleId, userId))
            .thenReturn(true);

        assertThat(accessService.requireCanViewRecordings(sessionId).getId()).isEqualTo(sessionId);
    }

    @Test
    void should_reject_teacher_without_assignment_and_without_proctor_role() {
        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    /**
     * An attempt with no {@code AssessmentCandidateResult} yet (AI grading not finished) has nothing to match
     * an assignment against -- it must fall through to the proctor branch and must not be let through.
     */
    @Test
    void should_reject_when_session_has_no_candidate_result() {
        when(assessmentCandidateResultRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_teacher_from_another_school() {
        when(userContextPort.getCurrentSchoolId()).thenReturn(UUID.randomUUID());
        when(assessmentGradingAssignmentRepository
            .existsByCandidateResultIdAndTeacherId(candidateResultId, userId)).thenReturn(true);

        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_reject_user_without_school() {
        when(userContextPort.getCurrentSchoolId()).thenReturn(null);

        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    /** A student cannot review the video of their own session through this path. */
    @Test
    void should_reject_non_teacher_non_admin_role() {
        when(userContextPort.isTeacher()).thenReturn(false);

        assertThatThrownBy(() -> accessService.requireCanViewRecordings(sessionId))
            .isInstanceOf(ForbiddenException.class);
    }

    private AssessmentSession session() {
        AssessmentSession session = new AssessmentSession();
        session.setId(sessionId);
        session.setAssessmentId(assessmentId);
        session.setCandidateId(candidateId);
        return session;
    }

    private Assessment assessment() {
        Assessment assessment = new Assessment();
        assessment.setId(assessmentId);
        assessment.setSchoolId(schoolId);
        return assessment;
    }

    private AssessmentCandidate candidate() {
        AssessmentCandidate candidate = new AssessmentCandidate();
        candidate.setId(candidateId);
        candidate.setAssessmentId(assessmentId);
        candidate.setScheduleId(scheduleId);
        return candidate;
    }

    private AssessmentCandidateResult candidateResult() {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(candidateResultId);
        result.setAssessmentId(assessmentId);
        result.setCandidateId(candidateId);
        result.setSessionId(sessionId);
        return result;
    }
}

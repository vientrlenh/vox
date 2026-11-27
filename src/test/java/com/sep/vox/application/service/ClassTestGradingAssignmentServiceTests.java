package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.port.input.service.ClassTestGradingAssignmentService;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMember;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;

class ClassTestGradingAssignmentServiceTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();

    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private ClassTestGradingAssignmentService service;

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        service = new ClassTestGradingAssignmentService(
            assessmentRepository, assessmentMemberRepository, assessmentCandidateResultRepository, assessmentGradingAssignmentRepository);

        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment(AssessmentKind.CLASS_TEST)));
        when(assessmentMemberRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(
            AssessmentMember.builder()
                    .assessmentId(ASSESSMENT_ID)
                    .userId(CHAIR_ID)
                    .role(AssessmentMemberRole.CHAIR)
                    .grantedAt(Instant.now())
                    .grantedBy(CHAIR_ID)
                    .build()
        ));
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultId(any())).thenReturn(Optional.empty());
    }

    @Test
    void should_open_initial_assignment_for_class_test_chair() {
        service.ensureAssignmentForResult(result(AssessmentCandidateResultStatus.PENDING_REVIEW));

        ArgumentCaptor<AssessmentGradingAssignment> captor = ArgumentCaptor.forClass(AssessmentGradingAssignment.class);
        verify(assessmentGradingAssignmentRepository).save(captor.capture());
        AssessmentGradingAssignment assignment = captor.getValue();
        assertThat(assignment.getCandidateResultId()).isEqualTo(RESULT_ID);
        assertThat(assignment.getTeacherId()).isEqualTo(CHAIR_ID);
        assertThat(assignment.getAssignedBy()).isEqualTo(CHAIR_ID);
        assertThat(assignment.getRoundType()).isEqualTo(GradingRoundType.INITIAL);
        assertThat(assignment.getStatus()).isEqualTo(GradingAssignmentStatus.ASSIGNED);
        // activeResultId is what builds the "one open assignment per submission" invariant in the
        // DB.
        assertThat(assignment.getActiveResultId()).isEqualTo(RESULT_ID);
    }

    /**
     * The reference point for measuring the AI vs. human deviation: if it is taken later, the
     * teacher has already edited it.
     */
    @Test
    void should_capture_score_before_at_the_moment_of_opening() {
        AssessmentCandidateResult result = result(AssessmentCandidateResultStatus.PENDING_REVIEW);
        result.setTotalScore(new BigDecimal("6.25"));

        service.ensureAssignmentForResult(result);

        ArgumentCaptor<AssessmentGradingAssignment> captor = ArgumentCaptor.forClass(AssessmentGradingAssignment.class);
        verify(assessmentGradingAssignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getScoreBefore()).isEqualByComparingTo("6.25");
    }

    /**
     * A class test has no administrative grading deadline -- the deadline reminder job only fires
     * for rows that HAVE a deadline.
     */
    @Test
    void should_open_without_deadline() {
        service.ensureAssignmentForResult(result(AssessmentCandidateResultStatus.PENDING_REVIEW));

        ArgumentCaptor<AssessmentGradingAssignment> captor = ArgumentCaptor.forClass(AssessmentGradingAssignment.class);
        verify(assessmentGradingAssignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getDeadlineAt()).isNull();
    }

    @Test
    void should_ignore_centralized_assessment() {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment(AssessmentKind.CENTRALIZED)));

        service.ensureAssignmentForResult(result(AssessmentCandidateResultStatus.PENDING_REVIEW));

        verify(assessmentGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_ignore_result_that_is_not_pending_review() {
        service.ensureAssignmentForResult(result(AssessmentCandidateResultStatus.RELEASED));

        verify(assessmentGradingAssignmentRepository, never()).save(any());
    }

    /**
     * The barrier against violating the unique index {@code uq_grading_assignment_active_result}:
     * {@code ClearInvalidResultUseCase} has already opened a new INITIAL round itself.
     */
    @Test
    void should_not_open_second_assignment_when_one_is_already_open() {
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultId(RESULT_ID))
            .thenReturn(Optional.of(mock(AssessmentGradingAssignment.class)));

        service.ensureAssignmentForResult(result(AssessmentCandidateResultStatus.PENDING_REVIEW));

        verify(assessmentGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_ignore_assessment_without_chair() {
        when(assessmentMemberRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(
            AssessmentMember.builder()
                    .assessmentId(ASSESSMENT_ID)
                    .userId(UUID.randomUUID())
                    .role(AssessmentMemberRole.AUTHOR)
                    .grantedAt(Instant.now())
                    .grantedBy(CHAIR_ID)
                    .build()
        ));

        service.ensureAssignmentForResult(result(AssessmentCandidateResultStatus.PENDING_REVIEW));

        verify(assessmentGradingAssignmentRepository, never()).save(any());
    }

    @Test
    void should_sweep_only_pending_review_results_of_the_assessment() {
        AssessmentCandidateResult pending = result(AssessmentCandidateResultStatus.PENDING_REVIEW);
        AssessmentCandidateResult released = result(AssessmentCandidateResultStatus.RELEASED);
        released.setId(UUID.randomUUID());
        when(assessmentCandidateResultRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(pending, released));
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyList())).thenReturn(List.of());
        when(assessmentGradingAssignmentRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        int opened = service.ensureAssignmentsForAssessment(ASSESSMENT_ID);

        assertThat(opened).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssessmentGradingAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assessmentGradingAssignmentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
            .satisfies(assignment -> assertThat(assignment.getCandidateResultId()).isEqualTo(RESULT_ID));
    }

    /** Two hooks overlapping is normal -- the catch-up sweep must not double up. */
    @Test
    void should_skip_results_that_already_have_an_open_assignment_when_sweeping() {
        AssessmentCandidateResult pending = result(AssessmentCandidateResultStatus.PENDING_REVIEW);
        when(assessmentCandidateResultRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(pending));
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyList())).thenReturn(List.of(
            AssessmentGradingAssignment.open(
                RESULT_ID, CHAIR_ID, GradingRoundType.INITIAL, null, null, Instant.now(), CHAIR_ID, null)
        ));

        int opened = service.ensureAssignmentsForAssessment(ASSESSMENT_ID);

        assertThat(opened).isZero();
        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_not_sweep_centralized_assessment() {
        when(assessmentRepository.findById(ASSESSMENT_ID)).thenReturn(Optional.of(assessment(AssessmentKind.CENTRALIZED)));

        assertThat(service.ensureAssignmentsForAssessment(ASSESSMENT_ID)).isZero();
        verify(assessmentCandidateResultRepository, never()).findByAssessmentId(any());
    }

    private Assessment assessment(AssessmentKind kind) {
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setKind(kind);
        return assessment;
    }

    private AssessmentCandidateResult result(AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(RESULT_ID);
        result.setAssessmentId(ASSESSMENT_ID);
        result.setStatus(status);
        return result;
    }
}

package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Pins the authorization of class tests. The six school coordination use cases all
 * ask exactly the functions here, so the rule exists in one place only.
 */
class AssessmentGradingAccessClassTestTests {

    private static final UUID CLASS_TEST_ID = UUID.randomUUID();
    private static final UUID CENTRALIZED_ID = UUID.randomUUID();
    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID CLASS_TEST_RESULT_ID = UUID.randomUUID();
    private static final UUID CENTRALIZED_RESULT_ID = UUID.randomUUID();

    private AssessmentRepository assessmentRepository;
    private AssessmentMemberRepository assessmentMemberRepository;
    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentGradingAccessService service;

    @BeforeEach
    void setUp() {
        assessmentRepository = mock(AssessmentRepository.class);
        assessmentMemberRepository = mock(AssessmentMemberRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        service = new AssessmentGradingAccessService(
            mock(AssessmentGradingAssignmentRepository.class),
            assessmentCandidateResultRepository,
            mock(AssessmentSessionRepository.class),
            assessmentRepository,
            assessmentMemberRepository,
            mock(UserRepository.class),
            mock(SchoolUserRepository.class),
            mock(UserRoleQueryRepository.class),
            mock(UserContextPort.class));

        when(assessmentRepository.findById(CLASS_TEST_ID)).thenReturn(Optional.of(assessment(CLASS_TEST_ID, AssessmentKind.CLASS_TEST)));
        when(assessmentRepository.findById(CENTRALIZED_ID))
            .thenReturn(Optional.of(assessment(CENTRALIZED_ID, AssessmentKind.CENTRALIZED)));
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(CLASS_TEST_ID, CHAIR_ID, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
        when(assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(CENTRALIZED_ID, CHAIR_ID, AssessmentMemberRole.CHAIR))
            .thenReturn(true);
    }

    @Test
    void should_recognise_class_test_chair() {
        assertThat(service.isClassTestChair(CLASS_TEST_ID, CHAIR_ID)).isTrue();
    }

    @Test
    void should_reject_someone_who_is_not_chair() {
        assertThat(service.isClassTestChair(CLASS_TEST_ID, UUID.randomUUID())).isFalse();
        assertThatThrownBy(() -> service.authorizeClassTestChair(CLASS_TEST_ID, UUID.randomUUID()))
            .isInstanceOf(ForbiddenException.class);
    }

    /**
     * The important control case: the CHAIR of a CENTRALIZED assessment does not get this right.
     * A centralized assessment is coordinated by the school; the right to grade comes from the assignment row.
     */
    @Test
    void should_not_treat_centralized_chair_as_class_test_chair() {
        assertThat(service.isClassTestChair(CENTRALIZED_ID, CHAIR_ID)).isFalse();
    }

    /**
     * The control pair of the rule above, and the easiest place to mix up in this service: the SAME person,
     * the SAME centralized assessment — the WRITE door of the grading flow is closed, the READ door is open. The two functions must return opposite
     * values here; if one day they return the same, one of the two rules was deleted by mistake.
     */
    @Test
    void should_treat_centralized_chair_as_assessment_chair_for_reads() {
        assertThat(service.isAssessmentChair(CENTRALIZED_ID, CHAIR_ID)).isTrue();
        assertThat(service.isClassTestChair(CENTRALIZED_ID, CHAIR_ID)).isFalse();

        service.authorizeSchoolAdminOrAssessmentChair(SCHOOL_ID, CENTRALIZED_ID, CHAIR_ID);
        assertThatThrownBy(() -> service.authorizeSchoolAdminOrClassTestChair(
                SCHOOL_ID, CENTRALIZED_ID, CHAIR_ID))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_not_treat_a_non_member_as_assessment_chair() {
        assertThat(service.isAssessmentChair(CENTRALIZED_ID, UUID.randomUUID())).isFalse();
        assertThat(service.isAssessmentChair(null, CHAIR_ID)).isFalse();
        assertThatThrownBy(() -> service.authorizeSchoolAdminOrAssessmentChair(
                SCHOOL_ID, CENTRALIZED_ID, UUID.randomUUID()))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void should_flag_result_of_a_class_test() {
        when(assessmentCandidateResultRepository.findById(CLASS_TEST_RESULT_ID))
            .thenReturn(Optional.of(result(CLASS_TEST_RESULT_ID, CLASS_TEST_ID)));

        assertThat(service.isClassTestResult(CLASS_TEST_RESULT_ID)).isTrue();
        assertThatThrownBy(() -> service.rejectClassTestCoordination(CLASS_TEST_RESULT_ID))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("the school does not assign graders");
    }

    @Test
    void should_not_flag_result_of_a_centralized_assessment() {
        when(assessmentCandidateResultRepository.findById(CENTRALIZED_RESULT_ID))
            .thenReturn(Optional.of(result(CENTRALIZED_RESULT_ID, CENTRALIZED_ID)));

        assertThat(service.isClassTestResult(CENTRALIZED_RESULT_ID)).isFalse();
        service.rejectClassTestCoordination(CENTRALIZED_RESULT_ID);
    }

    /** Mixed batch: block the WHOLE BATCH, do not filter individually — the result must not depend on hidden data. */
    @Test
    void should_reject_a_mixed_batch_as_a_whole() {
        givenMixedBatch();

        assertThatThrownBy(() -> service.rejectClassTestCoordination(
                List.of(CLASS_TEST_RESULT_ID, CENTRALIZED_RESULT_ID)))
            .isInstanceOf(ForbiddenException.class);
    }

    /** The bulk-revocation branch needs to know EXACTLY which rows to drop, not "yes or no". */
    @Test
    void should_report_exactly_which_results_belong_to_class_tests() {
        givenMixedBatch();

        assertThat(service.classTestResultIds(List.of(CLASS_TEST_RESULT_ID, CENTRALIZED_RESULT_ID)))
            .containsExactly(CLASS_TEST_RESULT_ID);
    }

    @Test
    void should_treat_an_empty_batch_as_clean() {
        assertThat(service.classTestResultIds(List.of())).isEmpty();
        assertThat(service.anyClassTestResult(null)).isFalse();
    }

    private void givenMixedBatch() {
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(CLASS_TEST_RESULT_ID, CLASS_TEST_ID),
            result(CENTRALIZED_RESULT_ID, CENTRALIZED_ID)));
        when(assessmentRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            assessment(CLASS_TEST_ID, AssessmentKind.CLASS_TEST),
            assessment(CENTRALIZED_ID, AssessmentKind.CENTRALIZED)));
    }

    private Assessment assessment(UUID id, AssessmentKind kind) {
        Assessment assessment = new Assessment();
        assessment.setId(id);
        assessment.setKind(kind);
        return assessment;
    }

    private AssessmentCandidateResult result(UUID id, UUID assessmentId) {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(id);
        result.setAssessmentId(assessmentId);
        return result;
    }
}

package com.sep.vox.application.usecase.assessmentsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.usecase.assessmentsession.ViewMyAssessmentResultsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.assessmentsession.StudentAssessmentResultSummaryResponse;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentCandidate;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentSessionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * Danh sách kết quả của học sinh dùng chung một luật hiển thị với màn chi tiết. Trước đợt
 * này vị từ bị chép ra hai bản và cả hai đều bỏ sót PASSED/FAILED — hai nơi suy luật là
 * sớm muộn hai nơi lệch nhau.
 */
class ViewMyAssessmentResultsUseCaseTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID BAND_ID = UUID.randomUUID();

    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private ViewMyAssessmentResultsUseCase useCase;

    @BeforeEach
    void setUp() {
        AssessmentCandidateRepository assessmentCandidateRepository = mock(AssessmentCandidateRepository.class);
        AssessmentSessionRepository assessmentSessionRepository = mock(AssessmentSessionRepository.class);
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
        RubricResultBandRepository rubricResultBandRepository = mock(RubricResultBandRepository.class);
        RubricVersionRepository rubricVersionRepository =
            mock(com.sep.vox.domain.repository.RubricVersionRepository.class);
        UserContextPort userContextPort = mock(UserContextPort.class);
        useCase = new ViewMyAssessmentResultsUseCase(
            assessmentCandidateRepository,
            assessmentSessionRepository,
            assessmentCandidateResultRepository,
            assessmentRepository,
            rubricResultBandRepository,
            rubricVersionRepository,
            userContextPort
        );

        when(userContextPort.getCurrentAuthenticatedUserId()).thenReturn(STUDENT_ID);
        when(assessmentCandidateRepository.findByStudentId(STUDENT_ID)).thenReturn(List.of(AssessmentCandidate.builder()
                .id(CANDIDATE_ID)
                .assessmentId(ASSESSMENT_ID)
                .studentId(STUDENT_ID)
                .assignedPaperId(null)
                .scheduleId(null)
                .status(AssessmentCandidateStatus.ATTENDED)
                .assignedAt(Instant.now())
                .updatedAt(Instant.now())
                .blockedAt(null)
                .assignedBy(null)
                .updatedBy(null)
                .build()));
        Assessment assessment = new Assessment();
        assessment.setId(ASSESSMENT_ID);
        assessment.setCode("CT-01");
        assessment.setName("Bài tập 1");
        assessment.setKind(AssessmentKind.CLASS_TEST);
        when(assessmentRepository.findByIdIn(List.of(ASSESSMENT_ID))).thenReturn(List.of(assessment));
        when(assessmentSessionRepository.findLatestByCandidateId(CANDIDATE_ID)).thenReturn(Optional.of(
            AssessmentSession.builder()
                    .id(SESSION_ID)
                    .assessmentId(ASSESSMENT_ID)
                    .candidateId(CANDIDATE_ID)
                    .paperId(UUID.randomUUID())
                    .startedAt(Instant.now())
                    .submittedAt(Instant.now())
                    .status(AssessmentSessionStatus.GRADED)
                    .flagged(false)
                    .flagReason(null)
                    .build()));
        when(rubricResultBandRepository.findById(BAND_ID)).thenReturn(Optional.empty());
    }

    @Test
    void should_hide_total_score_when_result_pending_review() {
        givenResult(AssessmentCandidateResultStatus.PENDING_REVIEW);

        List<StudentAssessmentResultSummaryResponse> summaries = useCase.execute(null);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).totalScore()).isNull();
        assertThat(summaries.get(0).rubricResultBandId()).isNull();
        // The status is still returned so the list can show "waiting for grading".
        assertThat(summaries.get(0).resultStatus()).isEqualTo(AssessmentCandidateResultStatus.PENDING_REVIEW);
    }

    @Test
    void should_show_total_score_when_result_released() {
        givenResult(AssessmentCandidateResultStatus.RELEASED);

        List<StudentAssessmentResultSummaryResponse> summaries = useCase.execute(null);

        assertThat(summaries.get(0).totalScore()).isEqualByComparingTo("7.50");
        assertThat(summaries.get(0).rubricResultBandId()).isEqualTo(BAND_ID);
    }

    /** Vị từ cũ bỏ sót PASSED nên bài đã chốt lại bị giấu điểm khỏi chính chủ. */
    @Test
    void should_show_total_score_when_result_passed() {
        givenResult(AssessmentCandidateResultStatus.PASSED);

        assertThat(useCase.execute(null).get(0).totalScore()).isEqualByComparingTo("7.50");
    }

    @Test
    void should_hide_total_score_while_the_appeal_is_being_processed() {
        givenResult(AssessmentCandidateResultStatus.APPEALED);

        assertThat(useCase.execute(null).get(0).totalScore()).isNull();
    }

    private void givenResult(AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = AssessmentCandidateResult.builder()
                .id(UUID.randomUUID())
                .assessmentId(ASSESSMENT_ID)
                .candidateId(CANDIDATE_ID)
                .sessionId(SESSION_ID)
                .assessmentPolicyId(UUID.randomUUID())
                .policyVersion(1)
                .rubricVersionId(UUID.randomUUID())
                .frameworkVersionId(UUID.randomUUID())
                .targetFrameworkBandId(null)
                .rubricResultBandId(BAND_ID)
                .totalScore(new BigDecimal("7.50"))
                .status(status)
                .releasedAt(null)
                .finalizedAt(null)
                .build();
        when(assessmentCandidateResultRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(result));
    }
}

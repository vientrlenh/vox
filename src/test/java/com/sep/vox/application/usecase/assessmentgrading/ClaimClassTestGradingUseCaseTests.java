package com.sep.vox.application.usecase.assessmentgrading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.ClaimClassTestGradingCommand;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClaimClassTestGradingUseCase;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentGradingAssignment;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentGradingAssignmentRepository;

class ClaimClassTestGradingUseCaseTests {

    private static final UUID ASSESSMENT_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();

    private AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private AssessmentGradingAssignmentRepository assessmentGradingAssignmentRepository;
    private AssessmentGradingAccessService assessmentGradingAccessService;
    private ClaimClassTestGradingUseCase useCase;

    @BeforeEach
    void setUp() {
        assessmentCandidateResultRepository = mock(AssessmentCandidateResultRepository.class);
        assessmentGradingAssignmentRepository = mock(AssessmentGradingAssignmentRepository.class);
        assessmentGradingAccessService = mock(AssessmentGradingAccessService.class);
        useCase = new ClaimClassTestGradingUseCase(
            assessmentCandidateResultRepository, assessmentGradingAssignmentRepository, assessmentGradingAccessService);

        when(assessmentGradingAccessService.requireActiveUserId()).thenReturn(CHAIR_ID);
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection()))
            .thenReturn(List.of(result(RESULT_ID, ASSESSMENT_ID, AssessmentCandidateResultStatus.RELEASED)));
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection())).thenReturn(List.of());
        when(assessmentGradingAssignmentRepository.saveAll(anyList())).thenAnswer(call -> {
            @SuppressWarnings("unchecked")
            List<AssessmentGradingAssignment> assignments = (List<AssessmentGradingAssignment>) call.getArgument(0);
            assignments.forEach(assignment -> assignment.setId(UUID.randomUUID()));
            return assignments;
        });
    }

    @Test
    void should_open_spot_check_assignment_for_the_chair_themselves() {
        List<UUID> ids = useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID));

        assertThat(ids).hasSize(1);
        List<AssessmentGradingAssignment> captor = captureSaved();
        assertThat(captor).singleElement().satisfies(assignment -> {
            assertThat(assignment.getTeacherId()).isEqualTo(CHAIR_ID);
            assertThat(assignment.getAssignedBy()).isEqualTo(CHAIR_ID);
            assertThat(assignment.getRoundType()).isEqualTo(GradingRoundType.SPOT_CHECK);
            assertThat(assignment.getDeadlineAt()).isNull();
        });
    }

    @Test
    void should_capture_score_before_at_claim_time() {
        AssessmentCandidateResult result = result(RESULT_ID, ASSESSMENT_ID, AssessmentCandidateResultStatus.RELEASED);
        result.setTotalScore(new BigDecimal("7.50"));
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(result));

        useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID));

        assertThat(captureSaved()).singleElement()
            .satisfies(assignment -> assertThat(assignment.getScoreBefore()).isEqualByComparingTo("7.50"));
    }

    @Test
    void should_reject_when_caller_is_not_the_chair() {
        doThrow(new ForbiddenException("BẢO MẬT"))
            .when(assessmentGradingAccessService).authorizeClassTestChair(eq(ASSESSMENT_ID), eq(CHAIR_ID));

        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID)))
            .isInstanceOf(ForbiddenException.class);

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    /** Vòng phúc khảo gắn với một đơn cụ thể — phải đi qua màn đơn, nơi biết luật COI. */
    @Test
    void should_reject_appeal_round() {
        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.APPEAL, RESULT_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("màn đơn phúc khảo");

        verify(assessmentGradingAssignmentRepository, never()).saveAll(anyList());
    }

    /** The assessmentId in the URL is the thing that has passed the permission check -- an attempt of another test must be blocked. */
    @Test
    void should_reject_result_belonging_to_another_assessment() {
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(RESULT_ID, UUID.randomUUID(), AssessmentCandidateResultStatus.RELEASED)));

        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("không thuộc bài kiểm tra này");
    }

    /** SPOT_CHECK chỉ nhận bài đã RELEASED — luật nằm ở GradingRoundPolicy, không suy lại. */
    @Test
    void should_reject_result_whose_status_does_not_match_the_round() {
        when(assessmentCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(RESULT_ID, ASSESSMENT_ID, AssessmentCandidateResultStatus.PENDING_REVIEW)));

        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SPOT_CHECK");
    }

    @Test
    void should_reject_result_that_is_already_being_graded() {
        when(assessmentGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection())).thenReturn(List.of(
            AssessmentGradingAssignment.open(
                RESULT_ID, CHAIR_ID, GradingRoundType.INITIAL, null, null, Instant.now(), CHAIR_ID, null)));

        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID)))
            .isInstanceOf(ResourceDuplicatedException.class)
            .hasMessageContaining("đang được chấm");
    }

    @Test
    void should_reject_duplicated_results_in_one_batch() {
        assertThatThrownBy(() -> useCase.execute(
                new ClaimClassTestGradingCommand(
                    ASSESSMENT_ID, GradingRoundType.SPOT_CHECK, List.of(RESULT_ID, RESULT_ID))))
            .isInstanceOf(ResourceDuplicatedException.class);
    }

    @Test
    void should_reject_an_empty_batch() {
        assertThatThrownBy(() -> useCase.execute(
                new ClaimClassTestGradingCommand(ASSESSMENT_ID, GradingRoundType.SPOT_CHECK, List.of())))
            .isInstanceOf(IllegalArgumentException.class);

        verify(assessmentGradingAssignmentRepository, never()).saveAll(any());
    }

    private List<AssessmentGradingAssignment> captureSaved() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AssessmentGradingAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(assessmentGradingAssignmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private ClaimClassTestGradingCommand command(GradingRoundType roundType, UUID... resultIds) {
        return new ClaimClassTestGradingCommand(ASSESSMENT_ID, roundType, List.of(resultIds));
    }

    private AssessmentCandidateResult result(UUID id, UUID assessmentId, AssessmentCandidateResultStatus status) {
        AssessmentCandidateResult result = new AssessmentCandidateResult();
        result.setId(id);
        result.setAssessmentId(assessmentId);
        result.setStatus(status);
        return result;
    }
}

package com.sep.vox.application.usecase.examgrading;

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

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ClaimClassTestGradingCommand;
import com.sep.vox.application.port.input.service.ExamGradingAccessService;
import com.sep.vox.application.port.input.usecase.examgrading.ClaimClassTestGradingUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamGradingAssignment;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamGradingAssignmentRepository;

class ClaimClassTestGradingUseCaseTests {

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID CHAIR_ID = UUID.randomUUID();
    private static final UUID RESULT_ID = UUID.randomUUID();

    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamGradingAssignmentRepository examGradingAssignmentRepository;
    private ExamGradingAccessService examGradingAccessService;
    private ClaimClassTestGradingUseCase useCase;

    @BeforeEach
    void setUp() {
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examGradingAssignmentRepository = mock(ExamGradingAssignmentRepository.class);
        examGradingAccessService = mock(ExamGradingAccessService.class);
        useCase = new ClaimClassTestGradingUseCase(
            examCandidateResultRepository, examGradingAssignmentRepository, examGradingAccessService);

        when(examGradingAccessService.requireActiveUserId()).thenReturn(CHAIR_ID);
        when(examCandidateResultRepository.findByIdIn(anyCollection()))
            .thenReturn(List.of(result(RESULT_ID, EXAM_ID, ExamCandidateResultStatus.RELEASED)));
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection())).thenReturn(List.of());
        when(examGradingAssignmentRepository.saveAll(anyList())).thenAnswer(call -> {
            @SuppressWarnings("unchecked")
            var assignments = (List<ExamGradingAssignment>) call.getArgument(0);
            assignments.forEach(assignment -> assignment.setId(UUID.randomUUID()));
            return assignments;
        });
    }

    @Test
    void should_open_spot_check_assignment_for_the_chair_themselves() {
        var ids = useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID));

        assertThat(ids).hasSize(1);
        var captor = captureSaved();
        assertThat(captor).singleElement().satisfies(assignment -> {
            assertThat(assignment.getTeacherId()).isEqualTo(CHAIR_ID);
            assertThat(assignment.getAssignedBy()).isEqualTo(CHAIR_ID);
            assertThat(assignment.getRoundType()).isEqualTo(GradingRoundType.SPOT_CHECK);
            assertThat(assignment.getDeadlineAt()).isNull();
        });
    }

    @Test
    void should_capture_score_before_at_claim_time() {
        var result = result(RESULT_ID, EXAM_ID, ExamCandidateResultStatus.RELEASED);
        result.setTotalScore(new BigDecimal("7.50"));
        when(examCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(result));

        useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID));

        assertThat(captureSaved()).singleElement()
            .satisfies(assignment -> assertThat(assignment.getScoreBefore()).isEqualByComparingTo("7.50"));
    }

    @Test
    void should_reject_when_caller_is_not_the_chair() {
        doThrow(new ForbiddenException("BẢO MẬT"))
            .when(examGradingAccessService).authorizeClassTestChair(eq(EXAM_ID), eq(CHAIR_ID));

        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID)))
            .isInstanceOf(ForbiddenException.class);

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    /** Vòng phúc khảo gắn với một đơn cụ thể — phải đi qua màn đơn, nơi biết luật COI. */
    @Test
    void should_reject_appeal_round() {
        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.APPEAL, RESULT_ID)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("màn đơn phúc khảo");

        verify(examGradingAssignmentRepository, never()).saveAll(anyList());
    }

    @Test
    void should_reject_unknown_round_type() {
        assertThatThrownBy(() -> useCase.execute(
                new ClaimClassTestGradingCommand(EXAM_ID, "KHONG_CO_THAT", List.of(RESULT_ID))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Vòng chấm không hợp lệ");
    }

    /** examId ở URL mới là thứ đã qua phân quyền — bài của bài kiểm tra khác phải bị chặn. */
    @Test
    void should_reject_result_belonging_to_another_exam() {
        when(examCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(RESULT_ID, UUID.randomUUID(), ExamCandidateResultStatus.RELEASED)));

        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID)))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("không thuộc bài kiểm tra này");
    }

    /** SPOT_CHECK chỉ nhận bài đã RELEASED — luật nằm ở GradingRoundPolicy, không suy lại. */
    @Test
    void should_reject_result_whose_status_does_not_match_the_round() {
        when(examCandidateResultRepository.findByIdIn(anyCollection())).thenReturn(List.of(
            result(RESULT_ID, EXAM_ID, ExamCandidateResultStatus.PENDING_REVIEW)));

        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SPOT_CHECK");
    }

    @Test
    void should_reject_result_that_is_already_being_graded() {
        when(examGradingAssignmentRepository.findOpenByCandidateResultIdIn(anyCollection())).thenReturn(List.of(
            ExamGradingAssignment.open(
                RESULT_ID, CHAIR_ID, GradingRoundType.INITIAL, null, null, Instant.now(), CHAIR_ID, null)));

        assertThatThrownBy(() -> useCase.execute(command(GradingRoundType.SPOT_CHECK, RESULT_ID)))
            .isInstanceOf(DuplicatedException.class)
            .hasMessageContaining("đang được chấm");
    }

    @Test
    void should_reject_duplicated_results_in_one_batch() {
        assertThatThrownBy(() -> useCase.execute(
                new ClaimClassTestGradingCommand(
                    EXAM_ID, GradingRoundType.SPOT_CHECK.name(), List.of(RESULT_ID, RESULT_ID))))
            .isInstanceOf(DuplicatedException.class);
    }

    @Test
    void should_reject_an_empty_batch() {
        assertThatThrownBy(() -> useCase.execute(
                new ClaimClassTestGradingCommand(EXAM_ID, GradingRoundType.SPOT_CHECK.name(), List.of())))
            .isInstanceOf(IllegalArgumentException.class);

        verify(examGradingAssignmentRepository, never()).saveAll(any());
    }

    private List<ExamGradingAssignment> captureSaved() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExamGradingAssignment>> captor = ArgumentCaptor.forClass(List.class);
        verify(examGradingAssignmentRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private ClaimClassTestGradingCommand command(GradingRoundType roundType, UUID... resultIds) {
        return new ClaimClassTestGradingCommand(EXAM_ID, roundType.name(), List.of(resultIds));
    }

    private ExamCandidateResult result(UUID id, UUID examId, ExamCandidateResultStatus status) {
        var result = new ExamCandidateResult();
        result.setId(id);
        result.setExamId(examId);
        result.setStatus(status);
        return result;
    }
}

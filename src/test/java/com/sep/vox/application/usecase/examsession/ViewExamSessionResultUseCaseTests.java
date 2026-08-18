package com.sep.vox.application.usecase.examsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import com.sep.vox.application.port.input.query.ViewExamSessionResultQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.service.ExamResultAccessService.SessionAccess;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator.CalculatedExamSessionResult;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator.ItemScore;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator.SectionScore;
import com.sep.vox.application.port.input.usecase.examsession.ViewExamSessionResultUseCase;
import com.sep.vox.domain.model.exam.ExamCandidateResult;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.model.exam.ExamSession;
import com.sep.vox.domain.model.exam.ExamSessionStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * Cửa duy nhất học sinh xem kết quả của mình, nên đây là chỗ khoá hai bất biến:
 * bài chưa có kết luận thì không lộ điểm cho chính chủ, còn giáo viên/admin thì luôn
 * xem được — họ cần điểm chưa công bố để có căn cứ mà chấm.
 *
 * <p>Trang vẫn phải mở được khi bị che: nếu ném lỗi thì học sinh chỉ thấy "không tìm thấy"
 * và không biết bài mình đang ở đâu.
 */
class ViewExamSessionResultUseCaseTests {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final UUID SECTION_ID = UUID.randomUUID();
    private static final UUID PAPER_ITEM_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID RESPONSE_ID = UUID.randomUUID();

    private ExamCandidateResultRepository examCandidateResultRepository;
    private ExamSessionResultCalculator examSessionResultCalculator;
    private FrameworkResultBandRepository frameworkResultBandRepository;
    private RubricResultBandRepository rubricResultBandRepository;
    private RubricVersionRepository rubricVersionRepository;
    private ExamResultAccessService examResultAccessService;
    private QuestionRepository questionRepository;
    private ExamItemResponseRepository examItemResponseRepository;
    private ExamItemEvaluationRepository examItemEvaluationRepository;
    private ExamPaperItemRepository examPaperItemRepository;
    private ViewExamSessionResultUseCase useCase;

    @BeforeEach
    void setUp() {
        examCandidateResultRepository = mock(ExamCandidateResultRepository.class);
        examSessionResultCalculator = mock(ExamSessionResultCalculator.class);
        frameworkResultBandRepository = mock(FrameworkResultBandRepository.class);
        rubricResultBandRepository = mock(RubricResultBandRepository.class);
        rubricVersionRepository = mock(RubricVersionRepository.class);
        examResultAccessService = mock(ExamResultAccessService.class);
        questionRepository = mock(QuestionRepository.class);
        examItemResponseRepository = mock(ExamItemResponseRepository.class);
        examItemEvaluationRepository = mock(ExamItemEvaluationRepository.class);
        examPaperItemRepository = mock(ExamPaperItemRepository.class);
        useCase = new ViewExamSessionResultUseCase(
            examCandidateResultRepository,
            examSessionResultCalculator,
            frameworkResultBandRepository,
            rubricResultBandRepository,
            rubricVersionRepository,
            examResultAccessService,
            questionRepository,
            examItemResponseRepository,
            examItemEvaluationRepository,
            examPaperItemRepository
        );
    }

    @Test
    void should_hide_score_from_candidate_when_result_pending_review() {
        givenAccess(true, false);
        givenResult(ExamCandidateResultStatus.PENDING_REVIEW);

        var response = useCase.execute(new ViewExamSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isFalse();
        assertThat(response.totalScore()).isNull();
        assertThat(response.rubricResultBandId()).isNull();
        assertThat(response.sections()).isEmpty();
        assertThat(response.items()).isEmpty();
    }

    @Test
    void should_still_return_status_when_score_hidden() {
        givenAccess(true, false);
        givenResult(ExamCandidateResultStatus.PENDING_REVIEW);

        var response = useCase.execute(new ViewExamSessionResultQuery(SESSION_ID));

        // Trang phải mở được để học sinh biết bài đang chờ chấm, thay vì "không tìm thấy".
        assertThat(response.status()).isEqualTo("PENDING_REVIEW");
        assertThat(response.sessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    void should_not_call_calculator_when_score_hidden() {
        givenAccess(true, false);
        givenResult(ExamCandidateResultStatus.PENDING_REVIEW);

        useCase.execute(new ViewExamSessionResultQuery(SESSION_ID));

        verify(examSessionResultCalculator, never()).calculate(any());
    }

    @Test
    void should_show_score_to_candidate_when_result_released() {
        givenAccess(true, false);
        givenResult(ExamCandidateResultStatus.RELEASED);
        givenCalculatedBreakdown();

        var response = useCase.execute(new ViewExamSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.totalScore()).isEqualByComparingTo("7.50");
        assertThat(response.sections()).hasSize(1);
        assertThat(response.items()).hasSize(1);
    }

    /**
     * Vị từ cũ chỉ cho FINAL/RELEASED nên bài bị gắn cờ rồi chốt thành PASSED lại bị giấu
     * khỏi chính chủ — lỗi tiềm ẩn được sửa cùng đợt này.
     */
    @Test
    void should_show_score_to_candidate_when_flagged_result_passed() {
        givenAccess(true, true);
        givenResult(ExamCandidateResultStatus.PASSED);
        givenCalculatedBreakdown();

        var response = useCase.execute(new ViewExamSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.totalScore()).isEqualByComparingTo("7.50");
    }

    @Test
    void should_show_score_to_teacher_when_result_pending_review() {
        givenAccess(false, false);
        givenResult(ExamCandidateResultStatus.PENDING_REVIEW);
        givenCalculatedBreakdown();

        var response = useCase.execute(new ViewExamSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.totalScore()).isEqualByComparingTo("7.50");
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void should_hide_breakdown_but_keep_total_when_result_invalid() {
        givenAccess(true, false);
        givenResult(ExamCandidateResultStatus.INVALID);

        var response = useCase.execute(new ViewExamSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isTrue();
        assertThat(response.sections()).isEmpty();
        assertThat(response.items()).isEmpty();
        verify(examSessionResultCalculator, never()).calculate(any());
    }

    @Test
    void should_hide_score_from_candidate_when_result_re_grading() {
        givenAccess(true, false);
        givenResult(ExamCandidateResultStatus.RE_GRADING);

        var response = useCase.execute(new ViewExamSessionResultQuery(SESSION_ID));

        assertThat(response.scoreVisible()).isFalse();
        assertThat(response.status()).isEqualTo("RE_GRADING");
    }

    private void givenAccess(boolean candidateOwner, boolean flagged) {
        var session = new ExamSession(
            SESSION_ID, EXAM_ID, CANDIDATE_ID, PAPER_ID, Instant.now(), Instant.now(),
            ExamSessionStatus.GRADED, flagged, flagged ? "nghi vấn" : null);
        when(examResultAccessService.authorizeSession(SESSION_ID))
            .thenReturn(new SessionAccess(session, candidateOwner));
    }

    private void givenResult(ExamCandidateResultStatus status) {
        var result = new ExamCandidateResult(
            UUID.randomUUID(), EXAM_ID, CANDIDATE_ID, SESSION_ID, UUID.randomUUID(), 1,
            UUID.randomUUID(), UUID.randomUUID(), null, null,
            new BigDecimal("7.50"), status, null, null, Instant.now(), Instant.now(), null, null);
        when(examCandidateResultRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(result));
    }

    private void givenCalculatedBreakdown() {
        // shouldIncludeBreakdown nay hỏi "mọi câu đã có bản chấm chưa" thay vì chỉ nhìn trạng
        // thái, nên bài có bảng điểm phải có response VÀ evaluation khớp nhau.
        var response = mock(com.sep.vox.domain.model.exam.ExamItemResponse.class);
        when(response.getId()).thenReturn(RESPONSE_ID);
        when(examItemResponseRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(response));
        var evaluation = mock(com.sep.vox.domain.model.exam.ExamItemEvaluation.class);
        when(evaluation.getResponseId()).thenReturn(RESPONSE_ID);
        when(examItemEvaluationRepository.findByResponseIdIn(List.of(RESPONSE_ID)))
            .thenReturn(List.of(evaluation));
        when(examSessionResultCalculator.calculate(SESSION_ID)).thenReturn(new CalculatedExamSessionResult(
            SESSION_ID, EXAM_ID, PAPER_ID, CANDIDATE_ID, null, new BigDecimal("7.50"), null, null,
            List.of(new SectionScore(SECTION_ID, "Part 1", new BigDecimal("7.50"))),
            List.of(new ItemScore(PAPER_ITEM_ID, RESPONSE_ID, SECTION_ID, QUESTION_ID,
                new BigDecimal("7.50"), new BigDecimal("7.50"))),
            false));
    }
}

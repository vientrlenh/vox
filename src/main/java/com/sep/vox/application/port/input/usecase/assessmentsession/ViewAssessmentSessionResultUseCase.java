package com.sep.vox.application.port.input.usecase.assessmentsession;

import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResult;
import com.sep.vox.domain.model.assessment.AssessmentItemResponse;
import com.sep.vox.domain.model.assessment.AssessmentPaperItem;
import com.sep.vox.domain.model.assessment.AssessmentSession;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewAssessmentSessionResultQuery;
import com.sep.vox.application.port.input.service.AssessmentResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.assessmentevaluation.AssessmentSessionResultCalculator;
import com.sep.vox.application.response.input.assessmentsession.AssessmentCandidateResultItemResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentCandidateResultResponse;
import com.sep.vox.application.response.input.assessmentsession.AssessmentCandidateResultSectionResponse;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.AssessmentItemResponseRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
@RequiredArgsConstructor
public class ViewAssessmentSessionResultUseCase implements IUseCase<ViewAssessmentSessionResultQuery, AssessmentCandidateResultResponse> {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentSessionResultCalculator assessmentSessionResultCalculator;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final AssessmentResultAccessService assessmentResultAccessService;
    private final QuestionRepository questionRepository;
    private final AssessmentItemResponseRepository assessmentItemResponseRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final com.sep.vox.domain.repository.QuestionAssetRepository questionAssetRepository;

    /**
     * The resources of the questions, loaded once and then mapped by questionId.
     *
     * <p>A question with several assets takes the first in the order the repo returns, the same rule as
     * {@code GetAssessmentSessionPaperUseCase} -- so the result page and the attempt screen do not look at two different
     * assets of the same question.
     */
    private java.util.Map<java.util.UUID, com.sep.vox.domain.dto.QuestionAssetDto> assetsByQuestionId(
            java.util.List<java.util.UUID> questionIds) {
        if (questionIds.isEmpty()) {
            return java.util.Map.of();
        }
        Map<UUID, QuestionAssetDto> assets = new java.util.HashMap<java.util.UUID, com.sep.vox.domain.dto.QuestionAssetDto>();
        for (UUID questionId : questionIds) {
            questionAssetRepository.findByQuestionId(questionId).stream()
                .findFirst()
                .map(com.sep.vox.domain.mapper.QuestionAssetDtoMapper::toDto)
                .ifPresent(asset -> assets.put(questionId, asset));
        }
        return assets;
    }

    /**
     * The question list with prompts. The prompts are loaded ONCE for the whole attempt instead of one query per question --
     * this endpoint runs every time the result page is opened, not once at grading time.
     *
     * <p>The order is kept as in {@code items}, i.e. the (section, item) order that
     * {@link AssessmentSessionResultCalculator} has already sorted -- the client numbers "Question 1, Question 2" by position
     * in this array so do not stream it through a Map again and shuffle the order.
     */
    private java.util.List<AssessmentCandidateResultItemResponse> itemResponses(
            java.util.List<AssessmentSessionResultCalculator.ItemScore> items) {
        List<UUID> questionIds = items.stream()
            .map(item -> item.questionId())
            .filter(id -> id != null)
            .distinct()
            .toList();
        Map<UUID, String> texts = new java.util.HashMap<java.util.UUID, String>();
        if (!questionIds.isEmpty()) {
            for (Question question : questionRepository.findByIdIn(questionIds)) {
                texts.put(question.getId(), question.getQuestionText());
            }
        }
        Map<UUID, QuestionAssetDto> assets = assetsByQuestionId(questionIds);
        return items.stream()
            .map(item -> new AssessmentCandidateResultItemResponse(
                item.paperItemId(),
                item.responseId(),
                item.sectionId(),
                item.questionId() == null ? null : texts.get(item.questionId()),
                item.questionId() == null ? null : assets.get(item.questionId()),
                item.itemScore(),
                item.weightedScore()
            ))
            .toList();
    }

    /**
     * The question list when grading is NOT finished -- built straight from the answers, without going through the calculator.
     *
     * <p>Previously an ungraded attempt returned {@code items = []}, and that was the whole reason the Result page was blank
     * after the violation was cleared: with no items the client has no {@code responseId}
     * to ask for details, so it NEVER CALLED the detail query -- the speech fallback patch in
     * {@code ViewAssessmentItemResponseEvaluationUseCase} sat behind a door nobody opened.
     *
     * <p>The score is {@code null} because an ungraded attempt really has no score. But the speech recording and the
     * transcript are NOT scores -- they are evidence, and there is no reason to make anyone wait until grading finishes
     * to see them. The grading screen already shows them from the start (JpaAssessmentGradingQueryRepository
     * LEFT JOINs to the evaluation); this pulls the result page onto the same assumption.
     */
    private java.util.List<AssessmentCandidateResultItemResponse> itemResponsesWithoutScores(
            java.util.UUID sessionId, java.util.UUID paperId) {
        Map<UUID, UUID> responseByPaperItemId = new java.util.HashMap<java.util.UUID, java.util.UUID>();
        for (AssessmentItemResponse response : assessmentItemResponseRepository.findBySessionId(sessionId)) {
            if (response.getPaperItemId() != null) {
                responseByPaperItemId.putIfAbsent(response.getPaperItemId(), response.getId());
            }
        }
        if (responseByPaperItemId.isEmpty() || paperId == null) {
            return java.util.List.of();
        }

        // Đi theo ĐỀ chứ không theo response: giữ đúng thứ tự (section, order) mà calculator dùng,
        // nên số "Câu 1, Câu 2" trên client không nhảy khi bài chuyển từ chưa chấm sang đã chấm.
        List<AssessmentPaperItem> paperItems = assessmentPaperItemRepository.findByPaperId(paperId).stream()
            .filter(paperItem -> responseByPaperItemId.containsKey(paperItem.getId()))
            .sorted(java.util.Comparator.comparingInt(paperItem -> paperItem.getOrder()))
            .toList();
        List<UUID> questionIds = paperItems.stream()
            .map(paperItem -> paperItem.getQuestionId())
            .filter(id -> id != null)
            .distinct()
            .toList();
        Map<UUID, String> texts = new java.util.HashMap<java.util.UUID, String>();
        if (!questionIds.isEmpty()) {
            for (Question question : questionRepository.findByIdIn(questionIds)) {
                texts.put(question.getId(), question.getQuestionText());
            }
        }
        Map<UUID, QuestionAssetDto> assets = assetsByQuestionId(questionIds);
        return paperItems.stream()
            .map(paperItem -> new AssessmentCandidateResultItemResponse(
                paperItem.getId(),
                responseByPaperItemId.get(paperItem.getId()),
                paperItem.getSectionId(),
                paperItem.getQuestionId() == null ? null : texts.get(paperItem.getQuestionId()),
                paperItem.getQuestionId() == null ? null : assets.get(paperItem.getQuestionId()),
                null,
                null
            ))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssessmentCandidateResultResponse execute(ViewAssessmentSessionResultQuery input) {
        AssessmentResultAccessService.SessionAccess access = assessmentResultAccessService.authorizeSession(input.sessionId());
        AssessmentSession session = access.session();
        AssessmentCandidateResult result = assessmentCandidateResultRepository.findBySessionId(session.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ket qua phien thi"));
        // Chính chủ chỉ xem được bài đã có kết luận; giáo viên/admin thì luôn xem được.
        // Trang vẫn trả về bản ghi kèm status — che field chứ không chặn, để học sinh còn
        // biết bài mình đang ở đâu thay vì gặp màn "không tìm thấy".
        boolean scoreVisible = !access.candidateOwner()
            || AssessmentCandidateResultStatus.isVisibleToCandidate(result.getStatus());
        boolean includeBreakdown = scoreVisible && shouldIncludeBreakdown(result.getStatus(), session.getId());
        AssessmentSessionResultCalculator.CalculatedAssessmentSessionResult calculated = includeBreakdown ? assessmentSessionResultCalculator.calculate(session.getId()) : null;
        FrameworkResultBand targetBand = result.getTargetFrameworkBandId() == null
            ? null
            : frameworkResultBandRepository.findById(result.getTargetFrameworkBandId()).orElse(null);
        RubricResultBand rubricBand = result.getRubricResultBandId() == null
            ? null
            : rubricResultBandRepository.findById(result.getRubricResultBandId()).orElse(null);
        // The scoring scale accompanies the result so the client does not have to guess -- see the javadoc at
        // AssessmentCandidateResultResponse.scoringScaleMin for what happens when it guesses.
        RubricVersion rubricVersion = !scoreVisible || result.getRubricVersionId() == null
            ? null
            : rubricVersionRepository.findById(result.getRubricVersionId()).orElse(null);

        return new AssessmentCandidateResultResponse(
            result.getId(),
            result.getSessionId(),
            result.getAssessmentId(),
            calculated == null ? session.getPaperId() : calculated.paperId(),
            result.getCandidateId(),
            session.isFlagged(),
            session.getFlagReason(),
            scoreVisible,
            scoreVisible ? result.getTotalScore() : null,
            rubricVersion == null ? null : rubricVersion.getScoringScaleMin(),
            rubricVersion == null ? null : rubricVersion.getScoringScaleMax(),
            scoreVisible ? result.getTargetFrameworkBandId() : null,
            scoreVisible && targetBand != null ? targetBand.getCode() : null,
            scoreVisible && targetBand != null ? targetBand.getLabel() : null,
            scoreVisible ? result.getRubricResultBandId() : null,
            scoreVisible && rubricBand != null ? rubricBand.getCode() : null,
            scoreVisible && rubricBand != null ? rubricBand.getName() : null,
            result.getStatus().name(),
            scoreVisible && calculated != null ? calculated.sections().stream()
                .map(section -> new AssessmentCandidateResultSectionResponse(section.sectionId(), section.title(), section.score()))
                .toList() : java.util.List.of(),
            // Chưa chấm xong (kể cả bài đang INVALID) thì vẫn trả danh sách câu, chỉ khuyết điểm
            // -- xem itemResponsesWithoutScores. Vẫn nằm sau scoreVisible để giữ nguyên luật cũ:
            // học sinh chưa được xem kết quả thì không thấy câu nào, y như trước.
            scoreVisible
                ? (calculated != null
                    ? itemResponses(calculated.items())
                    : itemResponsesWithoutScores(session.getId(), session.getPaperId()))
                : java.util.List.of()
        );
    }

    /**
     * Whether a detailed score table can be built for this attempt.
     *
     * <p>Asks EXACTLY the question that {@link AssessmentSessionResultCalculator} needs: "does every question have an evaluation yet".
     * The previous version asked "is the status INVALID" -- an indirect test, right only by coincidence
     * because an ungraded attempt is usually INVALID.
     *
     * <p>It breaks as soon as an ungraded attempt carries another status. Measured 2026-08-17 on session
     * 01a00e64: unblocking moved the attempt from INVALID to PENDING_REVIEW, the old condition let it pass, the calculator
     * met a question without an evaluation and threw ResourceNotFoundException -- breaking the WHOLE
     * {@code assessmentSessionResult} query, so the Result page showed "No answer data" even though the
     * answers were intact in the DB.
     *
     * <p>An attempt that is not fully graded returns without a score table; the page still shows the header, status and
     * suspicion flag -- a missing score is correct, while breaking the whole page is not.
     */
    private boolean shouldIncludeBreakdown(AssessmentCandidateResultStatus status, java.util.UUID sessionId) {
        if (status == AssessmentCandidateResultStatus.INVALID) {
            return false;
        }
        List<UUID> responseIds = assessmentItemResponseRepository.findBySessionId(sessionId).stream()
            .map(response -> response.getId())
            .toList();
        if (responseIds.isEmpty()) {
            return false;
        }
        Set<UUID> evaluatedResponseIds = assessmentItemEvaluationRepository.findByResponseIdIn(responseIds).stream()
            .map(evaluation -> evaluation.getResponseId())
            .collect(java.util.stream.Collectors.toSet());
        return evaluatedResponseIds.containsAll(responseIds);
    }
}

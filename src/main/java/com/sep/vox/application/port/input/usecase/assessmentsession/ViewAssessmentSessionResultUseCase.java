package com.sep.vox.application.port.input.usecase.assessmentsession;

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
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.assessment.AssessmentCandidateResultRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemEvaluationRepository;
import com.sep.vox.domain.repository.assessment.AssessmentItemAnswerRepository;
import com.sep.vox.domain.repository.assessment.AssessmentPaperItemRepository;

@Service
public class ViewAssessmentSessionResultUseCase implements IUseCase<ViewAssessmentSessionResultQuery, AssessmentCandidateResultResponse> {

    private final AssessmentCandidateResultRepository assessmentCandidateResultRepository;
    private final AssessmentSessionResultCalculator assessmentSessionResultCalculator;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final AssessmentResultAccessService assessmentResultAccessService;
    private final QuestionRepository questionRepository;
    private final AssessmentItemAnswerRepository assessmentItemAnswerRepository;
    private final AssessmentItemEvaluationRepository assessmentItemEvaluationRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final com.sep.vox.domain.repository.QuestionAssetRepository questionAssetRepository;

    public ViewAssessmentSessionResultUseCase(
            AssessmentCandidateResultRepository assessmentCandidateResultRepository,
            AssessmentSessionResultCalculator assessmentSessionResultCalculator,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            AssessmentResultAccessService assessmentResultAccessService,
            QuestionRepository questionRepository,
            AssessmentItemAnswerRepository assessmentItemAnswerRepository,
            AssessmentItemEvaluationRepository assessmentItemEvaluationRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            com.sep.vox.domain.repository.QuestionAssetRepository questionAssetRepository) {
        this.assessmentCandidateResultRepository = assessmentCandidateResultRepository;
        this.assessmentSessionResultCalculator = assessmentSessionResultCalculator;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.assessmentResultAccessService = assessmentResultAccessService;
        this.questionRepository = questionRepository;
        this.assessmentItemAnswerRepository = assessmentItemAnswerRepository;
        this.assessmentItemEvaluationRepository = assessmentItemEvaluationRepository;
        this.assessmentPaperItemRepository = assessmentPaperItemRepository;
        this.questionAssetRepository = questionAssetRepository;
    }

    /**
     * Tài nguyên của các câu, nạp một lần rồi map theo questionId.
     *
     * <p>Câu có nhiều asset thì lấy cái đầu theo thứ tự repo trả về, cùng quy tắc với
     * {@code GetAssessmentSessionPaperUseCase} -- để trang kết quả và màn thi không nhìn vào hai asset
     * khác nhau của cùng một câu.
     */
    private java.util.Map<java.util.UUID, com.sep.vox.domain.dto.QuestionAssetDto> assetsByQuestionId(
            java.util.List<java.util.UUID> questionIds) {
        if (questionIds.isEmpty()) {
            return java.util.Map.of();
        }
        var assets = new java.util.HashMap<java.util.UUID, com.sep.vox.domain.dto.QuestionAssetDto>();
        for (var questionId : questionIds) {
            questionAssetRepository.findByQuestionId(questionId).stream()
                .findFirst()
                .map(com.sep.vox.domain.mapper.QuestionAssetDto::toDto)
                .ifPresent(asset -> assets.put(questionId, asset));
        }
        return assets;
    }

    /**
     * Danh sách câu kèm đề bài. Đề bài nạp MỘT lần cho cả bài thay vì mỗi câu một truy vấn --
     * endpoint này chạy mỗi lần mở trang kết quả, không phải một lần lúc chấm.
     *
     * <p>Thứ tự giữ nguyên theo {@code items}, tức thứ tự (section, item) mà
     * {@link AssessmentSessionResultCalculator} đã sắp -- client đánh số "Câu 1, Câu 2" theo vị trí
     * trong mảng này nên đừng stream lại qua Map làm xáo thứ tự.
     */
    private java.util.List<AssessmentCandidateResultItemResponse> itemResponses(
            java.util.List<AssessmentSessionResultCalculator.ItemScore> items) {
        var questionIds = items.stream()
            .map(item -> item.questionId())
            .filter(id -> id != null)
            .distinct()
            .toList();
        var texts = new java.util.HashMap<java.util.UUID, String>();
        if (!questionIds.isEmpty()) {
            for (var question : questionRepository.findByIdIn(questionIds)) {
                texts.put(question.getId(), question.getQuestionText());
            }
        }
        var assets = assetsByQuestionId(questionIds);
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
     * Danh sách câu khi CHƯA chấm xong -- dựng thẳng từ câu trả lời, không qua calculator.
     *
     * <p>Trước đây bài chưa chấm trả về {@code items = []}, và đó là toàn bộ lý do trang Xem kết
     * quả trắng trơn sau khi gỡ vi phạm: không có item thì client không có {@code responseId} nào
     * để hỏi chi tiết, nên nó KHÔNG GỌI truy vấn chi tiết lần nào -- bản vá fallback lượt nói ở
     * {@code ViewAssessmentItemAnswerEvaluationUseCase} nằm sau một cánh cửa không ai mở.
     *
     * <p>Điểm để {@code null} vì chưa chấm thì thật sự chưa có điểm. Nhưng bản ghi tiếng nói và
     * transcript KHÔNG phải điểm -- chúng là bằng chứng, và không có lý do gì bắt chờ chấm xong
     * mới cho xem. Màn chấm vốn đã hiện chúng ngay từ đầu (JpaAssessmentGradingQueryRepository
     * LEFT JOIN sang evaluation); đây là kéo trang kết quả về cùng một giả định.
     */
    private java.util.List<AssessmentCandidateResultItemResponse> itemResponsesWithoutScores(
            java.util.UUID sessionId, java.util.UUID paperId) {
        var responseByPaperItemId = new java.util.HashMap<java.util.UUID, java.util.UUID>();
        for (var response : assessmentItemAnswerRepository.findBySessionId(sessionId)) {
            if (response.getPaperItemId() != null) {
                responseByPaperItemId.putIfAbsent(response.getPaperItemId(), response.getId());
            }
        }
        if (responseByPaperItemId.isEmpty() || paperId == null) {
            return java.util.List.of();
        }

        // Đi theo ĐỀ chứ không theo response: giữ đúng thứ tự (section, order) mà calculator dùng,
        // nên số "Câu 1, Câu 2" trên client không nhảy khi bài chuyển từ chưa chấm sang đã chấm.
        var paperItems = assessmentPaperItemRepository.findByPaperId(paperId).stream()
            .filter(paperItem -> responseByPaperItemId.containsKey(paperItem.getId()))
            .sorted(java.util.Comparator.comparingInt(paperItem -> paperItem.getOrder()))
            .toList();
        var questionIds = paperItems.stream()
            .map(paperItem -> paperItem.getQuestionId())
            .filter(id -> id != null)
            .distinct()
            .toList();
        var texts = new java.util.HashMap<java.util.UUID, String>();
        if (!questionIds.isEmpty()) {
            for (var question : questionRepository.findByIdIn(questionIds)) {
                texts.put(question.getId(), question.getQuestionText());
            }
        }
        var assets = assetsByQuestionId(questionIds);
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
        var access = assessmentResultAccessService.authorizeSession(input.sessionId());
        var session = access.session();
        var result = assessmentCandidateResultRepository.findBySessionId(session.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ket qua phien thi"));
        // Chính chủ chỉ xem được bài đã có kết luận; giáo viên/admin thì luôn xem được.
        // Trang vẫn trả về bản ghi kèm status — che field chứ không chặn, để học sinh còn
        // biết bài mình đang ở đâu thay vì gặp màn "không tìm thấy".
        var scoreVisible = !access.candidateOwner()
            || AssessmentCandidateResultStatus.isVisibleToCandidate(result.getStatus());
        var includeBreakdown = scoreVisible && shouldIncludeBreakdown(result.getStatus(), session.getId());
        var calculated = includeBreakdown ? assessmentSessionResultCalculator.calculate(session.getId()) : null;
        var targetBand = result.getTargetFrameworkBandId() == null
            ? null
            : frameworkResultBandRepository.findById(result.getTargetFrameworkBandId()).orElse(null);
        var rubricBand = result.getRubricResultBandId() == null
            ? null
            : rubricResultBandRepository.findById(result.getRubricResultBandId()).orElse(null);
        // Thang điểm đi kèm kết quả để client khỏi phải đoán -- xem javadoc ở
        // AssessmentCandidateResultResponse.scoringScaleMin để biết chuyện gì xảy ra khi nó đoán.
        var rubricVersion = !scoreVisible || result.getRubricVersionId() == null
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
     * Có dựng được bảng điểm chi tiết cho bài này không.
     *
     * <p>Hỏi ĐÚNG câu mà {@link AssessmentSessionResultCalculator} cần: "mọi câu đã có bản chấm chưa".
     * Bản trước hỏi "trạng thái có phải INVALID không" -- một phép thử gián tiếp, đúng tình cờ
     * vì bài chưa chấm thường đang ở INVALID.
     *
     * <p>Nó vỡ ngay khi bài chưa chấm mang trạng thái khác. Đo được 2026-08-17 trên phiên
     * 01a00e64: gỡ chặn đưa bài từ INVALID sang PENDING_REVIEW, điều kiện cũ cho qua, calculator
     * gặp câu không có evaluation và ném ResourceNotFoundException -- hỏng CẢ truy vấn
     * {@code assessmentSessionResult}, nên trang Xem kết quả hiện "Không có dữ liệu trả lời" dù câu
     * trả lời còn nguyên trong DB.
     *
     * <p>Bài chưa chấm đủ thì trả về không có bảng điểm; trang vẫn hiện đầu bài, trạng thái và
     * cờ nghi vấn -- thiếu điểm là đúng, còn hỏng cả trang thì không.
     */
    private boolean shouldIncludeBreakdown(AssessmentCandidateResultStatus status, java.util.UUID sessionId) {
        if (status == AssessmentCandidateResultStatus.INVALID) {
            return false;
        }
        var responseIds = assessmentItemAnswerRepository.findBySessionId(sessionId).stream()
            .map(response -> response.getId())
            .toList();
        if (responseIds.isEmpty()) {
            return false;
        }
        var evaluatedResponseIds = assessmentItemEvaluationRepository.findByResponseIdIn(responseIds).stream()
            .map(evaluation -> evaluation.getResponseId())
            .collect(java.util.stream.Collectors.toSet());
        return evaluatedResponseIds.containsAll(responseIds);
    }
}

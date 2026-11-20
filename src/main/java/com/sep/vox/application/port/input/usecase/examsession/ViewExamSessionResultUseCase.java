package com.sep.vox.application.port.input.usecase.examsession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewExamSessionResultQuery;
import com.sep.vox.application.port.input.service.ExamResultAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.examevaluation.ExamSessionResultCalculator;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultItemResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultResponse;
import com.sep.vox.application.response.input.examsession.ExamCandidateResultSectionResponse;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
import com.sep.vox.domain.repository.ExamCandidateResultRepository;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

@Service
public class ViewExamSessionResultUseCase implements IUseCase<ViewExamSessionResultQuery, ExamCandidateResultResponse> {

    private final ExamCandidateResultRepository examCandidateResultRepository;
    private final ExamSessionResultCalculator examSessionResultCalculator;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final ExamResultAccessService examResultAccessService;
    private final QuestionRepository questionRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final ExamPaperItemRepository examPaperItemRepository;
    private final com.sep.vox.domain.repository.QuestionAssetRepository questionAssetRepository;

    public ViewExamSessionResultUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionResultCalculator examSessionResultCalculator,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            ExamResultAccessService examResultAccessService,
            QuestionRepository questionRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            ExamPaperItemRepository examPaperItemRepository,
            com.sep.vox.domain.repository.QuestionAssetRepository questionAssetRepository) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionResultCalculator = examSessionResultCalculator;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.examResultAccessService = examResultAccessService;
        this.questionRepository = questionRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.examPaperItemRepository = examPaperItemRepository;
        this.questionAssetRepository = questionAssetRepository;
    }

    /**
     * Tài nguyên của các câu, nạp một lần rồi map theo questionId.
     *
     * <p>Câu có nhiều asset thì lấy cái đầu theo thứ tự repo trả về, cùng quy tắc với
     * {@code GetExamSessionPaperUseCase} -- để trang kết quả và màn thi không nhìn vào hai asset
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
                .map(com.sep.vox.domain.mapper.QuestionAssetDtoMapper::toDto)
                .ifPresent(asset -> assets.put(questionId, asset));
        }
        return assets;
    }

    /**
     * Danh sách câu kèm đề bài. Đề bài nạp MỘT lần cho cả bài thay vì mỗi câu một truy vấn --
     * endpoint này chạy mỗi lần mở trang kết quả, không phải một lần lúc chấm.
     *
     * <p>Thứ tự giữ nguyên theo {@code items}, tức thứ tự (section, item) mà
     * {@link ExamSessionResultCalculator} đã sắp -- client đánh số "Câu 1, Câu 2" theo vị trí
     * trong mảng này nên đừng stream lại qua Map làm xáo thứ tự.
     */
    private java.util.List<ExamCandidateResultItemResponse> itemResponses(
            java.util.List<ExamSessionResultCalculator.ItemScore> items) {
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
            .map(item -> new ExamCandidateResultItemResponse(
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
     * {@code ViewExamItemResponseEvaluationUseCase} nằm sau một cánh cửa không ai mở.
     *
     * <p>Điểm để {@code null} vì chưa chấm thì thật sự chưa có điểm. Nhưng bản ghi tiếng nói và
     * transcript KHÔNG phải điểm -- chúng là bằng chứng, và không có lý do gì bắt chờ chấm xong
     * mới cho xem. Màn chấm vốn đã hiện chúng ngay từ đầu (JpaExamGradingQueryRepository
     * LEFT JOIN sang evaluation); đây là kéo trang kết quả về cùng một giả định.
     */
    private java.util.List<ExamCandidateResultItemResponse> itemResponsesWithoutScores(
            java.util.UUID sessionId, java.util.UUID paperId) {
        var responseByPaperItemId = new java.util.HashMap<java.util.UUID, java.util.UUID>();
        for (var response : examItemResponseRepository.findBySessionId(sessionId)) {
            if (response.getPaperItemId() != null) {
                responseByPaperItemId.putIfAbsent(response.getPaperItemId(), response.getId());
            }
        }
        if (responseByPaperItemId.isEmpty() || paperId == null) {
            return java.util.List.of();
        }

        // Đi theo ĐỀ chứ không theo response: giữ đúng thứ tự (section, order) mà calculator dùng,
        // nên số "Câu 1, Câu 2" trên client không nhảy khi bài chuyển từ chưa chấm sang đã chấm.
        var paperItems = examPaperItemRepository.findByPaperId(paperId).stream()
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
            .map(paperItem -> new ExamCandidateResultItemResponse(
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
    public ExamCandidateResultResponse execute(ViewExamSessionResultQuery input) {
        var access = examResultAccessService.authorizeSession(input.sessionId());
        var session = access.session();
        var result = examCandidateResultRepository.findBySessionId(session.getId())
            .orElseThrow(() -> new NotFoundException("Khong tim thay ket qua phien thi"));
        // Chính chủ chỉ xem được bài đã có kết luận; giáo viên/admin thì luôn xem được.
        // Trang vẫn trả về bản ghi kèm status — che field chứ không chặn, để học sinh còn
        // biết bài mình đang ở đâu thay vì gặp màn "không tìm thấy".
        var scoreVisible = !access.candidateOwner()
            || ExamCandidateResultStatus.isVisibleToCandidate(result.getStatus());
        var includeBreakdown = scoreVisible && shouldIncludeBreakdown(result.getStatus(), session.getId());
        var calculated = includeBreakdown ? examSessionResultCalculator.calculate(session.getId()) : null;
        var targetBand = result.getTargetFrameworkBandId() == null
            ? null
            : frameworkResultBandRepository.findById(result.getTargetFrameworkBandId()).orElse(null);
        var rubricBand = result.getRubricResultBandId() == null
            ? null
            : rubricResultBandRepository.findById(result.getRubricResultBandId()).orElse(null);
        // Thang điểm đi kèm kết quả để client khỏi phải đoán -- xem javadoc ở
        // ExamCandidateResultResponse.scoringScaleMin để biết chuyện gì xảy ra khi nó đoán.
        var rubricVersion = !scoreVisible || result.getRubricVersionId() == null
            ? null
            : rubricVersionRepository.findById(result.getRubricVersionId()).orElse(null);

        return new ExamCandidateResultResponse(
            result.getId(),
            result.getSessionId(),
            result.getExamId(),
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
                .map(section -> new ExamCandidateResultSectionResponse(section.sectionId(), section.title(), section.score()))
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
     * <p>Hỏi ĐÚNG câu mà {@link ExamSessionResultCalculator} cần: "mọi câu đã có bản chấm chưa".
     * Bản trước hỏi "trạng thái có phải INVALID không" -- một phép thử gián tiếp, đúng tình cờ
     * vì bài chưa chấm thường đang ở INVALID.
     *
     * <p>Nó vỡ ngay khi bài chưa chấm mang trạng thái khác. Đo được 2026-08-17 trên phiên
     * 01a00e64: gỡ chặn đưa bài từ INVALID sang PENDING_REVIEW, điều kiện cũ cho qua, calculator
     * gặp câu không có evaluation và ném NotFoundException -- hỏng CẢ truy vấn
     * {@code examSessionResult}, nên trang Xem kết quả hiện "Không có dữ liệu trả lời" dù câu
     * trả lời còn nguyên trong DB.
     *
     * <p>Bài chưa chấm đủ thì trả về không có bảng điểm; trang vẫn hiện đầu bài, trạng thái và
     * cờ nghi vấn -- thiếu điểm là đúng, còn hỏng cả trang thì không.
     */
    private boolean shouldIncludeBreakdown(ExamCandidateResultStatus status, java.util.UUID sessionId) {
        if (status == ExamCandidateResultStatus.INVALID) {
            return false;
        }
        var responseIds = examItemResponseRepository.findBySessionId(sessionId).stream()
            .map(response -> response.getId())
            .toList();
        if (responseIds.isEmpty()) {
            return false;
        }
        var evaluatedResponseIds = examItemEvaluationRepository.findByResponseIdIn(responseIds).stream()
            .map(evaluation -> evaluation.getResponseId())
            .collect(java.util.stream.Collectors.toSet());
        return evaluatedResponseIds.containsAll(responseIds);
    }
}

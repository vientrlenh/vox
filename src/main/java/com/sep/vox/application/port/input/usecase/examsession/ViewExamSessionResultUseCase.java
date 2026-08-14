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

    public ViewExamSessionResultUseCase(
            ExamCandidateResultRepository examCandidateResultRepository,
            ExamSessionResultCalculator examSessionResultCalculator,
            FrameworkResultBandRepository frameworkResultBandRepository,
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            ExamResultAccessService examResultAccessService,
            QuestionRepository questionRepository) {
        this.examCandidateResultRepository = examCandidateResultRepository;
        this.examSessionResultCalculator = examSessionResultCalculator;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.examResultAccessService = examResultAccessService;
        this.questionRepository = questionRepository;
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
        return items.stream()
            .map(item -> new ExamCandidateResultItemResponse(
                item.paperItemId(),
                item.responseId(),
                item.sectionId(),
                item.questionId() == null ? null : texts.get(item.questionId()),
                item.itemScore(),
                item.weightedScore()
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
        var includeBreakdown = scoreVisible && shouldIncludeBreakdown(result.getStatus());
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
            scoreVisible && calculated != null ? itemResponses(calculated.items()) : java.util.List.of()
        );
    }

    private boolean shouldIncludeBreakdown(ExamCandidateResultStatus status) {
        return status != ExamCandidateResultStatus.INVALID;
    }
}

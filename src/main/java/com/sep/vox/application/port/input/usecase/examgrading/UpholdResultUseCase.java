package com.sep.vox.application.port.input.usecase.examgrading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.domain.model.exam.GradingOutcome;
import com.sep.vox.domain.model.exam.GradingRoundPolicy;
import com.sep.vox.domain.model.exam.GradingRoundType;
import com.sep.vox.application.port.input.service.MissingResponseBackfillService;
import com.sep.vox.application.port.input.usecase.examevaluation.UpsertExamCandidateResultUseCase;
import com.sep.vox.domain.repository.ExamItemEvaluationRepository;
import com.sep.vox.domain.repository.ExamItemResponseRepository;
import com.sep.vox.domain.repository.ExamSessionRepository;

/**
 * Giáo viên xác nhận điểm đang có là đúng — không nhập điểm mới.
 *
 * <p>Đây là hành động gộp của bản rework: "admin release bài chờ chấm" và "hậu kiểm
 * rồi giữ nguyên" hoá ra là cùng một quyết định (điểm hiện có đúng), chỉ khác vòng.
 * Vòng {@code INITIAL} thì bài chuyển sang RELEASED; vòng {@code SPOT_CHECK} thì bài
 * vốn đã RELEASED nên không đổi gì và <em>không</em> gửi mail.
 *
 * <p>Không tính lại điểm: không có item nào thay đổi, nên tổng vẫn là hàm của đúng
 * các item cũ. Bất biến {@code total = f(items)} được giữ mà không phải chạm gì.
 */
@Service
public class UpholdResultUseCase implements IUseCase<GradingDecisionCommand, GradingActionResponse> {

    private final GradingActionSupport gradingActionSupport;
    private final ExamSessionRepository examSessionRepository;
    private final ExamItemResponseRepository examItemResponseRepository;
    private final ExamItemEvaluationRepository examItemEvaluationRepository;
    private final MissingResponseBackfillService missingResponseBackfillService;
    private final UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase;

    public UpholdResultUseCase(
            GradingActionSupport gradingActionSupport,
            ExamSessionRepository examSessionRepository,
            ExamItemResponseRepository examItemResponseRepository,
            ExamItemEvaluationRepository examItemEvaluationRepository,
            MissingResponseBackfillService missingResponseBackfillService,
            UpsertExamCandidateResultUseCase upsertExamCandidateResultUseCase) {
        this.gradingActionSupport = gradingActionSupport;
        this.examSessionRepository = examSessionRepository;
        this.examItemResponseRepository = examItemResponseRepository;
        this.examItemEvaluationRepository = examItemEvaluationRepository;
        this.missingResponseBackfillService = missingResponseBackfillService;
        this.upsertExamCandidateResultUseCase = upsertExamCandidateResultUseCase;
    }

    @Override
    @Transactional
    public GradingActionResponse execute(GradingDecisionCommand command) {
        var prepared = gradingActionSupport.prepare(
            command.assignmentId(), GradingOutcome.UPHELD, command.reason());

        // Gỡ cờ nghi vấn: giáo viên đã xem và kết luận không vi phạm. Giữ nguyên
        // flagReason để còn tra lại vì sao bài từng bị đánh dấu.
        //
        // Ở REMEDIATION thì KHÔNG gỡ: giữ nguyên ở vòng đó nghĩa là xác nhận vi phạm,
        // gỡ cờ lúc này sẽ nói ngược lại chính quyết định vừa ghi.
        var session = prepared.context().session();
        if (prepared.roundType() != GradingRoundType.REMEDIATION && session.isFlagged()) {
            session.setFlagged(false);
            examSessionRepository.save(session);
        }

        var result = prepared.context().candidateResult();

        // "Giữ nguyên điểm" trên một câu CHƯA có bản chấm nào nghĩa là giữ nguyên con số rỗng.
        // Ghi thẳng 0 cho những câu đó rồi mới chốt.
        //
        // Đo được 2026-08-17 trên phiên 01a00e64: bài 2 câu, 0 evaluation, bấm Uphold ở vòng
        // INITIAL đẩy kết quả sang RELEASED với total_score = NULL -- bài đã CÔNG BỐ mà không
        // có điểm. Trang Xem kết quả thì vỡ luôn vì calculator gặp câu thiếu evaluation.
        //
        // Dùng lại recordSilentAnswer (0 điểm, không gọi LLM) thay vì thêm luật mới: đó đã là
        // cách hệ xử lý "không có nội dung để chấm" ở đường nộp bài.
        var responses = examItemResponseRepository.findBySessionId(result.getSessionId());
        var evaluatedResponseIds = examItemEvaluationRepository
            .findByResponseIdIn(responses.stream().map(response -> response.getId()).toList()).stream()
            .map(evaluation -> evaluation.getResponseId())
            .collect(java.util.stream.Collectors.toSet());
        var filledAny = false;
        for (var response : responses) {
            if (!evaluatedResponseIds.contains(response.getId())) {
                missingResponseBackfillService.recordSilentAnswer(
                    response.getId(), response.getPaperItemId());
                filledAny = true;
            }
        }

        // Chỉ tính lại khi thật sự vừa điền -- bài đã chấm đủ giữ nguyên đường cũ, không đụng
        // tới điểm đang có.
        var finalResult = result;
        if (filledAny) {
            var targetStatus = GradingRoundPolicy.resultStatusAfter(
                prepared.roundType(), GradingOutcome.UPHELD);
            finalResult = upsertExamCandidateResultUseCase.execute(
                result.getSessionId(),
                targetStatus == null ? result.getStatus() : targetStatus);
        }

        gradingActionSupport.finish(prepared, finalResult);

        return new GradingActionResponse(
            command.assignmentId(),
            finalResult.getId(),
            GradingOutcome.UPHELD.name(),
            finalResult.getStatus() == null ? null : finalResult.getStatus().name(),
            finalResult.getTotalScore(),
            null
        );
    }
}

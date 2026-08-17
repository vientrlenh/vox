package com.sep.vox.application.port.input.usecase.examgrading;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.GradingDecisionCommand;
import com.sep.vox.application.port.input.service.GradingActionSupport;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.examgrading.GradingActionResponse;
import com.sep.vox.domain.model.exam.ExamCandidateResultStatus;
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
        var targetStatus = GradingRoundPolicy.resultStatusAfter(
            prepared.roundType(), GradingOutcome.UPHELD);

        // Rào "phải chấm đủ" bên dưới CHỈ áp cho vòng nào mà giữ nguyên đồng nghĩa với CÔNG BỐ
        // (INITIAL, APPEAL -- xem ma trận ở GradingRoundPolicy). Đó là chỗ điểm thiếu lọt ra
        // tới học sinh, nên cũng là chỗ duy nhất đáng chặn.
        //
        // Vòng REMEDIATION đứng ngoài: bài ở đó đang INVALID và giữ nguyên INVALID (targetStatus
        // null), giữ nguyên nghĩa là XÁC NHẬN VI PHẠM chứ không phải giữ một con điểm. Bài bị
        // buộc kết thúc ngay trong lúc thi thì theo thiết kế không có evaluation nào
        // (SubmitExamSessionUseCase thoát sớm ở nhánh blockedAt), nên rào này bắt trúng CHÍNH
        // những bài mà vòng REMEDIATION sinh ra để xử lý: giáo viên không chấm được (không có
        // gì để chấm), không giữ nguyên được, phân công thành ngõ cụt không có lối đóng.
        // Hậu kiểm (SPOT_CHECK) cũng đứng ngoài vì bài vốn đã RELEASED -- chặn ở đó không thu
        // hồi lại được gì, chỉ khoá nốt người đang soi bài.
        var publishesResult = targetStatus == ExamCandidateResultStatus.RELEASED;

        var filledAny = publishesResult && fillSilentAnswersOrRefuse(result.getSessionId());

        // Chỉ tính lại khi thật sự vừa điền -- bài đã chấm đủ giữ nguyên đường cũ, không đụng
        // tới điểm đang có.
        var finalResult = result;
        if (filledAny) {
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

    /**
     * Lấp nốt các câu chưa có bản chấm trước khi bài được công bố, hoặc từ chối nếu không lấp
     * được. CHỈ gọi ở vòng mà giữ nguyên = công bố.
     *
     * <p>"Giữ nguyên điểm" trên một câu CHƯA có bản chấm nào nghĩa là giữ nguyên con số rỗng.
     * Câu thí sinh không nói gì thì ghi thẳng 0 rồi mới chốt; câu có nói thì chặn.
     *
     * <p>Đo được 2026-08-17 trên phiên 01a00e64: bài 2 câu, 0 evaluation, bấm Uphold ở vòng
     * INITIAL đẩy kết quả sang RELEASED với {@code total_score = NULL} -- bài đã CÔNG BỐ mà
     * không có điểm. Trang Xem kết quả thì vỡ luôn vì calculator gặp câu thiếu evaluation.
     *
     * <p>Dùng lại {@code recordSilentAnswer} (0 điểm, không gọi LLM) thay vì thêm luật mới: đó
     * đã là cách hệ xử lý "không có nội dung để chấm" ở đường nộp bài.
     *
     * @return true nếu vừa ghi thêm bản chấm 0 -- tức tổng điểm cần được tính lại
     * @throws IllegalStateException khi còn câu thí sinh CÓ trả lời mà chưa ai chấm
     */
    private boolean fillSilentAnswersOrRefuse(UUID sessionId) {
        var responses = examItemResponseRepository.findBySessionId(sessionId);
        if (responses.isEmpty()) {
            return false;
        }
        var evaluatedResponseIds = examItemEvaluationRepository
            .findByResponseIdIn(responses.stream().map(response -> response.getId()).toList()).stream()
            .map(evaluation -> evaluation.getResponseId())
            .collect(Collectors.toSet());
        var ungraded = responses.stream()
            .filter(response -> !evaluatedResponseIds.contains(response.getId()))
            .toList();

        // Chỉ câu THẬT SỰ rỗng mới được cho 0. Câu thí sinh có nói mà chưa ai chấm thì không
        // "giữ nguyên" được -- không có con điểm nào ở đó để mà giữ.
        //
        // Bản đầu của nhánh này cho 0 tất cả câu chưa có bản chấm, không hỏi câu đó có nội dung
        // hay không. Đo được 2026-08-17 trên phiên 01a0101d: một câu đã nói 2 lượt (41 giây +
        // 36 giây, đủ audio lẫn transcript) bị ghi 0 điểm kèm "Thí sinh không đưa ra câu trả
        // lời nào cho câu hỏi này" -- hệ thống phát ngôn một điều sai về học sinh.
        //
        // Chặn thay vì đoán: ba lối ra đều tệ hơn. Cho 0 là chấm oan; bỏ qua rồi chốt là công bố
        // bài thiếu điểm (đúng lỗi mà nhánh này sinh ra để vá); gọi AI chấm hộ thì mâu thuẫn với
        // quyết định "gỡ chặn không cho AI chấm lại". Việc còn thiếu ở đây là việc của người
        // chấm, nên nói thẳng ra là còn thiếu.
        var answeredButUngraded = ungraded.stream()
            .filter(response -> !missingResponseBackfillService.isSilentAnswer(response))
            .count();
        if (answeredButUngraded > 0) {
            throw new IllegalStateException(
                "Còn " + answeredButUngraded + " câu thí sinh có trả lời nhưng chưa được chấm. "
                    + "Hãy chấm những câu đó trước khi giữ nguyên điểm."
            );
        }

        var filledAny = false;
        for (var response : ungraded) {
            filledAny |= missingResponseBackfillService.recordSilentAnswer(
                response.getId(), response.getPaperItemId());
        }
        return filledAny;
    }
}

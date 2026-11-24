package com.sep.vox.interfaces.rest.controller.internal;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.usecase.practicesession.GetPracticeTurnUploadUrlUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ResolveNextPracticeQuestionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.SubmitPracticeTurnUseCase;
import com.sep.vox.application.response.input.assessmentturn.TurnUploadUrlResponse;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.SubmitTurnResult;
import com.sep.vox.domain.model.personalization.SubmitPracticeTurn;
import com.sep.vox.domain.model.personalization.TurnCorrectionSubmission;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

/**
 * Internal Python -> Java endpoints for the realtime practice session (package 11, section 2.4,
 * step 4) -- PracticeInternalSecretFilter protects the whole /internal/practice-sessions/** path,
 * which does not go through the user JWT because the caller is Python, not a logged-in student.
 */
@RestController
@RequestMapping("/internal/practice-sessions")
public class RestPracticeSessionInternalController {

    private final ResolveNextPracticeQuestionUseCase resolveNextPracticeQuestionUseCase;
    private final SubmitPracticeTurnUseCase submitPracticeTurnUseCase;
    private final GetPracticeTurnUploadUrlUseCase getPracticeTurnUploadUrlUseCase;
    private final PracticeSessionRepository practiceSessionRepository;
    private final AsyncTaskExecutor practiceGenerationExecutor;

    public RestPracticeSessionInternalController(
            ResolveNextPracticeQuestionUseCase resolveNextPracticeQuestionUseCase,
            SubmitPracticeTurnUseCase submitPracticeTurnUseCase,
            GetPracticeTurnUploadUrlUseCase getPracticeTurnUploadUrlUseCase,
            PracticeSessionRepository practiceSessionRepository,
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor practiceGenerationExecutor) {
        this.resolveNextPracticeQuestionUseCase = resolveNextPracticeQuestionUseCase;
        this.submitPracticeTurnUseCase = submitPracticeTurnUseCase;
        this.getPracticeTurnUploadUrlUseCase = getPracticeTurnUploadUrlUseCase;
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceGenerationExecutor = practiceGenerationExecutor;
    }

    
    private static final long NEXT_QUESTION_TIMEOUT_SECONDS = 90;

    @PostMapping("/{sessionId}/next-question")
    public WebAsyncTask<ApiResponse<ResolveNextPracticeQuestionUseCase.Result>> nextQuestion(
            @PathVariable UUID sessionId) {
        return new WebAsyncTask<>(
            Duration.ofSeconds(NEXT_QUESTION_TIMEOUT_SECONDS).toMillis(),
            practiceGenerationExecutor,
            () -> ApiResponse.success("OK", resolveNextPracticeQuestionUseCase.execute(sessionId))
        );
    }

    /**
     * Nhịp tim của phiên luyện -- agents gọi định kỳ trong suốt lúc WebSocket realtime còn mở.
     *
     * <p>Vì sao để AGENTS gửi chứ không phải app: app đã giữ WS tới agents và bơm audio liên
     * tục, nên agents biết từng giây phiên còn sống hay không. Timer trong app thì vẫn chạy dù
     * mạng đã rớt, và Android bóp timer khi app xuống nền -- cả hai đều cho tín hiệu sai.
     *
     * <p>Trả 200 kể cả khi phiên đã đóng: đây là tín hiệu sống, không phải lệnh nghiệp vụ. Agents
     * không có việc gì để làm với lỗi ở đây, và ném lỗi chỉ tổ đẻ ra log nhiễu lúc phiên vừa kết
     * thúc bình thường mà nhịp cuối còn đang bay.
     */
    @PostMapping("/{sessionId}/heartbeat")
    public ApiResponse<Void> heartbeat(@PathVariable UUID sessionId) {
        practiceSessionRepository.touchHeartbeat(sessionId);
        return ApiResponse.success("OK", null);
    }

    @GetMapping("/{sessionId}/turns/{turnOrder}/upload-url")
    public ApiResponse<TurnUploadUrlResponse> turnUploadUrl(
            @PathVariable UUID sessionId,
            @PathVariable int turnOrder) {
        return ApiResponse.success("OK", getPracticeTurnUploadUrlUseCase.execute(sessionId, turnOrder));
    }

    @PostMapping("/{sessionId}/turns")
    public ApiResponse<SubmitTurnResult> submitTurn(
            @PathVariable UUID sessionId,
            @RequestBody TurnRequest request) {
        UUID studentId = practiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên luyện."))
            .getStudentId();
        SubmitPracticeTurn turn = SubmitPracticeTurn.builder()
                .sessionId(sessionId)
                .questionId(request.questionId())
                .turnOrder(request.turnOrder())
                .turnType(request.turnType())
                .promptText(request.promptText())
                .audioUrl(request.audioUrl())
                .transcript(request.transcript())
                .durationSeconds(request.durationSeconds())
                .turnCostUsd(request.turnCostUsd())
                .wordFeedbackJson(request.wordFeedbackJson())
                .turnScore(request.turnScore())
                .questionComplete(request.questionComplete())
                .corrections(request.corrections() == null
                ? List.of()
                : request.corrections().stream().map(correction -> correction.toDomain()).toList())
                .build();
        return ApiResponse.success("OK", submitPracticeTurnUseCase.execute(studentId, turn));
    }

    public record TurnRequest(
        UUID questionId,
        int turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        int durationSeconds,
        // Chi phí AI thật (USD) của turn này, Python tính đồng bộ từ realtimeCorrectionGraph
        // ngay trong request này (xem PracticeAttemptConnection._flush_turn_usage bên
        // Agentic AI) -- nullable phòng client Python cũ chưa deploy kịp, SubmitPracticeTurnUseCase
        // fallback về ZERO. Xem AI_USAGE_QUOTA_USD_MIGRATION.md.
        BigDecimal turnCostUsd,
        String wordFeedbackJson,
        Double turnScore,
        boolean questionComplete,
        List<TurnCorrectionRequest> corrections) {
    }

    public record TurnCorrectionRequest(
        String category,
        String originalText,
        String correctedText,
        String explanation,
        String correctAudioUrl,
        double confidence) {

        TurnCorrectionSubmission toDomain() {
            return TurnCorrectionSubmission.builder()
                    .category(category)
                    .originalText(originalText)
                    .correctedText(correctedText)
                    .explanation(explanation)
                    .correctAudioUrl(correctAudioUrl)
                    .confidence(confidence)
                    .build();
        }
    }
}

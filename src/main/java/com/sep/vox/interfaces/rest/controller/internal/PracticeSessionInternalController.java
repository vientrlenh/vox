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

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.usecase.practicesession.GetPracticeTurnUploadUrlUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.ResolveNextPracticeQuestionUseCase;
import com.sep.vox.application.port.input.usecase.practicesession.SubmitPracticeTurnUseCase;
import com.sep.vox.application.response.input.examturn.TurnUploadUrlResponse;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.SubmitTurnResult;
import com.sep.vox.domain.model.personalization.SubmitPracticeTurn;
import com.sep.vox.domain.model.personalization.TurnCorrectionSubmission;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.interfaces.rest.dto.response.ApiResponse;

/**
 * Endpoint nội bộ Python -> Java cho phiên luyện realtime (gói 11 mục 2.4 bước 4) --
 * PracticeInternalSecretFilter bảo vệ toàn bộ path /internal/practice-sessions/**, không đi qua
 * JWT người dùng vì bên gọi là Python, không phải học sinh đăng nhập.
 */
@RestController
@RequestMapping("/internal/practice-sessions")
public class PracticeSessionInternalController {

    private final ResolveNextPracticeQuestionUseCase resolveNextPracticeQuestionUseCase;
    private final SubmitPracticeTurnUseCase submitPracticeTurnUseCase;
    private final GetPracticeTurnUploadUrlUseCase getPracticeTurnUploadUrlUseCase;
    private final PracticeSessionRepository practiceSessionRepository;
    private final AsyncTaskExecutor practiceGenerationExecutor;

    public PracticeSessionInternalController(
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
        var studentId = practiceSessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."))
            .getStudentId();
        var turn = new SubmitPracticeTurn(
            sessionId,
            request.questionId(),
            request.turnOrder(),
            request.turnType(),
            request.promptText(),
            request.audioUrl(),
            request.transcript(),
            request.durationSeconds(),
            request.turnCostUsd(),
            request.wordFeedbackJson(),
            request.turnScore(),
            request.questionComplete(),
            request.corrections() == null
                ? List.of()
                : request.corrections().stream().map(correction -> correction.toDomain()).toList()
        );
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
            return new TurnCorrectionSubmission(
                category, originalText, correctedText, explanation, correctAudioUrl, confidence
            );
        }
    }
}

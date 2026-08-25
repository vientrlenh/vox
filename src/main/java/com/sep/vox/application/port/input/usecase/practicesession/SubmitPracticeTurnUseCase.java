package com.sep.vox.application.port.input.usecase.practicesession;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.command.SubmitPracticeTurnCommand;
import com.sep.vox.application.port.input.service.PracticeEvaluationRequestFactory;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ConsumeQuotaUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.SubmitTurnResult;
import com.sep.vox.domain.dto.personalization.SubmitTurnResultDto;
import com.sep.vox.domain.dto.personalization.TurnCorrectionDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.personalization.SubmitPracticeTurn;
import com.sep.vox.domain.model.personalization.TurnCorrectionSubmission;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.TurnCorrectionRepository;

@Service
public class SubmitPracticeTurnUseCase implements IUseCase<SubmitPracticeTurnCommand, SubmitTurnResult> {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(SubmitPracticeTurnUseCase.class);

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final TurnCorrectionRepository turnCorrectionRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PracticeEvaluationRequestFactory evaluationRequestFactory;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final ConsumeQuotaUseCase consumeQuotaUseCase;
    private final ExternalEventPublisherPort eventPublisher;
    private final UserContextPort userContextPort;
    // REQUIRES_NEW riêng cho lời gọi ConsumeQuotaUseCase -- xem lý do đầy đủ tại chỗ dùng bên dưới.
    private final TransactionTemplate consumeQuotaTransactionTemplate;

    public SubmitPracticeTurnUseCase(
            PracticeSessionRepository practiceSessionRepository,
            PracticeItemResponseRepository practiceItemResponseRepository,
            PracticeResponseTurnRepository practiceResponseTurnRepository,
            TurnCorrectionRepository turnCorrectionRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PracticeEvaluationRequestFactory evaluationRequestFactory,
            PracticeTopicOfferEnrichmentService enrichmentService,
            ConsumeQuotaUseCase consumeQuotaUseCase,
            ExternalEventPublisherPort eventPublisher,
            UserContextPort userContextPort,
            PlatformTransactionManager transactionManager) {
        this.practiceSessionRepository = practiceSessionRepository;
        this.practiceItemResponseRepository = practiceItemResponseRepository;
        this.practiceResponseTurnRepository = practiceResponseTurnRepository;
        this.turnCorrectionRepository = turnCorrectionRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.evaluationRequestFactory = evaluationRequestFactory;
        this.enrichmentService = enrichmentService;
        this.consumeQuotaUseCase = consumeQuotaUseCase;
        this.eventPublisher = eventPublisher;
        this.userContextPort = userContextPort;
        this.consumeQuotaTransactionTemplate = new TransactionTemplate(transactionManager);
        this.consumeQuotaTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional
    public SubmitTurnResult execute(SubmitPracticeTurnCommand input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return execute(studentId, input.turn());
    }

    /**
     * Cho phép gọi với studentId biết trước (không qua UserContextPort) -- dùng bởi endpoint nội
     * bộ /internal/practice-sessions/{id}/turns (Python gọi, không có SecurityContext đăng nhập).
     */
    @Transactional
    public SubmitTurnResult execute(UUID studentId, SubmitPracticeTurn turn) {
        requireOwnedInProgress(turn.getSessionId(), studentId);
        var responseId = practiceItemResponseRepository.upsertResponse(
            turn.getSessionId(),
            turn.getQuestionId(),
            turn.getAudioUrl(),
            turn.getTranscript(),
            turn.isQuestionComplete()
        );
        var turnId = practiceResponseTurnRepository.save(
            responseId,
            turn.getTurnOrder(),
            turn.getTurnType(),
            turn.getPromptText(),
            turn.getAudioUrl(),
            turn.getTranscript(),
            Math.max(0, turn.getDurationSeconds()),
            turn.getWordFeedbackJson(),
            turn.getTurnScore()
        );
        var corrections = storeCorrections(turnId, turn.getCorrections());
        // Nạp phiên KỂ CẢ khi lượt này im lặng: client vẫn phải thấy đúng tổng "đã nói" trên
        // thanh tiến độ, chứ không phải một ô trống mỗi lần học sinh bấm mic rồi không nói gì.
        var session = practiceSessionRepository.findById(turn.getSessionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        var spokenSeconds = session.getGradedSeconds();
        var quotaExhausted = false;
        if (turn.getDurationSeconds() > 0) {
            try {
                // Chi phí AI thật (USD) của turn này, Python tính đồng bộ từ realtimeCorrectionGraph
                // ngay trong request submit_turn (không đợi được ai_usage_record qua Kafka như exam,
                // vì PRACTICE trừ quota NGAY trong request này) -- xem
                // PracticeAttemptConnection._flush_turn_usage bên Agentic AI,
                // AI_USAGE_QUOTA_USD_MIGRATION.md mục 5/7. Fallback ZERO cho client Python cũ chưa gửi.
                var costUsd = turn.getTurnCostUsd() != null ? turn.getTurnCostUsd() : BigDecimal.ZERO;
                var subscriptionId = activeSubscriptionId(studentId);
                // ConsumeQuotaUseCase cũng @Transactional (REQUIRED mặc định) -- gọi thẳng thì nó
                // tham gia CHUNG transaction với response/turn/corrections vừa lưu ở trên. Khi nó
                // ném QuotaExceededException, transaction interceptor của lời gọi đó đánh dấu
                // rollback-only lên transaction DÙNG CHUNG ngay lập tức, TRƯỚC khi exception bay
                // tới catch bên dưới -- bắt được và chạy tiếp bình thường không cứu được gì, tới
                // lúc method này commit Spring vẫn ném UnexpectedRollbackException (500), cuốn theo
                // cả response/turn/corrections đã lưu. Bọc lời gọi trong transaction RIÊNG
                // (REQUIRES_NEW) thì lúc nó throw chỉ transaction cô lập đó bị rollback -- transaction
                // ngoài (của method này) không hề bị đụng tới, vẫn commit sạch ở cuối như bình
                // thường. Xem QuestionStatusTransition/PracticeSessionClosedHandler cho các chỗ khác
                // trong repo đã từng dính đúng cái bẫy UnexpectedRollbackException này.
                consumeQuotaTransactionTemplate.execute(status -> consumeQuotaUseCase.execute(new ConsumeQuotaCommand(
                    subscriptionId,
                    turn.getSessionId(),
                    QuotaType.PRACTICE,
                    costUsd,
                    studentId
                )));
            } catch (QuotaExceededException exception) {
                // Đã nói thì phải được ghi. Hết hạn mức chỉ có nghĩa là phiên dừng SAU lượt
                // này, nên báo bằng cờ để tầng gọi đóng phiên một cách tử tế.
                LOGGER.info(
                    "Hết hạn mức PRACTICE ở phiên {} -- vẫn ghi lượt {} rồi đóng phiên.",
                    turn.getSessionId(),
                    turn.getTurnOrder()
                );
                quotaExhausted = true;
            }
            spokenSeconds += turn.getDurationSeconds();
            practiceSessionRepository.save(
                session.withGradedSecondsAndHeartbeat(spokenSeconds, Instant.now())
            );
        }
        var result = new SubmitTurnResultDto(
            responseId,
            turnId,
            practiceResponseTurnRepository.findRemainingQuestionSeconds(turn.getSessionId(), turn.getQuestionId()),
            turn.isQuestionComplete(),
            corrections,
            quotaExhausted,
            spokenSeconds,
            enrichmentService.sessionBudgetSeconds(
                studentId, enrichmentService.bandOrder(session.getTargetFrameworkBandId())
            )
        );
        if (result.evaluationQueued()) {
            eventPublisher.publish(evaluationRequestFactory.build(
                turn.getSessionId(),
                result.responseId(),
                turn.getQuestionId()
            ));
            // Đóng dấu ở CẢ đường này, không riêng đường xả chấm: cột grading_requested_at phải
            // đúng nghĩa "đã hỏi chấm lúc nào" cho mọi câu, nếu không nó nói dối về đa số dữ
            // liệu -- phần lớn câu đi qua đây chứ không qua flush.
            practiceItemResponseRepository.markGradingRequested(result.responseId(), Instant.now());
        }
        return PracticeSessionResponseMapper.toResponse(result);
    }

    private void requireOwnedInProgress(UUID sessionId, UUID studentId) {
        if (!practiceSessionRepository.existsByIdAndStudentIdAndStatus(sessionId, studentId, "IN_PROGRESS")) {
            throw new NotFoundException("Phiên luyện không còn hoạt động.");
        }
    }

    private UUID activeSubscriptionId(UUID studentId) {
        return schoolSubscriptionRepository.findActiveSubscriptionIdForUser(studentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đang hoạt động."));
    }

    /**
     * Ngưỡng tin cậy chỉ áp cho những dòng sửa do LLM PHÁN ĐOÁN (ngữ pháp, dùng từ) -- ở đó
     * điểm tin cậy nói lên model chắc tới đâu. Dòng phát âm thì khác hẳn: nó là SỐ ĐO của
     * Azure (điểm chính xác từng âm vị), không tự xưng độ tin cậy nào cả nên luôn về 0.0 và bị
     * ngưỡng này loại sạch. Kết quả là thẻ sửa lỗi lúc đang nói thì hiện lỗi phát âm (đi thẳng
     * qua WebSocket), nhưng mở lại sau buổi thì chúng biến mất -- chưa từng được ghi xuống.
     */
    private static final String MEASURED_CATEGORY = "pronunciation";
    private static final double MIN_JUDGED_CONFIDENCE = 0.8;

    /**
     * Trần số dòng lưu cho MỘT lượt. Rộng hơn con số 3 cũ vì trần này chỉ chi phối bản LƯU
     * (để xem lại sau buổi), không chi phối thẻ hiện lúc đang nói -- thẻ đó do Python đẩy
     * thẳng qua WebSocket, không đi qua đây. Xem lại thì càng đủ càng tốt; giữ trần chỉ để
     * chặn ghi vô hạn.
     */
    private static final int MAX_STORED_CORRECTIONS = 8;

    private List<TurnCorrectionDto> storeCorrections(UUID turnId, List<TurnCorrectionSubmission> inputs) {
        var result = new ArrayList<TurnCorrectionDto>();
        for (var correction : inputs == null ? List.<TurnCorrectionSubmission>of() : inputs) {
            var measured = MEASURED_CATEGORY.equalsIgnoreCase(correction.getCategory());
            if (result.size() >= MAX_STORED_CORRECTIONS
                    || (!measured && correction.getConfidence() < MIN_JUDGED_CONFIDENCE)) {
                continue;
            }
            turnCorrectionRepository.save(
                turnId,
                correction.getCategory(),
                correction.getOriginalText(),
                correction.getCorrectedText(),
                correction.getExplanation(),
                correction.getCorrectAudioUrl()
            );
            result.add(new TurnCorrectionDto(
                correction.getCategory(),
                correction.getOriginalText(),
                correction.getCorrectedText(),
                correction.getExplanation(),
                correction.getCorrectAudioUrl()
            ));
        }
        return result;
    }

}

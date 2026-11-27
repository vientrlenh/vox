package com.sep.vox.application.port.input.usecase.practicesession;

import com.sep.vox.application.response.input.subscription.ConsumeQuotaResponse;
import com.sep.vox.domain.model.personalization.PracticeSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.practicesession.PracticeSessionResponseMapper;
import com.sep.vox.application.port.input.command.practicesession.SubmitPracticeTurnCommand;
import com.sep.vox.application.port.input.service.ConsumeQuotaService;
import com.sep.vox.application.port.input.service.PracticeEvaluationRequestFactory;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ExternalEventPublisherPort;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.practicesession.PracticeSessionResponses.SubmitTurnResult;
import com.sep.vox.domain.dto.personalization.SubmitTurnResultDto;
import com.sep.vox.domain.dto.personalization.TurnCorrectionDto;
import com.sep.vox.domain.model.personalization.SubmitPracticeTurn;
import com.sep.vox.domain.model.personalization.TurnCorrectionSubmission;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticeResponseTurnRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.TurnCorrectionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitPracticeTurnUseCase implements IUseCase<SubmitPracticeTurnCommand, SubmitTurnResult> {

    // Same scale as school_subscription_quota_records.used_amount_vnd numeric(18,6).
    private static final int COST_VND_SCALE = 6;

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final PracticeResponseTurnRepository practiceResponseTurnRepository;
    private final TurnCorrectionRepository turnCorrectionRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PracticeEvaluationRequestFactory evaluationRequestFactory;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final ConsumeQuotaService consumeQuotaService;
    private final ExternalEventPublisherPort eventPublisher;
    private final UserContextPort userContextPort;
    private final QuotaPricingPort quotaPricingPort;
    // Separate REQUIRES_NEW transaction for the ConsumeQuotaService call -- see the full reason where it is used below.
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    // CẢNH BÁO: lời gọi execute(...) bên dưới là SELF-INVOCATION -- không đi qua proxy, nên
    // @Transactional trên overload kia KHÔNG có tác dụng ở đường này; transaction đang chạy là
    // transaction của CHÍNH method này. Hiện vô hại vì hai annotation cùng cấu hình, nhưng đổi
    // overload kia sang REQUIRES_NEW/readOnly thì thay đổi đó chỉ có hiệu lực ở đường gọi trực tiếp
    // (/internal/practice-sessions/{id}/turns) và im lặng không xảy ra gì ở đây.
    @Override
    @Transactional
    public SubmitTurnResult execute(SubmitPracticeTurnCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        return execute(studentId, input.turn());
    }

    /**
     * Allows a call with a known studentId (not through UserContextPort) -- used by the internal endpoint
     * /internal/practice-sessions/{id}/turns (called by Python, which has no logged-in SecurityContext).
     */
    @Transactional
    public SubmitTurnResult execute(UUID studentId, SubmitPracticeTurn turn) {
        requireOwnedInProgress(turn.getSessionId(), studentId);
        UUID responseId = upsertResponse(turn);
        PracticeResponseTurnRepository.TurnWrite turnWrite = saveTurn(responseId, turn);
        UUID turnId = turnWrite.turnId();
        // A RESUBMITTED turn does NOT write extra correction rows: turn_corrections has no unique
        // constraint, so every Python retry would double the correction cards the student sees when
        // reviewing the session. The list is still built for the response -- the retry payload is the
        // first payload, so filtering it again yields exactly the rows already stored.
        List<TurnCorrectionDto> corrections = collectCorrections(turnId, turn.getCorrections(), turnWrite.created());
        // Load the session EVEN when this turn is silent: the client must still see the correct total
        // "spoken" on the progress bar, not an empty box each time the student taps the mic and says nothing.
        PracticeSession session = practiceSessionRepository.findById(turn.getSessionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên luyện."));
        SpokenTurnOutcome outcome = recordSpokenSeconds(studentId, turn, session, turnWrite.created());
        SubmitTurnResultDto result = buildTurnResult(studentId, turn, session, responseId, turnId, corrections, outcome);
        if (turnWrite.created() && result.evaluationQueued()) {
            requestGrading(turn, result);
        }
        return PracticeSessionResponseMapper.toResponse(result);
    }

    private UUID upsertResponse(SubmitPracticeTurn turn) {
        return practiceItemResponseRepository.upsertResponse(
            turn.getSessionId(),
            turn.getQuestionId(),
            turn.getAudioUrl(),
            turn.getTranscript(),
            turn.isQuestionComplete()
        );
    }

    private PracticeResponseTurnRepository.TurnWrite saveTurn(UUID responseId, SubmitPracticeTurn turn) {
        return practiceResponseTurnRepository.save(
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
    }

    private record SpokenTurnOutcome(boolean quotaExhausted, int spokenSeconds) {
    }

    /**
     * Deducts the cost of a spoken turn and adds its seconds to the session, only for the FIRST submission.
     *
     * <p>{@code firstSubmission} (turnWrite.created()) guards the WHOLE block: the turns table de-duplicates
     * itself (uq_practice_response_turn_order), but the two things done here do not -- charging the school
     * wallet, and adding the spoken seconds to the session. Before this flag existed, a Python retry after a
     * lost HTTP response would charge REAL money a second time for the same turn, and the ledger still
     * balanced (entry and balance were both deducted twice) so the invariant SUM(entries) = balance did not
     * catch it. The very table being protected is what made this spot look safe.
     *
     * <p>Skipping the charge also means not knowing whether the funds ran out, so it reports false: the
     * session will close on the next REAL turn. This is still within the lag the design already accepted --
     * the session closes AFTER the turn that exhausts the funds, not before.
     */
    private SpokenTurnOutcome recordSpokenSeconds(UUID studentId, SubmitPracticeTurn turn,
                                                  PracticeSession session, boolean firstSubmission) {
        int spokenSeconds = session.getGradedSeconds();
        if (!firstSubmission || turn.getDurationSeconds() <= 0) {
            return new SpokenTurnOutcome(false, spokenSeconds);
        }
        boolean quotaExhausted = deductTurnCost(studentId, turn);
        spokenSeconds += turn.getDurationSeconds();
        practiceSessionRepository.save(
            session.withGradedSecondsAndHeartbeat(spokenSeconds, Instant.now())
        );
        return new SpokenTurnOutcome(quotaExhausted, spokenSeconds);
    }

    /**
     * Converts the turn cost to VND RIGHT here because quota is counted in VND. Unlike the assessment path,
     * where the already-fixed cost_vnd of each ai_usage_records row is summed, here the cost is sent by
     * Python straight in the request so there is no row to sum yet -- the current exchange rate is the
     * right one, since the expense has just been incurred.
     *
     * @return whether the funds are exhausted after this deduction
     */
    private boolean deductTurnCost(UUID studentId, SubmitPracticeTurn turn) {
        BigDecimal costUsd = turnCostUsd(turn);
        BigDecimal fxRateUsed = quotaPricingPort.usdToVndRate();
        BigDecimal costVnd = costUsd
            .multiply(fxRateUsed)
            .setScale(COST_VND_SCALE, RoundingMode.HALF_UP);
        UUID subscriptionId = activeSubscriptionId(studentId);
        ConsumeQuotaResponse consumeResult = consumeInNewTransaction(
            studentId, turn.getSessionId(), subscriptionId, costVnd, costUsd, fxRateUsed);
        boolean quotaExhausted = consumeResult != null && consumeResult.fundsExhausted();
        if (quotaExhausted) {
            // What was spoken must be recorded. Running out of money only means the session stops AFTER this turn.
            log.info(
                "Hết hạn mức lẫn số dư PRACTICE ở phiên {} -- vẫn ghi lượt {} rồi đóng phiên.",
                turn.getSessionId(),
                turn.getTurnOrder()
            );
        }
        return quotaExhausted;
    }

    /**
     * The real AI cost (USD) of this turn, computed by Python synchronously from the realtimeCorrectionGraph
     * right inside the submit_turn request (it cannot wait for ai_usage_record via Kafka like assessments,
     * because PRACTICE deducts quota RIGHT in this request) -- see
     * PracticeAttemptConnection._flush_turn_usage on the Agentic AI side,
     * AI_USAGE_QUOTA_USD_MIGRATION.md sections 5/7. Falls back to ZERO for an old Python client that does not send it yet.
     */
    private BigDecimal turnCostUsd(SubmitPracticeTurn turn) {
        return turn.getTurnCostUsd() != null ? turn.getTurnCostUsd() : BigDecimal.ZERO;
    }

    /**
     * ConsumeQuotaService is also @Transactional (REQUIRED by default) -- called directly, it
     * would join the SAME transaction as the response/turn/corrections saved earlier. If it
     * throws (now only real failures such as quota/plan not found, no longer
     * BusinessRuleViolationException for an exhausted quota), the transaction interceptor of that call marks the SHARED
     * transaction rollback-only immediately, BEFORE the exception reaches the place that
     * catches it -- by the time the caller commits, Spring still throws
     * UnexpectedRollbackException (500), dragging along the response/turn/corrections
     * already saved. Wrapping it in its OWN transaction (REQUIRES_NEW) means only that
     * isolated transaction is rolled back. See
     * QuestionStatusTransition/PracticeSessionClosedHandler for other places in the repo
     * that have fallen into exactly this trap.
     *
     * <p>consumePracticeAllowingDebt and NOT a hard block: by the time we get here Azure has
     * already billed (costUsd is the REAL cost of the turn just spoken), so refusing
     * would not keep a single cent and would only make the expense vanish from the books.
     * Whether funds are exhausted is now read from the fundsExhausted flag of the
     * result, no longer through an exception.
     */
    private ConsumeQuotaResponse consumeInNewTransaction(UUID studentId, UUID sessionId, UUID subscriptionId,
                                                         BigDecimal costVnd, BigDecimal costUsd,
                                                         BigDecimal fxRateUsed) {
        return requiresNewTransactionPort.callInNewTransaction(
            () -> consumeQuotaService.consumePracticeAllowingDebt(
                subscriptionId,
                sessionId,
                costVnd,
                costUsd,
                fxRateUsed,
                studentId
            ));
    }

    private SubmitTurnResultDto buildTurnResult(UUID studentId, SubmitPracticeTurn turn, PracticeSession session,
                                                UUID responseId, UUID turnId, List<TurnCorrectionDto> corrections,
                                                SpokenTurnOutcome outcome) {
        return new SubmitTurnResultDto(
            responseId,
            turnId,
            practiceResponseTurnRepository.findRemainingQuestionSeconds(turn.getSessionId(), turn.getQuestionId()),
            turn.isQuestionComplete(),
            corrections,
            outcome.quotaExhausted(),
            outcome.spokenSeconds(),
            enrichmentService.sessionBudgetSeconds(
                studentId, enrichmentService.bandOrder(session.getTargetFrameworkBandId())
            )
        );
    }

    /**
     * Also guarded by created() at the call site: the first submission already pushed the grading request
     * (ExternalEventPublisherPort fires straight to Kafka, not through the outbox), so firing again would be
     * a second AI grading run for one answer -- real money again, just like the quota deduction skipped above.
     * The flag returned to the client stays as it is: this answer HAS been queued for grading, just from an
     * earlier submission.
     */
    private void requestGrading(SubmitPracticeTurn turn, SubmitTurnResultDto result) {
        eventPublisher.publish(evaluationRequestFactory.build(
            turn.getSessionId(),
            result.responseId(),
            turn.getQuestionId()
        ));
        // Stamped on THIS path too, not only on the flush path: the grading_requested_at column must really
        // mean "when grading was requested" for every answer, otherwise it lies about most of the data --
        // most answers go through here and not through flush.
        practiceItemResponseRepository.markGradingRequested(result.responseId(), Instant.now());
    }

    private void requireOwnedInProgress(UUID sessionId, UUID studentId) {
        if (!practiceSessionRepository.existsByIdAndStudentIdAndStatus(sessionId, studentId, "IN_PROGRESS")) {
            throw new ResourceNotFoundException("Phiên luyện không còn hoạt động.");
        }
    }

    private UUID activeSubscriptionId(UUID studentId) {
        return schoolSubscriptionRepository.findActiveSubscriptionIdForUser(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gói đang hoạt động."));
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

    /**
     * Filters, then (depending on {@code persist}) writes the correction rows of one turn.
     *
     * <p>Filtering and writing are DELIBERATELY in one loop: the list returned to the client must be exactly
     * the rows that were stored, so splitting them would set up two criteria that drift apart.
     * {@code persist=false} (a resubmission) goes through the same filter but skips the save call, so the
     * client receives exactly what it got the first time.
     */
    private List<TurnCorrectionDto> collectCorrections(UUID turnId,
            List<TurnCorrectionSubmission> inputs, boolean persist) {
        List<TurnCorrectionDto> result = new ArrayList<TurnCorrectionDto>();
        for (TurnCorrectionSubmission correction : inputs == null ? List.<TurnCorrectionSubmission>of() : inputs) {
            if (!isWorthStoring(correction, result.size())) {
                continue;
            }
            if (persist) {
                saveCorrection(turnId, correction);
            }
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

    private boolean isWorthStoring(TurnCorrectionSubmission correction, int alreadyKept) {
        boolean measured = MEASURED_CATEGORY.equalsIgnoreCase(correction.getCategory());
        return alreadyKept < MAX_STORED_CORRECTIONS
            && (measured || correction.getConfidence() >= MIN_JUDGED_CONFIDENCE);
    }

    private void saveCorrection(UUID turnId, TurnCorrectionSubmission correction) {
        turnCorrectionRepository.save(
            turnId,
            correction.getCategory(),
            correction.getOriginalText(),
            correction.getCorrectedText(),
            correction.getExplanation(),
            correction.getCorrectAudioUrl()
        );
    }

}

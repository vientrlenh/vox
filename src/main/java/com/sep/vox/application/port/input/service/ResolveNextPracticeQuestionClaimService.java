package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.personalization.PracticeSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.PracticePaperItemRepository;
import com.sep.vox.domain.repository.PracticeQuestionRepository;
import com.sep.vox.domain.repository.PracticeSessionRepository;
import com.sep.vox.domain.repository.PracticeTopicRepository;

/**
 * Phase 1 (khoá + đọc + kiểm tra idempotency/budget) của ResolveNextPracticeQuestionUseCase --
 * tách riêng thành bean @Transactional của chính nó để giữ FOR UPDATE + connection DB chỉ trong
 * đúng khoảng đọc nhanh này, KHÔNG trải dài qua lúc gọi LLM chậm (selectionService.resolveNextQuestion
 * bậc 4) như code cũ. Việc serialize các lời gọi trùng cho cùng 1 session giờ do khoá JVM ở
 * ResolveNextPracticeQuestionUseCase đảm nhiệm; FOR UPDATE ở đây chỉ còn bảo vệ đúng đoạn đọc
 * ngắn này khỏi ghi đè từ nơi khác, không còn phải giữ suốt cuộc gọi ngoài.
 */
@Service
@RequiredArgsConstructor
public class ResolveNextPracticeQuestionClaimService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeTopicRepository topicRepository;
    private final PracticeQuestionRepository questionRepository;
    private final PracticePaperItemRepository paperItemRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final PracticeQuestionSelectionService selectionService;
    private final PracticeTopicOfferEnrichmentService enrichmentService;

    /**
     * Đúng 1 trong 3 kết quả có ý nghĩa:
     * - idempotentQuestion != null: câu MỚI NHẤT đã được resolve trước đó (do lần gọi này hoặc
     *   1 lần gọi trước timeout) nhưng chưa có response -- trả lại NGUYÊN nó, không resolve mới.
     * - earlyExitReason != null: dừng luôn (hết hạn mức), không cần gọi resolveNextQuestion.
     * - cả 2 đều null: cần đi tiếp bậc 2 (gọi resolveNextQuestion, có thể chậm) rồi persist.
     */
    public record Claim(
        UUID studentId,
        UUID practicePaperId,
        PracticeTopic topic,
        PracticeFocusInfo focus,
        List<PracticeQuestion> alreadyChosen,
        PracticeQuestion idempotentQuestion,
        int idempotentSlot,
        String earlyExitReason,
        /**
         * Cặp số cho thanh tiến độ "đã nói / ngân sách" trên máy học sinh. Khác {@code
         * usedSeconds}/{@code maxSeconds} ngay bên trên ở CHỖ QUAN TRỌNG NHẤT: usedSeconds là
         * ngân sách DỰ TRÙ của các câu đã phát ra đề (dùng để quyết có phát thêm câu nữa
         * không), còn spokenSeconds là số giây học sinh THẬT SỰ nói (đúng thứ bị trừ quota).
         * Trên dữ liệu thật hai số này lệch nhau rất xa -- 45 giây dự trù cho 16 giây nói.
         */
        int spokenSeconds,
        int budgetSeconds,
        /**
         * Thứ tự bậc học sinh đã chọn khi dựng đề, đọc lại từ phiên. Trước đây chỗ này ước
         * lượng lại bậc mỗi lượt (bậc đo được + EMA hiệu năng + lần bỏ dở gần nhất), nên độ
         * khó có thể trôi ngay giữa phiên mà học sinh không hiểu vì sao.
         */
        int targetBandOrder,
        /** Số bậc của khung mà targetBandOrder thuộc về -- xem frameworkBandCountOfBand. */
        int targetBandCount,
        /** Khung mà bậc đích thuộc về -- ladder gửi xuống Python phải theo đúng khung này. */
        UUID targetFrameworkVersionId) {
    }

    /**
     * Dưới ngưỡng này thì không phát câu mới nữa. Không phải "đủ cho một câu trọn vẹn" --
     * chỉ là mốc dưới đó thì phát thêm câu là vô nghĩa: học sinh chưa kịp nói gì đã hết giờ.
     */
    private static final int MINIMUM_USEFUL_TURN_SECONDS = 10;

    @Transactional
    public Claim claim(UUID sessionId) {
        PracticeSession session = lockInProgressSession(sessionId);
        Progress progress = measureProgress(session);

        List<UUID> alreadyChosenIds = paperItemRepository.findQuestionIdsForPaper(session.getPracticePaperId());
        Optional<Claim> idempotentClaim = findIdempotentClaim(session, progress, alreadyChosenIds);
        if (idempotentClaim.isPresent()) {
            return idempotentClaim.get();
        }

        if (isBudgetExhausted(progress)) {
            return claimOf(session, progress, null, null, null, null, 0, "budget_exhausted");
        }

        PracticeTopic topic = topicRepository.findTopicById(session.getChosenPracticeTopicId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ đề luyện tập."));
        PracticeFocusInfo focus = selectionService.resolveFocus(session.getStudentId(), null);
        List<PracticeQuestion> alreadyChosen = questionRepository.findByIds(alreadyChosenIds);

        return claimOf(session, progress, topic, focus, alreadyChosen, null, 0, null);
    }

    /**
     * FOR UPDATE: it now only has to protect this short read/check section -- the JVM lock in
     * ResolveNextPracticeQuestionUseCase is what serializes the whole slow external call.
     */
    private PracticeSession lockInProgressSession(UUID sessionId) {
        PracticeSession session = practiceSessionRepository.findByIdForUpdate(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên luyện."));
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalArgumentException("Phiên luyện không còn hoạt động.");
        }
        return session;
    }

    private record Progress(
        int spokenSeconds, int budgetSeconds, int targetBandOrder, int targetBandCount,
        UUID targetFrameworkVersionId) {
    }

    /**
     * Computed EARLY, even before the idempotent branch: every returned branch must carry the pair of numbers for the
     * progress bar, including the branch that returns the old question unchanged after a call that timed out.
     */
    private Progress measureProgress(PracticeSession session) {
        int spokenSeconds = session.getGradedSeconds();
        int targetBandOrder = enrichmentService.bandOrder(session.getTargetFrameworkBandId());
        int targetBandCount = enrichmentService.frameworkBandCountOfBand(session.getTargetFrameworkBandId());
        UUID targetFrameworkVersionId =
            enrichmentService.frameworkVersionIdOfBand(session.getTargetFrameworkBandId());
        int budgetSeconds = enrichmentService.sessionBudgetSeconds(
            session.getStudentId(), targetBandOrder
        );
        return new Progress(spokenSeconds, budgetSeconds, targetBandOrder, targetBandCount, targetFrameworkVersionId);
    }

    private Optional<Claim> findIdempotentClaim(PracticeSession session, Progress progress, List<UUID> alreadyChosenIds) {
        if (alreadyChosenIds.isEmpty()) {
            return Optional.empty();
        }
        UUID latestQuestionId = alreadyChosenIds.getLast();
        if (practiceItemResponseRepository.existsResponse(session.getId(), latestQuestionId)) {
            return Optional.empty();
        }
        PracticeQuestion latestQuestion = questionRepository.findById(latestQuestionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi luyện."));
        return Optional.of(claimOf(session, progress, null, null, null, latestQuestion, alreadyChosenIds.size(), null));
    }

    /**
     * Is there still room for a meaningful speaking turn? Measured in seconds ACTUALLY SPOKEN, not the planned
     * budget of the questions already issued.
     *
     * <p>The previous version compared sumPlannedSecondsForPaper (the total of max_response_seconds) with the ceiling.
     * On real data the plan is 40-70% higher than reality (60 vs 29, 45 vs 27), so a session
     * reported the budget as exhausted while nearly half the quota was left. And quota is charged by the REAL cost of the
     * turns spoken (SubmitPracticeTurnUseCase converts turnCostUsd to VND and hands it to
     * ConsumeQuotaService), i.e. proportional to the real speaking time and not to the planned budget --
     * two clocks measuring two different quantities will sooner or later drift apart.
     *
     * <p>This check comes BEFORE choosing/generating the question, and that is the main point: the previous version checked AFTER
     * resolveNextQuestion had run, so it could pay 10-40 seconds of LLM calls to generate a question and then throw it
     * away because the budget was exhausted.
     *
     * <p>Speaking past the threshold midway is fine: the quotaExhausted flag takes care of that -- the turn is still
     * recorded and graded, and the session closes properly after the result is returned.
     */
    private boolean isBudgetExhausted(Progress progress) {
        return progress.spokenSeconds() + MINIMUM_USEFUL_TURN_SECONDS > progress.budgetSeconds();
    }

    private Claim claimOf(
            PracticeSession session, Progress progress, PracticeTopic topic, PracticeFocusInfo focus,
            List<PracticeQuestion> alreadyChosen, PracticeQuestion idempotentQuestion, int idempotentSlot,
            String earlyExitReason) {
        return new Claim(
            session.getStudentId(), session.getPracticePaperId(), topic, focus, alreadyChosen,
            idempotentQuestion, idempotentSlot, earlyExitReason,
            progress.spokenSeconds(), progress.budgetSeconds(), progress.targetBandOrder(),
            progress.targetBandCount(), progress.targetFrameworkVersionId()
        );
    }

}

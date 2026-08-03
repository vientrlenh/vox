package com.sep.vox.application.port.input.usecase.practicesession;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.service.PracticeQuestionSelectionService;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionClaimService;
import com.sep.vox.application.port.input.service.ResolveNextPracticeQuestionPersistenceService;
import com.sep.vox.domain.model.personalization.PracticeQuestion;

/**
 * Resolve ĐÚNG 1 câu MAIN tiếp theo trong lúc phiên luyện đang chạy -- gọi từ Python (nội bộ,
 * PracticeInternalSecretFilter bảo vệ) khi decision.should_continue == False. Xem gói 11 mục
 * 2.4 bước 4.
 *
 * KHÔNG @Transactional ở tầng này: selectionService.resolveNextQuestion (gọi trong doExecute)
 * có thể gọi LLM sinh câu hỏi trực tiếp (bậc 4, 10-20s) khi topic còn thưa câu -- giữ 1
 * transaction/connection DB mở suốt lúc đó từng gây HikariCP cạn pool dưới tải thật (xem
 * BuildPracticePaperUseCase, cùng lớp bug, sửa cùng đợt).
 *
 * Việc serialize các lời gọi trùng cho CÙNG 1 session (Python's timeout-retry-8s racing lần gọi
 * gốc vẫn đang chạy, xem practice_session_client.py) trước đây dựa vào SELECT ... FOR UPDATE giữ
 * suốt cả method -- giờ thay bằng khoá JVM keyed theo sessionId, giữ đúng phạm vi tương đương
 * (đủ vì Python luôn retry vào lại đúng instance vox-app này) nhưng không chiếm connection DB khi
 * đang chờ mạng. Transaction thật (FOR UPDATE + đọc, rồi ghi) được tách thành 2 bean riêng
 * (ResolveNextPracticeQuestionClaimService / ...PersistenceService) bao quanh đúng phần DB, với
 * cuộc gọi LLM chậm nằm GIỮA, ngoài mọi transaction.
 */
@Service
public class ResolveNextPracticeQuestionUseCase {

    public record QuestionPayload(
        UUID questionId,
        int slot,
        String questionText,
        String criterionCode,
        String subAttribute,
        int difficultyRank,
        int maxResponseSeconds,
        int minResponseSeconds,
        List<String> suggestedIdeas,
        /**
         * Câu CUỐI của phiên: ngân sách còn lại không đủ cho một câu cỡ bình thường, nên
         * câu này đã được may đo vừa đúng phần còn lại. Trả lời xong là đóng phiên, không
         * hỏi câu tiếp -- xem cách tính ở {@link #toPayload}.
         */
        boolean lastQuestion) {
    }

    /**
     * {@code sessionSpokenSeconds}/{@code sessionBudgetSeconds}: cặp số cho thanh tiến độ trên
     * máy học sinh. Đi kèm câu hỏi vì đây là thứ Python gửi xuống client NGAY khi phiên mở --
     * lượt đầu chưa nộp nên chưa có SubmitTurnResult nào để lấy ngân sách từ đó.
     */
    public record Result(
        String status,
        String reason,
        QuestionPayload question,
        int sessionSpokenSeconds,
        int sessionBudgetSeconds) {

        static Result ok(QuestionPayload question, int spokenSeconds, int budgetSeconds) {
            return new Result("ok", null, question, spokenSeconds, budgetSeconds);
        }

        static Result noMoreQuestions(String reason, int spokenSeconds, int budgetSeconds) {
            return new Result("no_more_questions", reason, null, spokenSeconds, budgetSeconds);
        }
    }

    // Không dọn dẹp entry cũ: ở quy mô hiện tại (demo, vài chục phiên đồng thời) số sessionId
    // phân biệt trong suốt vòng đời app là nhỏ, không đáng để thêm logic dọn có nguy cơ race
    // (xoá đúng lúc 1 luồng khác vừa lấy lock cũ từ map).
    private final ConcurrentHashMap<UUID, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    private final ResolveNextPracticeQuestionClaimService claimService;
    private final PracticeQuestionSelectionService selectionService;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final ResolveNextPracticeQuestionPersistenceService persistenceService;

    public ResolveNextPracticeQuestionUseCase(
            ResolveNextPracticeQuestionClaimService claimService,
            PracticeQuestionSelectionService selectionService,
            PracticeTopicOfferEnrichmentService enrichmentService,
            ResolveNextPracticeQuestionPersistenceService persistenceService) {
        this.claimService = claimService;
        this.selectionService = selectionService;
        this.enrichmentService = enrichmentService;
        this.persistenceService = persistenceService;
    }

    public Result execute(UUID sessionId) {
        var lock = sessionLocks.computeIfAbsent(sessionId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return doExecute(sessionId);
        } finally {
            lock.unlock();
        }
    }

    private Result doExecute(UUID sessionId) {
        var claim = claimService.claim(sessionId);
        if (claim.idempotentQuestion() != null) {
            return Result.ok(
                toPayload(
                    claim.idempotentQuestion(),
                    claim.idempotentSlot(),
                    claim.budgetSeconds() - claim.spokenSeconds()
                ),
                claim.spokenSeconds(),
                claim.budgetSeconds()
            );
        }
        if (claim.earlyExitReason() != null) {
            return Result.noMoreQuestions(
                claim.earlyExitReason(), claim.spokenSeconds(), claim.budgetSeconds()
            );
        }

        var signal = enrichmentService.studentRankSignal(claim.studentId());
        var baseRank = enrichmentService.rankForTopic(claim.studentId(), claim.topic().getId(), signal);

        var selection = selectionService
            .resolveNextQuestion(claim.topic(), claim.studentId(), claim.focus(), baseRank, claim.alreadyChosen())
            .orElse(null);
        if (selection == null) {
            return Result.noMoreQuestions(
                "pool_exhausted", claim.spokenSeconds(), claim.budgetSeconds()
            );
        }

        // Không còn chốt ngân sách ở đây: nó đã chuyển lên ResolveNextPracticeQuestionClaimService,
        // chạy TRƯỚC resolveNextQuestion. Đặt sau như cũ nghĩa là có thể sinh câu bằng LLM
        // (10-40 giây) rồi vứt đi vì hết ngân sách.
        var question = selection.question();

        persistenceService.persist(claim.studentId(), claim.practicePaperId(), selection);
        return Result.ok(
            toPayload(question, selection.slot(), claim.budgetSeconds() - claim.spokenSeconds()),
            claim.spokenSeconds(),
            claim.budgetSeconds()
        );
    }

    /**
     * May đo thời lượng câu hỏi theo ngân sách CÒN LẠI của phiên.
     *
     * Khi phần còn lại không đủ cho một câu cỡ bình thường, thay vì từ chối phát câu (bỏ
     * phí phần thời gian đó) thì cắt câu cho vừa: trần = đúng số giây còn lại, sàn = một
     * nửa. Học sinh vẫn nói được thêm một lượt trọn vẹn theo cỡ nhỏ hơn, rồi phiên đóng.
     *
     * KHÔNG ghi đè vào bản ghi câu hỏi trong kho -- câu hỏi dùng chung cho mọi học sinh,
     * sửa nó là làm hỏng cho người khác. Chỉ đổi con số gửi xuống cho phiên NÀY.
     */
    private QuestionPayload toPayload(PracticeQuestion question, int slot, int remainingSeconds) {
        // > 1 chứ không > 0: còn đúng 1 giây thì cắt ra sàn = trần = 1, mà sàn phải NHỎ HƠN
        // trần -- bằng nhau thì SignalNode coi mọi câu trả lời là chưa đạt và hỏi mãi.
        // Dưới ngưỡng đó cứ giữ nguyên số của câu; phiên sẽ đóng ở chốt ngân sách lần sau.
        var last = remainingSeconds > 1 && remainingSeconds < question.getMaxResponseSeconds();
        var maxSeconds = last ? remainingSeconds : question.getMaxResponseSeconds();
        var minSeconds = Math.max(1, Math.min(
            last ? remainingSeconds / 2 : question.getMinResponseSeconds(),
            maxSeconds - 1
        ));
        return new QuestionPayload(
            question.getId(),
            slot,
            question.getQuestionText(),
            question.getTargetCriterionCode(),
            question.getTargetSubAttribute(),
            question.getDifficultyRank(),
            maxSeconds,
            minSeconds,
            parseIdeas(question.getSuggestedIdeasJson()),
            last
        );
    }

    private List<String> parseIdeas(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return new tools.jackson.databind.json.JsonMapper()
                .readValue(value, new tools.jackson.core.type.TypeReference<List<String>>() {
                });
        } catch (Exception exception) {
            return List.of();
        }
    }
}

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
        int preparationTimeSeconds,
        int maxResponseSeconds,
        int maxFollowupSeconds,
        List<String> suggestedIdeas) {
    }

    public record Result(String status, String reason, QuestionPayload question) {

        static Result ok(QuestionPayload question) {
            return new Result("ok", null, question);
        }

        static Result noMoreQuestions(String reason) {
            return new Result("no_more_questions", reason, null);
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
            return Result.ok(toPayload(claim.idempotentQuestion(), claim.idempotentSlot()));
        }
        if (claim.earlyExitReason() != null) {
            return Result.noMoreQuestions(claim.earlyExitReason());
        }

        var signal = enrichmentService.studentRankSignal(claim.studentId());
        var baseRank = enrichmentService.rankForTopic(claim.studentId(), claim.topic().getId(), signal);

        var selection = selectionService
            .resolveNextQuestion(claim.topic(), claim.studentId(), claim.focus(), baseRank, claim.alreadyChosen())
            .orElse(null);
        if (selection == null) {
            return Result.noMoreQuestions("pool_exhausted");
        }

        var question = selection.question();
        if (claim.usedSeconds() + question.plannedSeconds() > claim.maxSeconds()) {
            return Result.noMoreQuestions("budget_exhausted");
        }

        persistenceService.persist(claim.studentId(), claim.practicePaperId(), selection);
        return Result.ok(toPayload(question, selection.slot()));
    }

    private QuestionPayload toPayload(PracticeQuestion question, int slot) {
        return new QuestionPayload(
            question.getId(),
            slot,
            question.getQuestionText(),
            question.getTargetCriterionCode(),
            question.getTargetSubAttribute(),
            question.getDifficultyRank(),
            question.getPreparationTimeSeconds(),
            question.getMaxResponseSeconds(),
            question.getMaxFollowupSeconds(),
            parseIdeas(question.getSuggestedIdeasJson())
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

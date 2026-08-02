package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.personalization.PracticeItemResponseRepository;
import com.sep.vox.domain.repository.personalization.PracticePaperItemRepository;
import com.sep.vox.domain.repository.personalization.PracticeQuestionRepository;
import com.sep.vox.domain.repository.personalization.PracticeSessionRepository;
import com.sep.vox.domain.repository.personalization.PracticeTopicRepository;

/**
 * Phase 1 (khoá + đọc + kiểm tra idempotency/budget) của ResolveNextPracticeQuestionUseCase --
 * tách riêng thành bean @Transactional của chính nó để giữ FOR UPDATE + connection DB chỉ trong
 * đúng khoảng đọc nhanh này, KHÔNG trải dài qua lúc gọi LLM chậm (selectionService.resolveNextQuestion
 * bậc 4) như code cũ. Việc serialize các lời gọi trùng cho cùng 1 session giờ do khoá JVM ở
 * ResolveNextPracticeQuestionUseCase đảm nhiệm; FOR UPDATE ở đây chỉ còn bảo vệ đúng đoạn đọc
 * ngắn này khỏi ghi đè từ nơi khác, không còn phải giữ suốt cuộc gọi ngoài.
 */
@Service
public class ResolveNextPracticeQuestionClaimService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final PracticeTopicRepository topicRepository;
    private final PracticeQuestionRepository questionRepository;
    private final PracticePaperItemRepository paperItemRepository;
    private final PracticeItemResponseRepository practiceItemResponseRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final PracticeQuestionSelectionService selectionService;
    private final PracticeTopicOfferEnrichmentService enrichmentService;

    public ResolveNextPracticeQuestionClaimService(
            PracticeSessionRepository practiceSessionRepository,
            PracticeTopicRepository topicRepository,
            PracticeQuestionRepository questionRepository,
            PracticePaperItemRepository paperItemRepository,
            PracticeItemResponseRepository practiceItemResponseRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            PracticeQuestionSelectionService selectionService,
            PracticeTopicOfferEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
        this.practiceSessionRepository = practiceSessionRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.paperItemRepository = paperItemRepository;
        this.practiceItemResponseRepository = practiceItemResponseRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.selectionService = selectionService;
    }

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
        int usedSeconds,
        int maxSeconds,
        PracticeQuestion idempotentQuestion,
        int idempotentSlot,
        String earlyExitReason) {
    }

    @Transactional
    public Claim claim(UUID sessionId) {
        // FOR UPDATE: chỉ còn cần bảo vệ đúng đoạn đọc/kiểm tra ngắn này -- khoá JVM ở
        // ResolveNextPracticeQuestionUseCase mới là thứ serialize xuyên suốt cuộc gọi ngoài chậm.
        var session = practiceSessionRepository.findByIdForUpdate(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException("Phiên luyện không còn hoạt động.");
        }

        var alreadyChosenIds = paperItemRepository.findQuestionIdsForPaper(session.getPracticePaperId());
        if (!alreadyChosenIds.isEmpty()) {
            var latestQuestionId = alreadyChosenIds.getLast();
            if (!practiceItemResponseRepository.existsResponse(sessionId, latestQuestionId)) {
                var latestQuestion = questionRepository.findById(latestQuestionId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi luyện."));
                return new Claim(
                    session.getStudentId(), session.getPracticePaperId(), null, null, null, 0, 0,
                    latestQuestion, alreadyChosenIds.size(), null
                );
            }
        }

        var usedSeconds = paperItemRepository.sumPlannedSecondsForPaper(session.getPracticePaperId());
        var maxMinutes = schoolSubscriptionRepository
            .findMaxTimePerAttemptMinForUser(session.getStudentId());
        var quotaSeconds = maxMinutes == null ? 0 : maxMinutes * 60;
        // HAI ngân sách (thiết kế gói 6 mục 4.1), lấy cái chặt hơn:
        //   quota      = trường mua bao nhiêu giây, tiêu dần QUA NHIỀU phiên
        //   trần bậc   = MỘT phiên dài tối đa bao lâu là hợp sức bậc đó
        // Trần bậc KHÔNG đốt quota: hết trần thì phiên dừng, phần quota còn lại vẫn nguyên
        // cho phiên sau. Nên quota 30 phút ở bậc thấp thành nhiều phiên ngắn 12 phút, chứ
        // không phải một phiên 30 phút quá sức.
        var maxSeconds = Math.min(
            quotaSeconds,
            enrichmentService.sessionSecondsCapForStudent(session.getStudentId())
        );
        if (usedSeconds >= maxSeconds) {
            return new Claim(
                session.getStudentId(), session.getPracticePaperId(), null, null, null, 0, 0,
                null, 0, "budget_exhausted"
            );
        }

        var topic = topicRepository.findTopicById(session.getChosenPracticeTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề luyện tập."));
        var focus = selectionService.resolveFocus(session.getStudentId(), null);
        var alreadyChosen = questionRepository.findByIds(alreadyChosenIds);

        return new Claim(
            session.getStudentId(), session.getPracticePaperId(), topic, focus, alreadyChosen,
            usedSeconds, maxSeconds, null, 0, null
        );
    }
}

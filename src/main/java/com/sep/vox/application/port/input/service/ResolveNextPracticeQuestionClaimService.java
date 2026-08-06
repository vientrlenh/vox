package com.sep.vox.application.port.input.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.query.dto.PracticeFocusInfo;
import com.sep.vox.domain.model.personalization.PracticeQuestion;
import com.sep.vox.domain.model.personalization.PracticeTopic;
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
    private final PracticeQuestionSelectionService selectionService;
    private final PracticeTopicOfferEnrichmentService enrichmentService;

    public ResolveNextPracticeQuestionClaimService(
            PracticeSessionRepository practiceSessionRepository,
            PracticeTopicRepository topicRepository,
            PracticeQuestionRepository questionRepository,
            PracticePaperItemRepository paperItemRepository,
            PracticeItemResponseRepository practiceItemResponseRepository,
            PracticeQuestionSelectionService selectionService,
            PracticeTopicOfferEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
        this.practiceSessionRepository = practiceSessionRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.paperItemRepository = paperItemRepository;
        this.practiceItemResponseRepository = practiceItemResponseRepository;
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
        int targetBandOrder) {
    }

    /**
     * Dưới ngưỡng này thì không phát câu mới nữa. Không phải "đủ cho một câu trọn vẹn" --
     * chỉ là mốc dưới đó thì phát thêm câu là vô nghĩa: học sinh chưa kịp nói gì đã hết giờ.
     */
    private static final int MINIMUM_USEFUL_TURN_SECONDS = 10;

    @Transactional
    public Claim claim(UUID sessionId) {
        // FOR UPDATE: chỉ còn cần bảo vệ đúng đoạn đọc/kiểm tra ngắn này -- khoá JVM ở
        // ResolveNextPracticeQuestionUseCase mới là thứ serialize xuyên suốt cuộc gọi ngoài chậm.
        var session = practiceSessionRepository.findByIdForUpdate(sessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên luyện."));
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException("Phiên luyện không còn hoạt động.");
        }

        // Tính SỚM, trước cả nhánh idempotent: mọi nhánh trả về đều phải mang được cặp số cho
        // thanh tiến độ, kể cả nhánh trả lại nguyên câu cũ sau một lần gọi bị timeout.
        var spokenSeconds = session.getGradedSeconds();
        var targetBandOrder = enrichmentService.bandOrder(session.getTargetFrameworkBandId());
        var budgetSeconds = enrichmentService.sessionBudgetSeconds(
            session.getStudentId(), targetBandOrder
        );

        var alreadyChosenIds = paperItemRepository.findQuestionIdsForPaper(session.getPracticePaperId());
        if (!alreadyChosenIds.isEmpty()) {
            var latestQuestionId = alreadyChosenIds.getLast();
            if (!practiceItemResponseRepository.existsResponse(sessionId, latestQuestionId)) {
                var latestQuestion = questionRepository.findById(latestQuestionId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy câu hỏi luyện."));
                return new Claim(
                    session.getStudentId(), session.getPracticePaperId(), null, null, null,
                    latestQuestion, alreadyChosenIds.size(), null,
                    spokenSeconds, budgetSeconds, targetBandOrder
                );
            }
        }

        // Còn đủ chỗ cho một lượt nói có nghĩa không? Đo bằng giây NÓI THẬT, không phải
        // ngân sách dự trù của các câu đã phát.
        //
        // Bản trước so sumPlannedSecondsForPaper (tổng max_response_seconds) với trần.
        // Trên dữ liệu thật dự trù cao hơn thực tế 40-70% (60 vs 29, 45 vs 27), nên phiên
        // báo hết ngân sách khi quota còn dư gần nửa. Mà quota thì trừ theo giây nói thật
        // (ConsumeQuotaUseCase ăn durationSeconds) -- hai đồng hồ đo hai đại lượng khác
        // nhau thì sớm muộn cũng lệch.
        //
        // Chốt này nằm TRƯỚC lúc chọn/sinh câu, và đó là điểm chính: bản trước kiểm SAU
        // khi resolveNextQuestion đã chạy, tức có thể trả 10-40 giây gọi LLM để sinh ra
        // một câu rồi vứt đi vì hết ngân sách.
        //
        // Nói vượt ngưỡng giữa chừng không sao: cờ quotaExhausted lo phần đó -- lượt vẫn
        // được ghi và chấm, phiên đóng tử tế sau khi trả kết quả.
        if (spokenSeconds + MINIMUM_USEFUL_TURN_SECONDS > budgetSeconds) {
            return new Claim(
                session.getStudentId(), session.getPracticePaperId(), null, null, null,
                null, 0, "budget_exhausted", spokenSeconds, budgetSeconds, targetBandOrder
            );
        }

        var topic = topicRepository.findTopicById(session.getChosenPracticeTopicId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề luyện tập."));
        var focus = selectionService.resolveFocus(session.getStudentId(), null);
        var alreadyChosen = questionRepository.findByIds(alreadyChosenIds);

        return new Claim(
            session.getStudentId(), session.getPracticePaperId(), topic, focus, alreadyChosen,
            null, 0, null, spokenSeconds, budgetSeconds, targetBandOrder
        );
    }

}

package com.sep.vox.application.response.input.practiceplanning;

import java.util.List;
import java.util.UUID;

public final class PracticePlanningResponses {

    private PracticePlanningResponses() {
    }

    /**
     * Không còn trường {@code level}. Trước đây mỗi thẻ mang một mức độ riêng, nên học sinh
     * nhìn thấy "Công nghệ — Nâng cao" cạnh "Đồ ăn — Cơ bản" rồi bấm cái thứ hai: hệ thống ghi
     * nhận "em thích Đồ ăn" trong khi em chỉ chọn cái dễ hơn. Giờ độ khó chọn MỘT lần cho cả
     * phiên nên mọi thẻ cùng một mức, và lựa chọn chủ đề trở lại thuần sở thích.
     */
    public record PracticeTopicOffer(
            UUID topicId,
            String name,
            String dimension,
            boolean savedByMe,
            Integer matchPercent,
            int minutes,
            String rationale,
            List<String> reasons) {

        public PracticeTopicOffer(UUID topicId, String name, String dimension, boolean savedByMe) {
            this(topicId, name, dimension, savedByMe, null, 0, null, List.of());
        }
    }

    public record TopicSearchResult(
            List<PracticeTopicOffer> topics,
            boolean canGenerate) {
    }

    public record PracticePaperQuestion(
            UUID questionId,
            int slot,
            String questionText,
            String criterionCode,
            String subAttribute,
            int difficultyRank,
            int maxResponseSeconds,
            int minResponseSeconds,
            List<String> suggestedIdeas) {
    }

    public record PracticePaper(
            UUID id,
            UUID topicId,
            String origin,
            int plannedSeconds,
            int reservedQuotaSeconds,
            /** Trần nói của cả phiên (giây) -- xem PracticePaperDto.sessionBudgetSeconds. */
            int sessionBudgetSeconds,
            List<PracticePaperQuestion> questions) {
    }

    /**
     * Kết quả dựng đề theo mô hình 2 pha: dựng đề có thể phải nhờ AI sinh câu mới
     * (10-20s) khi kho chưa có câu phù hợp, quá lâu để giữ 1 request HTTP. Pha 1
     * (buildPracticePaper) trả về ngay READY nếu kho có sẵn, hoặc PREPARING nếu đang
     * sinh; pha 2 (practicePaperDraft) để client hỏi lại.
     */
    public record PracticePaperDraft(
            UUID draftId,
            String status,
            String reason,
            PracticePaper paper) {

        public static PracticePaperDraft preparing(UUID draftId) {
            return new PracticePaperDraft(draftId, "PREPARING", null, null);
        }

        public static PracticePaperDraft ready(UUID draftId, PracticePaper paper) {
            return new PracticePaperDraft(draftId, "READY", null, paper);
        }

        public static PracticePaperDraft failed(UUID draftId, String reason) {
            return new PracticePaperDraft(draftId, "FAILED", reason, null);
        }
    }
}

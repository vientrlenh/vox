package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record PracticePaperDto(
    UUID id,
    UUID topicId,
    String origin,
    int plannedSeconds,
    int reservedQuotaSeconds,
    /**
     * Ngân sách nói của CẢ phiên (giây): chỗ hẹp hơn giữa hạn mức gói và trần bậc.
     *
     * Khác {@code plannedSeconds} ngay bên trên: cái kia là tổng dự trù của các câu trong
     * đề này, cái này là trần cho toàn phiên. Client cần nó ngay từ lúc mở đề để vẽ thanh
     * "đã nói / ngân sách" -- câu đầu tiên chưa nộp lượt nào nên chưa có nguồn nào khác.
     */
    int sessionBudgetSeconds,
    List<PracticePaperQuestionDto> questions
) {
}

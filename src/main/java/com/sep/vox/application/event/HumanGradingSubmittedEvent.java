package com.sep.vox.application.event;

import java.util.List;
import java.util.UUID;

/**
 * Bắn sau khi giáo viên chấm lại (RegradeResultUseCase) — nghe bởi
 * GradingDiagnosticsInferenceJob để suy nhãn điểm yếu (sub-attribute) từ feedbackSummary,
 * không tham gia tính điểm/xếp loại.
 */
public record HumanGradingSubmittedEvent(UUID studentId, List<Item> items) {

    public record Item(UUID evaluationId, UUID paperItemId, String feedbackSummary, List<CriterionRef> criteria) {
    }

    /** Đủ để gửi code cho Python phân loại, và ánh xạ ngược về frameworkCriterionId khi lưu. */
    public record CriterionRef(String code, UUID frameworkCriterionId) {
    }
}

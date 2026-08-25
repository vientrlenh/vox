package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record SubmitTurnResultDto(
    UUID responseId,
    UUID turnId,
    int remainingGradedSeconds,
    boolean evaluationQueued,
    List<TurnCorrectionDto> corrections,
    /**
     * Lượt này đã tiêu nốt phần hạn mức còn lại -- phiên phải đóng sau khi trả kết quả.
     *
     * Báo bằng CỜ chứ không bằng lỗi: trước đây hết quota thì ConsumeQuotaUseCase ném
     * QuotaExceededException, mà method này có @Transactional nên cả lượt nói vừa lưu ở trên
     * bị rollback theo. Học sinh đã nói thì lượt đó phải được ghi và chấm; hết hạn mức là
     * chuyện của phiên tiếp theo, không phải lý do xoá công sức vừa bỏ ra.
     */
    boolean quotaExhausted,
    /** Ví nào cạn khi quotaExhausted=true -- "SCHOOL" (hạn mức chung của trường) hay "PERSONAL"
     * (phần trường cấp riêng cho học sinh này). Null khi quotaExhausted=false. */
    String quotaExhaustedScope,
    /** Đã nói bao nhiêu giây trong phiên (kể cả lượt này) -- xem SubmitTurnResult. */
    int sessionSpokenSeconds,
    /** Ngân sách nói của phiên: chỗ hẹp hơn giữa hạn mức gói và trần bậc. */
    int sessionBudgetSeconds
) {
}

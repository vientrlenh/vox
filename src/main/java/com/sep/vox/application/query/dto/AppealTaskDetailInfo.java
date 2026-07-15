package com.sep.vox.application.query.dto;

import java.util.List;
import java.util.UUID;

/**
 * Góc nhìn giám khảo. CÓ aiScores (chính sách đã chốt: giám khảo được tham chiếu
 * điểm AI), nhưng KHÔNG có báo cáo của giám khảo khác — để tránh thiên lệch.
 */
public record AppealTaskDetailInfo(
    UUID appealId,
    String partLabel,
    List<AppealTurnInfo> turns,
    List<AppealCriterionScoreInfo> aiScores,
    List<AppealCriterionMetaInfo> criteria,
    AppealReviewerInfo myReport
) {
}

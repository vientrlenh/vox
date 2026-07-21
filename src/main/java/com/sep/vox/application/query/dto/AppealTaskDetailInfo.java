package com.sep.vox.application.query.dto;

import java.util.List;
import java.util.UUID;

/**
 * Góc nhìn giám khảo. CÓ aiScores (chính sách đã chốt: giám khảo được tham chiếu
 * điểm AI), nhưng KHÔNG có báo cáo của giám khảo khác — để tránh thiên lệch.
 */
public record AppealTaskDetailInfo(
    UUID appealId,
    /** Các phần thi phải chấm lại, mỗi phần kèm lượt nói và điểm AI gốc của riêng nó. */
    List<AppealItemInfo> items,
    List<AppealCriterionMetaInfo> criteria,
    /** Báo cáo của chính giám khảo này theo từng phần; rỗng khi chưa nộp. */
    List<AppealReviewerItemInfo> myReport
) {
}

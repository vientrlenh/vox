package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Một phần thi được phúc khảo, kèm dữ liệu để đối chiếu.
 * `finalScore` null cho tới khi đơn được công bố.
 */
public record AppealItemInfo(
    UUID appealItemId,
    UUID paperItemId,
    String partLabel,
    /**
     * Điểm tiêu chí của bản chấm đang có hiệu lực: vòng đầu là bản AI, vòng sau là
     * bản chấm tay của vòng trước. Không phải lúc nào cũng là điểm AI.
     */
    List<AppealCriterionScoreInfo> baselineScores,
    /** Luôn lấy từ bản AI — chỉ bản AI mới có lượt nói. */
    List<AppealTurnInfo> turns,
    BigDecimal finalScore
) {
}

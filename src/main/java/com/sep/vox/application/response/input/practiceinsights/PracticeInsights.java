package com.sep.vox.application.response.input.practiceinsights;

import java.util.List;
import java.util.UUID;

public final class PracticeInsights {

    private PracticeInsights() {
    }

    /**
     * Chỉ còn mức TIÊU CHÍ. Phần sub-attribute (nhãn lỗi chi tiết kèm bằng chứng, xu hướng,
     * "sắp khỏi"/"mới phát hiện") đã bỏ cùng trang hồ sơ điểm yếu -- dữ liệu vẫn được đo và
     * vẫn dùng để chọn tiêu chí/nhãn khi ra đề, chỉ là không phơi ra màn hình nữa.
     */
    public record WeaknessProfile(
        List<CriterionWeakness> criteria,
        int sessionsAnalysed
    ) {
    }

    public record CriterionWeakness(
        String criterionCode,
        String criterionName,
        double weakness,
        int observationCount,
        boolean reliable
    ) {
    }
}

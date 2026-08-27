package com.sep.vox.application.query.repository;

import java.time.Instant;
import java.util.List;

import com.sep.vox.application.query.dto.SessionCostDto;

/**
 * Đọc chi phí AI THẬT gộp theo phiên, cho QuotaPricingCalibrationService.
 *
 * <p>Tách khỏi AiUsageRecordRepository một cách CÓ CHỦ Ý dù cùng đọc một bảng: cổng kia phục vụ đường
 * GHI (ghi usage, cộng tiền, đóng dấu đã thu) nên nói bằng ngôn ngữ của aggregate, còn cái này là một
 * BÁO CÁO -- gộp nhiều phiên lại, không tương ứng với thực thể nào và không có đường ghi ngược. Để
 * chung thì hình dạng báo cáo phải sống trong domain.repository, và mỗi màn thống kê mới lại thêm một
 * record nữa vào đó cho tới khi không còn phân biệt được cái nào là mô hình nghiệp vụ.
 *
 * <p>Xem TokenUsageTimeseriesQueryRepository / QuestionBankStatsQueryRepository cho cùng khuôn.
 */
public interface SessionCostQueryRepository {

    /**
     * Tổng cost_usd theo TỪNG phiên có {@code occurred_at >= since}.
     *
     * <p>Không lọc theo charged_at: calibrate cần chi phí THẬT đã phát sinh để suy ra giá mỗi giây,
     * còn đã thu tiền hay chưa là chuyện kế toán không liên quan tới việc đo giá vốn.
     */
    List<SessionCostDto> sumCostUsdGroupedBySessionSince(Instant since);
}

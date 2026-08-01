package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tín hiệu rủi ro của một bài, lấy trong MỘT query cho cả tập ứng viên rồi mới xếp
 * hạng trong Java — xếp hạng trong SQL sẽ khoá logic vào một dialect và không test
 * đơn vị được.
 *
 * @param minConfidence độ tự tin thấp nhất trong các phần đã chấm ({@code null} khi
 *                      bài chưa có bản AI nào, ví dụ đã bị chấm tay đè hết)
 * @param passingScore  ngưỡng đạt của chính sách; {@code null} khi trường không đặt
 *                      ngưỡng, lúc đó "sát ngưỡng" không có nghĩa
 */
public record GradingRiskInfo(
    UUID candidateResultId,
    BigDecimal minConfidence,
    BigDecimal totalScore,
    BigDecimal passingScore
) {
}

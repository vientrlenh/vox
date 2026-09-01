package com.sep.vox.application.port.input.query;

import java.time.Instant;

/**
 * Cửa sổ thời gian của thẻ chi phí AI.
 *
 * <p>Không có {@code schoolId}: phạm vi lấy từ người đang đăng nhập. Bản cũ
 * ({@code schoolTokenUsageTimeseries}) nhận schoolId từ client — một quản trị trường gửi id trường
 * khác lên là đọc được chi tiêu của họ. Bỏ tham số đó là sửa luôn chỗ đó.
 *
 * @param from bao gồm; null = lùi 30 ngày kể từ {@code to}.
 * @param to   KHÔNG bao gồm; null = ngay lúc gọi. Cùng quy ước nửa mở với mọi cửa sổ khác trong hệ
 *             thống — hai dải liền nhau dùng {@code <=} sẽ đếm trùng khoản rơi đúng ranh giới.
 */
public record ViewSchoolAiCostQuery(
    Instant from,
    Instant to,
    AiCostGranularity granularity
) {

    /**
     * Đơn vị gom nhóm. Enum ĐÓNG chứ không phải chuỗi tự do: giá trị này ghép thẳng vào
     * {@code date_trunc} nên nó không bao giờ được đến từ dữ liệu người dùng.
     */
    public enum AiCostGranularity {
        DAY("day"),
        WEEK("week"),
        MONTH("month");

        private final String sqlUnit;

        AiCostGranularity(String sqlUnit) {
            this.sqlUnit = sqlUnit;
        }

        public String sqlUnit() {
            return sqlUnit;
        }
    }
}

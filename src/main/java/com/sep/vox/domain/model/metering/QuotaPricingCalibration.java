package com.sep.vox.domain.model.metering;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class QuotaPricingCalibration {
    private UUID id;
    // Thời điểm job tính ra row này -- dùng để lấy row mới nhất (ORDER BY computed_at DESC).
    private Instant computedAt;
    // Cửa sổ trailing (số ngày nhìn lại) đã dùng LÚC TÍNH row này -- lưu lại để tra được lịch sử
    // đang dùng cấu hình bao nhiêu ngày, phòng khi sau này đổi .env.
    //VD => "chỉ gom những session nào diễn ra TRONG VÒNG 90 ngày gần đây, bỏ qua session cũ hơn".
    private int windowDays;
    // Số session THẬT SỰ dùng để tính -- chỉ tính session nào khớp được cả cost_usd lẫn
    // duration_seconds (thiếu 1 trong 2 thì bị loại khỏi mẫu).
    private int sessionCount;
    // Tổng cost_usd cộng dồn từ đúng sessionCount session ở trên.
    private BigDecimal totalCostUsd;
    // Tổng giây trả lời THẬT (không phải giây cấu hình) cộng dồn từ đúng các session đó.
    private long totalAnsweredSeconds;
    // Số tính THẲNG, chưa qua xử lý gì: totalCostUsd / totalAnsweredSeconds.
    private BigDecimal rawRateUsdPerSecond;
    // Số THẬT SỰ dùng cho guard (QuotaPricingService đọc field này) -- rawRateUsdPerSecond sau khi
    // đã làm mượt (không lệch quá maxChangeRatio so với applied lần trước) và chặn biên an toàn
    // (kẹp trong [minRateBound, maxRateBound]).
    private BigDecimal appliedRateUsdPerSecond;
    // Lý do applied khác raw (bị làm mượt hoặc chặn biên) -- null nếu 2 số bằng nhau, không cần
    // điều chỉnh gì.
    private String note;
    // Nguồn dữ liệu đã dùng để calibrate row này -- EXAM (join exam_item_responses) hay PRACTICE
    // (join practice_response_turn), xem QuotaPricingSource.
    private QuotaPricingSource pricingSource;

    public QuotaPricingCalibration() {}

    public QuotaPricingCalibration(UUID id, Instant computedAt, int windowDays, int sessionCount,
            BigDecimal totalCostUsd, long totalAnsweredSeconds, BigDecimal rawRateUsdPerSecond,
            BigDecimal appliedRateUsdPerSecond, String note, QuotaPricingSource pricingSource) {
        this.id = id;
        this.computedAt = computedAt;
        this.windowDays = windowDays;
        this.sessionCount = sessionCount;
        this.totalCostUsd = totalCostUsd;
        this.totalAnsweredSeconds = totalAnsweredSeconds;
        this.rawRateUsdPerSecond = rawRateUsdPerSecond;
        this.appliedRateUsdPerSecond = appliedRateUsdPerSecond;
        this.note = note;
        this.pricingSource = pricingSource;
    }

    public QuotaPricingCalibration(Instant computedAt, int windowDays, int sessionCount,
            BigDecimal totalCostUsd, long totalAnsweredSeconds, BigDecimal rawRateUsdPerSecond,
            BigDecimal appliedRateUsdPerSecond, String note, QuotaPricingSource pricingSource) {
        this.computedAt = computedAt;
        this.windowDays = windowDays;
        this.sessionCount = sessionCount;
        this.totalCostUsd = totalCostUsd;
        this.totalAnsweredSeconds = totalAnsweredSeconds;
        this.rawRateUsdPerSecond = rawRateUsdPerSecond;
        this.appliedRateUsdPerSecond = appliedRateUsdPerSecond;
        this.note = note;
        this.pricingSource = pricingSource;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public BigDecimal getTotalCostUsd() {
        return totalCostUsd;
    }

    public void setTotalCostUsd(BigDecimal totalCostUsd) {
        this.totalCostUsd = totalCostUsd;
    }

    public long getTotalAnsweredSeconds() {
        return totalAnsweredSeconds;
    }

    public void setTotalAnsweredSeconds(long totalAnsweredSeconds) {
        this.totalAnsweredSeconds = totalAnsweredSeconds;
    }

    public BigDecimal getRawRateUsdPerSecond() {
        return rawRateUsdPerSecond;
    }

    public void setRawRateUsdPerSecond(BigDecimal rawRateUsdPerSecond) {
        this.rawRateUsdPerSecond = rawRateUsdPerSecond;
    }

    public BigDecimal getAppliedRateUsdPerSecond() {
        return appliedRateUsdPerSecond;
    }

    public void setAppliedRateUsdPerSecond(BigDecimal appliedRateUsdPerSecond) {
        this.appliedRateUsdPerSecond = appliedRateUsdPerSecond;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public QuotaPricingSource getPricingSource() {
        return pricingSource;
    }

    public void setPricingSource(QuotaPricingSource pricingSource) {
        this.pricingSource = pricingSource;
    }
}

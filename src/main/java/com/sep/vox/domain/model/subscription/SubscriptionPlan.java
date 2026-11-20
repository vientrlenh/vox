package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.common.ZoneConstant;

public class SubscriptionPlan {
    private UUID id;
    private String name;
    private String tagline;
    private BigDecimal priceVnd;
    private SubscriptionPlanPeriod periodType;
    private Integer periodCount;
    private Integer maxTimePerAttemptMin;
    private SubscriptionPlanStatus status;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private UUID replacedByPlanId;

    public SubscriptionPlan() {}

    public SubscriptionPlan(UUID id, String name, String tagline, BigDecimal priceVnd, SubscriptionPlanPeriod periodType, Integer periodCount,
            Integer maxTimePerAttemptMin, SubscriptionPlanStatus status, Long version,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy, UUID replacedByPlanId) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.priceVnd = priceVnd;
        this.periodType = periodType;
        this.periodCount = periodCount;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt; 
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.replacedByPlanId = replacedByPlanId;
    }

    public SubscriptionPlan(String name, String tagline, BigDecimal priceVnd, SubscriptionPlanPeriod periodType, Integer periodCount,
            Integer maxTimePerAttemptMin, SubscriptionPlanStatus status, Long version,
            Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy) {
        this.name = name;
        this.tagline = tagline;
        this.priceVnd = priceVnd;
        this.periodType = periodType;
        this.periodCount = periodCount;
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public BigDecimal getPriceVnd() {
        return priceVnd;
    }

    public void setPriceVnd(BigDecimal priceVnd) {
        this.priceVnd = priceVnd;
    }

    public SubscriptionPlanPeriod getPeriodType() {
        return periodType;
    }

    public void setPeriodType(SubscriptionPlanPeriod periodType) {
        this.periodType = periodType;
    }

    public Integer getPeriodCount() {
        return periodCount;
    }

    public void setPeriodCount(Integer periodCount) {
        this.periodCount = periodCount;
    }

    public Integer getMaxTimePerAttemptMin() {
        return maxTimePerAttemptMin;
    }

    public void setMaxTimePerAttemptMin(Integer maxTimePerAttemptMin) {
        this.maxTimePerAttemptMin = maxTimePerAttemptMin;
    }

    public SubscriptionPlanStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionPlanStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public UUID getReplacedByPlanId() {
        return replacedByPlanId;
    }

    public void setReplacedByPlanId(UUID replacedByPlanId) {
        this.replacedByPlanId = replacedByPlanId;
    }

    public static SubscriptionPlan create(String name, String tagline, BigDecimal priceVnd, SubscriptionPlanPeriod periodType, Integer periodCount, Integer maxTimePerAttemptMin, Instant now, UUID createdBy) {
        return new SubscriptionPlan(name, tagline, priceVnd, periodType, periodCount, maxTimePerAttemptMin, SubscriptionPlanStatus.DRAFT, null, now, now, createdBy, createdBy);
    }

    /**
     * Thời điểm hết hạn nếu gói bắt đầu chạy từ {@code start}.
     *
     * <p>Cộng theo LỊCH chứ không theo số ngày cố định: gói 1 tháng mua ngày 31/01 phải hết hạn
     * 28/02, còn cộng 30 ngày thì ra 02/03 -- trường dùng thừa hai ngày ở mỗi tháng ngắn. Đây cũng
     * chính là lý do periodType/periodCount thay cho validityDays cũ.
     *
     * <p>Quy về múi giờ nghiệp vụ trước khi cộng vì "một tháng" là khái niệm theo lịch địa phương:
     * cộng trên UTC sẽ lệch ngày với mọi mốc rơi vào 17:00-23:59 giờ Việt Nam.
     */
    public Instant endDateFrom(Instant start) {
        if (start == null || periodType == null || periodCount == null) {
            throw new IllegalStateException("Gói thiếu chu kỳ (periodType/periodCount), không tính được hạn dùng");
        }
        var local = start.atZone(ZoneConstant.BUSINESS_ZONE);
        var end = switch (periodType) {
            case DAY -> local.plusDays(periodCount);
            case MONTH -> local.plusMonths(periodCount);
            case YEAR -> local.plusYears(periodCount);
        };
        return end.toInstant();
    }

}

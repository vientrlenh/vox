package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SchoolBalance {
    private UUID id;
    private UUID schoolId;
    private BigDecimal grantedVnd;
    private BigDecimal purchasedVnd;
    private Instant createdAt;
    private Instant updatedAt;
    // Phải mang theo ở domain model, không chỉ ở JpaEntity: mapper dựng entity MỚI mỗi lần lưu nên
    // entity luôn detached -- thiếu version, Hibernate coi là transient và INSERT đè lên id đã có.
    private Long version;

    public SchoolBalance() {}

    public SchoolBalance(UUID id, UUID schoolId, BigDecimal grantedVnd, BigDecimal purchasedVnd, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.schoolId = schoolId;
        this.grantedVnd = grantedVnd;
        this.purchasedVnd = purchasedVnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public SchoolBalance(UUID schoolId, BigDecimal grantedVnd, BigDecimal purchasedVnd, Instant createdAt, Instant updatedAt) {
        this.schoolId = schoolId;
        this.grantedVnd = grantedVnd;
        this.purchasedVnd = purchasedVnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public BigDecimal getGrantedVnd() {
        return grantedVnd;
    }

    public void setGrantedVnd(BigDecimal grantedVnd) {
        this.grantedVnd = grantedVnd;
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

    public BigDecimal getPurchasedVnd() {
        return purchasedVnd;
    }

    public void setPurchasedVnd(BigDecimal purchasedVnd) {
        this.purchasedVnd = purchasedVnd;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Số dư khả dụng = phần kèm gói + phần tự nạp. Đây là con số DUY NHẤT dùng để trả lời "trường
     * còn dùng được không" -- không nơi nào được so sánh riêng lẻ từng cột.
     */
    public BigDecimal getAvailableVnd() {
        return grantedVnd.add(purchasedVnd);
    }

    /** Nợ nằm ở purchasedVnd âm (grantedVnd đã có CHECK >= 0). */
    public boolean isInDebt() {
        return getAvailableVnd().compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getAvailableAmountVnd() {
        return grantedVnd.subtract(purchasedVnd);
    }
}

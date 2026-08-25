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

    public SchoolBalance() {}

    public SchoolBalance(UUID id, UUID schoolId, BigDecimal grantedVnd, BigDecimal purchasedVnd, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.grantedVnd = grantedVnd;
        this.purchasedVnd = purchasedVnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public BigDecimal getAvailableAmountVnd() {
        return grantedVnd.subtract(purchasedVnd);
    }
}

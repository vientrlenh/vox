package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SchoolBalance {
    private UUID id;
    private UUID schoolId;
    private BigDecimal amountVnd;
    private Instant createdAt;
    private Instant updatedAt;

    public SchoolBalance() {}

    public SchoolBalance(UUID id, UUID schoolId, BigDecimal amountVnd, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.amountVnd = amountVnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public SchoolBalance(UUID schoolId, BigDecimal amountVnd, Instant createdAt, Instant updatedAt) {
        this.schoolId = schoolId;
        this.amountVnd = amountVnd;
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

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public void setAmountVnd(BigDecimal amountVnd) {
        this.amountVnd = amountVnd;
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

    
}

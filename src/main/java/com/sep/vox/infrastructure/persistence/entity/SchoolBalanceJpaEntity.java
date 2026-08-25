package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class SchoolBalanceJpaEntity {
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "granted_vnd", nullable = false, check = {
        @CheckConstraint(
            name = "chk_school_balances_granted_vnd_non_negative", 
            constraint = "granted_vnd >= 0"
        )
    })
    private BigDecimal grantedVnd;

    @Column(name = "purchased_vnd", nullable = false)
    private BigDecimal purchasedVnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SchoolBalanceJpaEntity() {}

    

    public SchoolBalanceJpaEntity(UUID id, UUID schoolId, BigDecimal grantedVnd, BigDecimal purchasedVnd,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
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

    public BigDecimal getPurchasedVnd() {
        return purchasedVnd;
    }

    public void setPurchasedVnd(BigDecimal purchasedVnd) {
        this.purchasedVnd = purchasedVnd;
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

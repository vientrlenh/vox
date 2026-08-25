package com.sep.vox.domain.model.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Order {
    private UUID id;
    private UUID schoolId;
    private OrderType type;
    private String description;
    private BigDecimal totalAmountVnd;
    private BigDecimal chargedFeeVnd;
    private BigDecimal discountAmountVnd;
    private OrderStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    // Phải mang theo ở domain model, không chỉ ở JpaEntity: mapper dựng entity MỚI mỗi lần lưu nên
    // entity luôn detached -- thiếu version, Hibernate coi là transient và INSERT đè lên id đã có.
    private Long version;

    public Order() {}

    public Order(UUID id, UUID schoolId, OrderType type, String description, BigDecimal totalAmountVnd, BigDecimal chargedFeeVnd,
            BigDecimal discountAmountVnd, OrderStatus status, String notes, Instant createdAt, Instant updatedAt, UUID createdBy,
            UUID updatedBy, Long version) {
        this.id = id;
        this.schoolId = schoolId;
        this.type = type; 
        this.description = description;
        this.totalAmountVnd = totalAmountVnd;
        this.chargedFeeVnd = chargedFeeVnd;
        this.discountAmountVnd = discountAmountVnd;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
    }

    public Order(UUID schoolId, OrderType type, String description, BigDecimal totalAmountVnd, BigDecimal chargedFeeVnd,
            BigDecimal discountAmountVnd, OrderStatus status, String notes, Instant createdAt, Instant updatedAt, UUID createdBy,
            UUID updatedBy) {
        this.schoolId = schoolId;
        this.type = type;
        this.description = description;
        this.totalAmountVnd = totalAmountVnd;
        this.chargedFeeVnd = chargedFeeVnd;
        this.discountAmountVnd = discountAmountVnd;
        this.status = status;
        this.notes = notes;
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

    public UUID getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public BigDecimal getTotalAmountVnd() {
        return totalAmountVnd;
    }

    public void setTotalAmountVnd(BigDecimal totalAmountVnd) {
        this.totalAmountVnd = totalAmountVnd;
    }

    public BigDecimal getChargedFeeVnd() {
        return chargedFeeVnd;
    }

    public void setChargedFeeVnd(BigDecimal chargedFeeVnd) {
        this.chargedFeeVnd = chargedFeeVnd;
    }

    public BigDecimal getDiscountAmountVnd() {
        return discountAmountVnd;
    }

    public void setDiscountAmountVnd(BigDecimal discountAmountVnd) {
        this.discountAmountVnd = discountAmountVnd;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    
}

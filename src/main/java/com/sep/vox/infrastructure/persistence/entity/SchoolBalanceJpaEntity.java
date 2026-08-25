package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "school_balances")
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

    // Hạn mức KÈM GÓI: bị ghi giảm về 0 mỗi lần gia hạn. Không bao giờ âm -- mọi phần vượt quá đều
    // rơi xuống purchased_vnd, xem câu UPDATE trừ số dư trong SpringDataSchoolBalanceRepository.
    @Column(name = "granted_vnd", nullable = false, precision = 18, scale = 6, check = {
        @CheckConstraint(
            name = "chk_school_balances_granted_vnd_non_negative",
            constraint = "granted_vnd >= 0"
        )
    })
    private BigDecimal grantedVnd;

    // Hạn mức trường TỰ NẠP: không hết hạn. CỐ Ý không có CHECK >= 0 -- phần âm ở đây CHÍNH LÀ nợ
    // (thay cho điều kiện used_quantity > total_allocated cũ), phát sinh khi QuotaType cho phép ghi
    // nợ (GRADING/CLASS_TEST) tiêu vượt số dư.
    @Column(name = "purchased_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal purchasedVnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Cả trường dồn vào ĐÚNG MỘT dòng này, nên mọi lần trừ số dư đều tranh chấp nhau. @Version là
    // lớp bảo vệ cho đường ghi qua JPA; đường trừ quota nóng phải dùng UPDATE ... WHERE có điều kiện
    // (xem SpringDataSchoolBalanceRepository.tryDebit) chứ KHÔNG đọc-rồi-ghi.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SchoolBalanceJpaEntity() {}

    public SchoolBalanceJpaEntity(UUID id, UUID schoolId, BigDecimal grantedVnd, BigDecimal purchasedVnd,
            Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.schoolId = schoolId;
        this.grantedVnd = grantedVnd;
        this.purchasedVnd = purchasedVnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

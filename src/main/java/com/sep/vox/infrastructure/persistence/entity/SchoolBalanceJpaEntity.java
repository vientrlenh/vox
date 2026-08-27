package com.sep.vox.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Ví tiền TỰ NẠP của trường. Hạn mức kèm gói KHÔNG nằm ở đây mà ở
 * school_subscription_quota_records -- tách theo từng QuotaType, nên gộp vào một số dư cấp trường
 * duy nhất sẽ xóa mất giới hạn theo loại.
 */
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

    // Tiền trường TỰ NẠP: không hết hạn. CỐ Ý không có CHECK >= 0 -- phần âm ở đây CHÍNH LÀ nợ, thay
    // cho điều kiện used_quantity > total_allocated cũ. Nợ là chuyện của TRƯỜNG, không tách theo
    // QuotaType nữa -- xem SchoolBalance / SchoolSubscriptionDebtGuardService.
    @Column(name = "balance_vnd", nullable = false, precision = 18, scale = 6)
    private BigDecimal balanceVnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Cả trường dồn vào ĐÚNG MỘT dòng này, nên mọi lần trừ số dư đều tranh chấp nhau. @Version chỉ là
    // lớp bảo vệ cuối; thứ thật sự tuần tự hóa các lần ghi là khóa hàng của
    // SchoolBalanceRepository.findBySchoolIdForUpdateOrCreate -- CỐ Ý không có UPDATE ... WHERE kiểu
    // trừ tại chỗ, vì bút toán đi kèm cần balance_after_vnd mà một câu như vậy không trả ra được.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected SchoolBalanceJpaEntity() {}

    public SchoolBalanceJpaEntity(UUID id, UUID schoolId, BigDecimal balanceVnd,
            Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.schoolId = schoolId;
        this.balanceVnd = balanceVnd;
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

    public BigDecimal getBalanceVnd() {
        return balanceVnd;
    }

    public void setBalanceVnd(BigDecimal balanceVnd) {
        this.balanceVnd = balanceVnd;
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

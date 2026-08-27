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
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Bảng tên SỐ NHIỀU bắt buộc: {@code order} là từ khóa SQL, không dùng làm tên bảng trần được.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(columnList = "school_id", name = "idx_orders_school_id")
}, check = {
    @CheckConstraint(
        name = "chk_orders_total_amount_vnd_matches_amount_combination", 
        constraint = "total_amount_vnd = subtotal_amount_vnd + charged_fee_vnd - discount_amount_vnd"
    ), 
    @CheckConstraint(
        name = "chk_orders_discount_amount_vnd_lower_or_equals_than_subtotal_and_charged_fee", 
        constraint = "discount_amount_vnd <= subtotal_amount_vnd + charged_fee_vnd"
    )
})
public class OrderJpaEntity {

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

    @Column(name = "type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_orders_type_valid",
            constraint = "type IN ('SUBSCRIPTION_REQUEST', 'SUBSCRIPTION_UPGRADE', 'TOPUP')"
        )
    })
    private String type;

    // Mô tả hiển thị cho trường (và đẩy sang cổng làm nội dung chuyển khoản) -- chốt lại lúc tạo đơn
    // để hóa đơn in ra sau này không đổi theo tên gói hiện tại.
    @Column(name = "description", updatable = false, length = 512)
    private String description;

    // Tiền hàng TRƯỚC phí và giảm giá; với đơn TOPUP đây chính là số dư trường nhận được.
    @Column(name = "subtotal_amount_vnd", nullable = false, precision = 15, scale = 0, check = {
        @CheckConstraint(
            name = "chk_orders_subtotal_amount_vnd_non_negative", 
            constraint = "subtotal_amount_vnd >= 0"
        )
    })
    private BigDecimal subtotalAmountVnd;

    // numeric(15,0): đây là tiền THẬT đi qua cổng thanh toán, PayOS/SePay không giao dịch được số lẻ
    // đồng. Khác school_balance_entries.amount_vnd -- bên đó là số dư đo tiêu dùng nên cần phần thập phân.
    @Column(name = "total_amount_vnd", nullable = false, precision = 15, scale = 0, check = {
        @CheckConstraint(
            name = "chk_orders_total_amount_vnd_non_negative", 
            constraint = "total_amount_vnd >= 0"
        )
    })
    private BigDecimal totalAmountVnd;

    @Column(name = "charged_fee_vnd", nullable = false, precision = 15, scale = 0, check = {
        @CheckConstraint(
            name = "chk_orders_charged_fee_vnd_non_negative", 
            constraint = "charged_fee_vnd >= 0"
        )
    })
    private BigDecimal chargedFeeVnd;

    @Column(name = "discount_amount_vnd", nullable = false, precision = 15, scale = 0, check = {
        @CheckConstraint(
            name = "chk_orders_discount_amount_vnd_non_negative", 
            constraint = "discount_amount_vnd >= 0"
        )
    })
    private BigDecimal discountAmountVnd;

    @Column(name = "status", nullable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_orders_status_valid",
            constraint = "status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED')"
        )
    })
    private String status;

    // Ghi chú nội bộ của System Admin (vd lý do hủy/từ chối đơn) -- KHÔNG updatable=false vì đây là
    // phần duy nhất của đơn được phép sửa sau khi tạo.
    @Column(name = "notes", length = 2048)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // updatable = false: hạn chót là cam kết chốt lúc tạo đơn và đã gửi sang cổng -- muốn gia hạn
    // thì hủy đơn rồi đặt lại, không sửa ngầm hạn của đơn đang treo.
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Webhook cổng và PendingOrderReconciler có thể cùng chuyển một đơn sang SUCCESS cách nhau vài
    // phút -- @Version để lần ghi thứ hai thất bại thay vì ghi đè im lặng.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected OrderJpaEntity() {}

    public OrderJpaEntity(UUID id, UUID schoolId, String type, String description, BigDecimal subtotalAmountVnd,
            BigDecimal totalAmountVnd,
            BigDecimal chargedFeeVnd, BigDecimal discountAmountVnd, String status, String notes, Instant createdAt,
            Instant updatedAt, Instant expiresAt, UUID createdBy, UUID updatedBy, Long version) {
        this.id = id;
        this.schoolId = schoolId;
        this.type = type;
        this.description = description;
        this.subtotalAmountVnd = subtotalAmountVnd;
        this.totalAmountVnd = totalAmountVnd;
        this.chargedFeeVnd = chargedFeeVnd;
        this.discountAmountVnd = discountAmountVnd;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public BigDecimal getSubtotalAmountVnd() {
        return subtotalAmountVnd;
    }

    public void setSubtotalAmountVnd(BigDecimal subtotalAmountVnd) {
        this.subtotalAmountVnd = subtotalAmountVnd;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}

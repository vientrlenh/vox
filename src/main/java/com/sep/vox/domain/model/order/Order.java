package com.sep.vox.domain.model.order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Order {

    /**
     * Đơn treo bao lâu thì coi như bỏ. 24 giờ vì trường chủ yếu trả bằng chuyển khoản ngân hàng --
     * đặt đơn cuối giờ chiều thì kế toán sang sáng hôm sau mới duyệt chi.
     *
     * <p>Hằng số ở model chứ không phải config: đây là con số ĐI KÈM từng đơn (ghi vào expires_at lúc
     * tạo và gửi sang cổng), nên đổi nó chỉ ảnh hưởng đơn mới. Nếu về sau cần chỉnh theo môi trường
     * thì chuyển thành property và truyền vào factory, đừng đọc config lúc kiểm tra hạn -- làm vậy là
     * đổi hạn của cả những đơn đang treo.
     */
    public static final Duration PENDING_TTL = Duration.ofHours(24);

    private UUID id;
    private UUID schoolId;
    private OrderType type;
    private String description;
    // Tiền hàng TRƯỚC phí và giảm giá. Bất biến: totalAmountVnd = subtotalAmountVnd + chargedFeeVnd
    // - discountAmountVnd (chk_orders_amounts_balance dưới DB giữ luôn cho chắc).
    private BigDecimal subtotalAmountVnd;
    private BigDecimal totalAmountVnd;
    private BigDecimal chargedFeeVnd;
    private BigDecimal discountAmountVnd;
    private OrderStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    // Hạn chót trả tiền, chốt lúc tạo đơn -- xem cột expires_at trong V2.
    private Instant expiresAt;
    private UUID createdBy;
    private UUID updatedBy;
    // Phải mang theo ở domain model, không chỉ ở JpaEntity: mapper dựng entity MỚI mỗi lần lưu nên
    // entity luôn detached -- thiếu version, Hibernate coi là transient và INSERT đè lên id đã có.
    private Long version;

    public Order() {}

    public Order(UUID id, UUID schoolId, OrderType type, String description, BigDecimal subtotalAmountVnd,
            BigDecimal totalAmountVnd, BigDecimal chargedFeeVnd,
            BigDecimal discountAmountVnd, OrderStatus status, String notes, Instant createdAt, Instant updatedAt, Instant expiresAt, UUID createdBy,
            UUID updatedBy, Long version) {
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

    public Order(UUID schoolId, OrderType type, String description, BigDecimal subtotalAmountVnd,
            BigDecimal totalAmountVnd, BigDecimal chargedFeeVnd,
            BigDecimal discountAmountVnd, OrderStatus status, String notes, Instant createdAt, Instant updatedAt, Instant expiresAt, UUID createdBy,
            UUID updatedBy) {
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

    public BigDecimal getSubtotalAmountVnd() {
        return subtotalAmountVnd;
    }

    public void setSubtotalAmountVnd(BigDecimal subtotalAmountVnd) {
        this.subtotalAmountVnd = subtotalAmountVnd;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
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

    /**
     * Đơn mua một chu kỳ gói. Tiền đơn đúng bằng giá niêm yết của gói, KHÔNG cộng phí dịch vụ: biên
     * lãi của gói đã nằm sẵn trong priceVnd do admin tự đặt (bán 10 triệu cho 8 triệu hạn mức).
     */
    public static Order forSubscription(UUID schoolId, OrderType type, String description, BigDecimal planPriceVnd,
            Instant now, UUID createdBy) {
        return new Order(schoolId, type, description, planPriceVnd, planPriceVnd, BigDecimal.ZERO, BigDecimal.ZERO,
            OrderStatus.PENDING, null, now, now, now.plus(PENDING_TTL), createdBy, createdBy);
    }

    /**
     * Đơn nạp thêm vào số dư. Phí dịch vụ cộng THÊM chứ không trích ra: trường nhận đúng
     * creditAmountVnd vào ví và trả creditAmountVnd + serviceFeeVnd.
     *
     * <p>Số dư được cộng chính là subtotalAmountVnd, GHI THẲNG chứ không suy ra từ total - fee: đơn
     * nạp thêm không có order_items nào để cộng lại, nên cách suy ngược đó là nguồn duy nhất -- và nó
     * sai ngay khi discountAmountVnd khác 0, sai về phía cộng dư tiền cho trường.
     */
    public static Order forTopUp(UUID schoolId, String description, BigDecimal creditAmountVnd,
            BigDecimal serviceFeeVnd, Instant now, UUID createdBy) {
        return new Order(schoolId, OrderType.TOPUP, description, creditAmountVnd,
            creditAmountVnd.add(serviceFeeVnd), serviceFeeVnd,
            BigDecimal.ZERO, OrderStatus.PENDING, null, now, now, now.plus(PENDING_TTL), createdBy, createdBy);
    }
}

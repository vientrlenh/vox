package com.sep.vox.domain.model.subscription;

import java.time.Instant;
import java.util.UUID;

/**
 * Sổ audit append-only cho những lần System Admin can thiệp vào vòng đời một gói đăng ký.
 *
 * <p>Cần một bảng riêng vì ba cột {@code suspendedAt/suspendedReason/suspendedBy} trên
 * SchoolSubscription là TRẠNG THÁI, không phải LỊCH SỬ: gỡ đình chỉ xóa sạch cả ba về null, nên sau
 * đó không còn dấu vết nào cho thấy trường từng bị đình chỉ, ai làm, vì sao. Với một thao tác cưỡng
 * chế nhắm vào khách hàng đang trả tiền thì mất dấu vết là không chấp nhận được.
 *
 * <p>KHÔNG dùng lại financial_event: bảng đó sinh ra cho tiền, có amount_signed/currency/payment_method
 * đều NOT NULL, nên mỗi lần đình chỉ phải nhét vào đó một khoản 0 VND trả bằng "MANUAL" -- ba con số
 * vô nghĩa chỉ để thỏa mãn ràng buộc. Phần TIỀN của bảng đó giờ đã thuộc về orders/payment_records/
 * invoices/school_balance_entries; phần vòng đời thì thuộc về đây.
 *
 * <p>Đi theo đúng lối school_debt_event đã mở: mỗi mối quan tâm một bảng sự kiện, cột nào cũng có
 * nghĩa với mọi dòng.
 */
public class SchoolSubscriptionEvent {
    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId;
    private SchoolSubscriptionEventType eventType;
    /** Người thực hiện -- luôn là System Admin, không bao giờ null. */
    private UUID actorId;
    /** Bắt buộc với SUSPENDED; với UNSUSPENDED là ghi chú tùy chọn. */
    private String reason;
    private Instant occurredAt;

    public SchoolSubscriptionEvent() {}

    public SchoolSubscriptionEvent(UUID id, UUID schoolId, UUID subscriptionId,
            SchoolSubscriptionEventType eventType, UUID actorId, String reason, Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.eventType = eventType;
        this.actorId = actorId;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public SchoolSubscriptionEvent(UUID schoolId, UUID subscriptionId,
            SchoolSubscriptionEventType eventType, UUID actorId, String reason, Instant occurredAt) {
        this(null, schoolId, subscriptionId, eventType, actorId, reason, occurredAt);
    }

    public static SchoolSubscriptionEvent suspended(UUID schoolId, UUID subscriptionId, UUID actorId,
            String reason, Instant now) {
        return new SchoolSubscriptionEvent(schoolId, subscriptionId,
            SchoolSubscriptionEventType.SUSPENDED, actorId, reason, now);
    }

    public static SchoolSubscriptionEvent unsuspended(UUID schoolId, UUID subscriptionId, UUID actorId,
            String note, Instant now) {
        return new SchoolSubscriptionEvent(schoolId, subscriptionId,
            SchoolSubscriptionEventType.UNSUSPENDED, actorId, note, now);
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

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public SchoolSubscriptionEventType getEventType() {
        return eventType;
    }

    public void setEventType(SchoolSubscriptionEventType eventType) {
        this.eventType = eventType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}

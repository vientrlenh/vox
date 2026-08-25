package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Sổ cái append-only của SchoolBalance. SchoolBalance chỉ là bản TỔNG HỢP để trừ nguyên tử; nguồn
 * sự thật là bảng này -- SUM(amountVnd) phải luôn khớp số dư tổng hợp.
 *
 * <p>Tham chiếu nguồn dùng cột CÓ KIỂU (orderId / examSessionId / actorId) thay cho cặp
 * (source_type, source_id) đa hình như invoice cũ: mỗi cột FK được thật, và CHECK ràng buộc đúng
 * cột nào được set theo entryType.
 */
public class SchoolBalanceEntry {

    private UUID id;
    private UUID schoolId;
    /** Gói đang ACTIVE lúc phát sinh -- CHỈ để truy vết, số dư không thuộc về gói nào. */
    private UUID subscriptionId;
    private SchoolBalanceEntryType entryType;
    /** Dương = nạp/hoàn/điều chỉnh tăng, âm = trừ. */
    private BigDecimal amountVnd;
    /** Số dư sau bút toán -- dựng lại sao kê không cần cộng dồn từ đầu. */
    private BigDecimal balanceAfterVnd;
    /** Đơn hàng nguồn: bắt buộc với TOP_UP/REFUND. */
    private UUID orderId;
    /** Phiên thi/luyện nói đã gây ra khoản trừ: bắt buộc với OVERAGE_CHARGE. */
    private UUID examSessionId;
    /** Chiều BÁO CÁO cho ViewTokenUsageTimeseries, không phải ví riêng. */
    private QuotaType quotaType;
    /** Chi phí GỐC nhà cung cấp tính (Azure), giữ nguyên USD để đối soát ngược với ai_usage_records. */
    private BigDecimal costUsd;
    /** Tỷ giá đã dùng để quy đổi costUsd sang amountVnd -- chốt lại vì tỷ giá đổi hằng ngày. */
    private BigDecimal fxRateUsed;
    /** Người thực hiện: bắt buộc với ADJUSTMENT. */
    private UUID actorId;
    private String reason;
    private Instant occurredAt;

    public SchoolBalanceEntry() {}

    public SchoolBalanceEntry(UUID id, UUID schoolId, UUID subscriptionId, SchoolBalanceEntryType entryType,
            BigDecimal amountVnd, BigDecimal balanceAfterVnd, UUID orderId, UUID examSessionId, QuotaType quotaType,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID actorId, String reason, Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.entryType = entryType;
        this.amountVnd = amountVnd;
        this.balanceAfterVnd = balanceAfterVnd;
        this.orderId = orderId;
        this.examSessionId = examSessionId;
        this.quotaType = quotaType;
        this.costUsd = costUsd;
        this.fxRateUsed = fxRateUsed;
        this.actorId = actorId;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public SchoolBalanceEntry(UUID schoolId, UUID subscriptionId, SchoolBalanceEntryType entryType,
            BigDecimal amountVnd, BigDecimal balanceAfterVnd, UUID orderId, UUID examSessionId, QuotaType quotaType,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID actorId, String reason, Instant occurredAt) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.entryType = entryType;
        this.amountVnd = amountVnd;
        this.balanceAfterVnd = balanceAfterVnd;
        this.orderId = orderId;
        this.examSessionId = examSessionId;
        this.quotaType = quotaType;
        this.costUsd = costUsd;
        this.fxRateUsed = fxRateUsed;
        this.actorId = actorId;
        this.reason = reason;
        this.occurredAt = occurredAt;
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

    public SchoolBalanceEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(SchoolBalanceEntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public void setAmountVnd(BigDecimal amountVnd) {
        this.amountVnd = amountVnd;
    }

    public BigDecimal getBalanceAfterVnd() {
        return balanceAfterVnd;
    }

    public void setBalanceAfterVnd(BigDecimal balanceAfterVnd) {
        this.balanceAfterVnd = balanceAfterVnd;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getCostUsd() {
        return costUsd;
    }

    public void setCostUsd(BigDecimal costUsd) {
        this.costUsd = costUsd;
    }

    public BigDecimal getFxRateUsed() {
        return fxRateUsed;
    }

    public void setFxRateUsed(BigDecimal fxRateUsed) {
        this.fxRateUsed = fxRateUsed;
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

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
    /** Phiên THI đã gây ra khoản trừ -- OVERAGE_CHARGE phải có ĐÚNG MỘT trong hai cột session. */
    private UUID examSessionId;
    /** Phiên LUYỆN NÓI đã gây ra khoản trừ -- cột riêng vì mỗi cột session là một FK được thật. */
    private UUID practiceSessionId;
    /** Chiều BÁO CÁO cho ViewTokenUsageTimeseries, không phải ví riêng. */
    private QuotaType quotaType;
    /** Chi phí GỐC nhà cung cấp tính (Azure), giữ nguyên USD để đối soát ngược với ai_usage_records. */
    private BigDecimal costUsd;
    /** Tỷ giá đã dùng để quy đổi costUsd sang amountVnd -- chốt lại vì tỷ giá đổi hằng ngày. */
    private BigDecimal fxRateUsed;
    /** Người thực hiện: bắt buộc với ADJUSTMENT/ALLOCATION_DRAW. */
    private UUID actorId;
    /** Người ĐƯỢC cấp/hạ hạn mức: bắt buộc với ALLOCATION_DRAW, null với mọi loại khác. */
    private UUID targetUserId;
    private String reason;
    private Instant occurredAt;

    public SchoolBalanceEntry() {}

    public SchoolBalanceEntry(UUID id, UUID schoolId, UUID subscriptionId, SchoolBalanceEntryType entryType,
            BigDecimal amountVnd, BigDecimal balanceAfterVnd, UUID orderId, UUID examSessionId,
            UUID practiceSessionId, QuotaType quotaType,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID actorId, UUID targetUserId, String reason,
            Instant occurredAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.entryType = entryType;
        this.amountVnd = amountVnd;
        this.balanceAfterVnd = balanceAfterVnd;
        this.orderId = orderId;
        this.examSessionId = examSessionId;
        this.practiceSessionId = practiceSessionId;
        this.quotaType = quotaType;
        this.costUsd = costUsd;
        this.fxRateUsed = fxRateUsed;
        this.actorId = actorId;
        this.targetUserId = targetUserId;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public SchoolBalanceEntry(UUID schoolId, UUID subscriptionId, SchoolBalanceEntryType entryType,
            BigDecimal amountVnd, BigDecimal balanceAfterVnd, UUID orderId, UUID examSessionId,
            UUID practiceSessionId, QuotaType quotaType,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID actorId, UUID targetUserId, String reason,
            Instant occurredAt) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.entryType = entryType;
        this.amountVnd = amountVnd;
        this.balanceAfterVnd = balanceAfterVnd;
        this.orderId = orderId;
        this.examSessionId = examSessionId;
        this.practiceSessionId = practiceSessionId;
        this.quotaType = quotaType;
        this.costUsd = costUsd;
        this.fxRateUsed = fxRateUsed;
        this.actorId = actorId;
        this.targetUserId = targetUserId;
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

    public UUID getPracticeSessionId() {
        return practiceSessionId;
    }

    public void setPracticeSessionId(UUID practiceSessionId) {
        this.practiceSessionId = practiceSessionId;
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

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
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

    /**
     * Bút toán NẠP TIỀN từ một đơn đã thu đủ.
     *
     * <p>Số cộng vào ví là {@code subtotalAmountVnd} của đơn, KHÔNG phải total: phần phí dịch vụ
     * (charged_fee_vnd) là tiền công của mình, trường không được tiêu lại. Xem Order.forTopUp.
     *
     * <p>quotaType/costUsd/fxRateUsed đều null: đây là tiền vào, chưa gắn với lượt dùng nào -- ba
     * cột đó chỉ có nghĩa với OVERAGE_CHARGE.
     */
    public static SchoolBalanceEntry forTopUp(UUID schoolId, UUID subscriptionId, UUID orderId,
            BigDecimal creditedAmountVnd, BigDecimal balanceAfterVnd, Instant now) {
        return new SchoolBalanceEntry(
            schoolId,
            subscriptionId,
            SchoolBalanceEntryType.TOP_UP,
            creditedAmountVnd,
            balanceAfterVnd,
            orderId,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            now
        );
    }

    /**
     * Bút toán TRÍCH/HOÀN ví tự nạp do cấp/hạ hạn mức cá nhân vượt pool
     * (DistributeQuotaToUsersService.computeManualAmounts).
     *
     * <p>Nhận thẳng {@code deltaVnd} đã có dấu đúng -- KHÔNG auto-negate như
     * {@link #overageCharge}: chiều này có thể dương (hạ hạn mức, hoàn lại ví) hoặc âm (cấp hạn mức,
     * ăn vào ví) tuỳ nghiệp vụ, không phải lúc nào cũng trừ như OVERAGE_CHARGE.
     */
    public static SchoolBalanceEntry forAllocationDraw(UUID schoolId, UUID subscriptionId,
            QuotaType quotaType, UUID targetUserId, UUID actorId, BigDecimal deltaVnd,
            BigDecimal balanceAfterVnd, String reason, Instant now) {
        return new SchoolBalanceEntry(
            schoolId,
            subscriptionId,
            SchoolBalanceEntryType.ALLOCATION_DRAW,
            deltaVnd,
            balanceAfterVnd,
            null,
            null,
            null,
            quotaType,
            null,
            null,
            actorId,
            targetUserId,
            reason,
            now
        );
    }

    /**
     * Bút toán TRỪ phần chi phí AI vượt quá hạn mức kèm gói, cho một phiên THI.
     *
     * <p>{@code overageVnd} là phần VƯỢT, không phải cả khoản chi: phần còn nằm trong hạn mức đã được
     * ghi ở school_subscription_quota_records.used_amount_vnd rồi, ghi lại ở đây là đếm hai lần cùng
     * một đồng tiền -- xem ConsumeQuotaService.chargeOverage.
     */
    public static SchoolBalanceEntry forExamOverageCharge(UUID schoolId, UUID subscriptionId,
            UUID examSessionId, QuotaType quotaType, BigDecimal overageVnd, BigDecimal balanceAfterVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, Instant now) {
        return overageCharge(schoolId, subscriptionId, examSessionId, null, quotaType,
            overageVnd, balanceAfterVnd, costUsd, fxRateUsed, now);
    }

    /** Như {@link #forExamOverageCharge} nhưng khoản trừ đến từ một phiên LUYỆN NÓI. */
    public static SchoolBalanceEntry forPracticeOverageCharge(UUID schoolId, UUID subscriptionId,
            UUID practiceSessionId, QuotaType quotaType, BigDecimal overageVnd, BigDecimal balanceAfterVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, Instant now) {
        return overageCharge(schoolId, subscriptionId, null, practiceSessionId, quotaType,
            overageVnd, balanceAfterVnd, costUsd, fxRateUsed, now);
    }

    /**
     * Truyền vào số DƯƠNG, factory tự đảo dấu: chk_school_balance_entries_overage_traceable đòi
     * amount_vnd &lt; 0, và bắt chỗ gọi tự nhớ .negate() là chừa sẵn một chỗ để quên. Cùng ràng buộc
     * đó đòi quotaType/costUsd/fxRateUsed NOT NULL và ĐÚNG MỘT trong hai cột session được set -- gom
     * vào đây để "cột nào bắt buộc" chỉ có một nơi định nghĩa, giống forTopUp.
     *
     * <p>private, và hai factory công khai ở trên mỗi cái chỉ set được một cột session: để lộ cả hai
     * tham số ra ngoài là để lộ luôn khả năng gọi với cả hai null (hoặc cả hai non-null), tức là dựng
     * sẵn một dòng chắc chắn bị DB từ chối.
     */
    private static SchoolBalanceEntry overageCharge(UUID schoolId, UUID subscriptionId,
            UUID examSessionId, UUID practiceSessionId, QuotaType quotaType, BigDecimal overageVnd,
            BigDecimal balanceAfterVnd, BigDecimal costUsd, BigDecimal fxRateUsed, Instant now) {
        return new SchoolBalanceEntry(
            schoolId,
            subscriptionId,
            SchoolBalanceEntryType.OVERAGE_CHARGE,
            overageVnd.negate(),
            balanceAfterVnd,
            null,
            examSessionId,
            practiceSessionId,
            quotaType,
            costUsd,
            fxRateUsed,
            null,
            null,
            null,
            now
        );
    }
}

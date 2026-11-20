package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Một lần trường bị trừ chi phí AI. Sổ chỉ-ghi-thêm: không sửa, không xoá.
 *
 * <p>Trả lời câu mà hai sổ tiền còn lại không trả lời được — trường đã tiêu bao nhiêu, theo NGÀY, và
 * của AI:
 *
 * <ul>
 *   <li>{@code ai_usage_records} là giá vốn của NỀN TẢNG, và từ V9 nó cố ý lệch khỏi số trường bị
 *       thu (khoản của lượt chấm hỏng được miễn). Nó cũng không biết gì về đường luyện nói.
 *   <li>{@code school_balance_entries} chỉ ghi phần tiêu VƯỢT hạn mức, nên cộng lại thì thiếu đúng
 *       phần lớn nhất.
 * </ul>
 *
 * <p>Ghi ĐỦ số tiền của lần trừ, gồm cả phần nằm gọn trong hạn mức.
 */
public class SchoolAiSpendEntry {

    private UUID id;
    private UUID schoolId;
    private UUID subscriptionId;
    private QuotaType quotaType;
    private UUID userId;
    private UUID examSessionId;
    private UUID practiceSessionId;
    private BigDecimal amountVnd;
    private Instant occurredAt;

    public SchoolAiSpendEntry() {
    }

    private SchoolAiSpendEntry(UUID schoolId, UUID subscriptionId, QuotaType quotaType, UUID userId,
            UUID examSessionId, UUID practiceSessionId, BigDecimal amountVnd, Instant occurredAt) {
        this.schoolId = schoolId;
        this.subscriptionId = subscriptionId;
        this.quotaType = quotaType;
        this.userId = userId;
        this.examSessionId = examSessionId;
        this.practiceSessionId = practiceSessionId;
        this.amountVnd = amountVnd;
        this.occurredAt = occurredAt;
    }

    /**
     * Khoản chi của một phiên THI.
     *
     * @param userId người ra đề bài kiểm tra trên lớp, hoặc {@code null} với kỳ thi tập trung — kỳ do
     *               nhà trường tổ chức nên không thuộc trần chi của ai. null ở đây là một CÂU TRẢ
     *               LỜI, không phải dữ liệu thiếu.
     */
    public static SchoolAiSpendEntry forExam(UUID schoolId, UUID subscriptionId, UUID examSessionId,
            UUID userId, BigDecimal amountVnd, Instant occurredAt) {
        return new SchoolAiSpendEntry(schoolId, subscriptionId, QuotaType.EXAM, userId,
            examSessionId, null, amountVnd, occurredAt);
    }

    /** Khoản chi của một phiên LUYỆN NÓI — luôn thuộc về học sinh đã nói. */
    public static SchoolAiSpendEntry forPractice(UUID schoolId, UUID subscriptionId,
            UUID practiceSessionId, UUID userId, BigDecimal amountVnd, Instant occurredAt) {
        return new SchoolAiSpendEntry(schoolId, subscriptionId, QuotaType.PRACTICE, userId,
            null, practiceSessionId, amountVnd, occurredAt);
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

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public UUID getPracticeSessionId() {
        return practiceSessionId;
    }

    public BigDecimal getAmountVnd() {
        return amountVnd;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

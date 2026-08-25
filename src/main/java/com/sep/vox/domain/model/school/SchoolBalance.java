package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Ví tiền TỰ NẠP của trường. CHỈ chứa tiền trường bỏ ra mua, không chứa hạn mức kèm gói --
 * hạn mức kèm gói nằm ở school_subscription_quota_records, tách theo từng QuotaType và có bộ đếm
 * used riêng.
 *
 * <p>Cố tình KHÔNG gộp hai thứ đó vào một con số: gói cấp hạn mức theo TỪNG loại (GRADING /
 * CLASS_TEST / PRACTICE), nên một số dư cấp trường duy nhất không diễn đạt được "còn 300.000 nhưng
 * không được dùng để chấm bài". Gộp lại là âm thầm xóa mất giới hạn theo loại.
 *
 * <p>Ví này chỉ được đụng tới KHI hạn mức của loại tương ứng đã cạn -- xem ConsumeQuotaUseCase.
 */
public class SchoolBalance {
    private UUID id;
    private UUID schoolId;
    // Không có CHECK >= 0 ở tầng DB: phần âm ở đây CHÍNH LÀ nợ, phát sinh khi QuotaType cho phép ghi
    // nợ (GRADING/CLASS_TEST) tiêu vượt số dư.
    private BigDecimal balanceVnd;
    private Instant createdAt;
    private Instant updatedAt;
    // Phải mang theo ở domain model, không chỉ ở JpaEntity: mapper dựng entity MỚI mỗi lần lưu nên
    // entity luôn detached -- thiếu version, Hibernate coi là transient và INSERT đè lên id đã có.
    private Long version;

    public SchoolBalance() {}

    public SchoolBalance(UUID id, UUID schoolId, BigDecimal balanceVnd, Instant createdAt, Instant updatedAt, Long version) {
        this.id = id;
        this.schoolId = schoolId;
        this.balanceVnd = balanceVnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public SchoolBalance(UUID schoolId, BigDecimal balanceVnd, Instant createdAt, Instant updatedAt) {
        this.schoolId = schoolId;
        this.balanceVnd = balanceVnd;
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

    /** Nợ = số dư âm. Trường vẫn dùng được (nếu QuotaType cho ghi nợ) nhưng phải nạp bù. */
    public boolean isInDebt() {
        return balanceVnd.compareTo(BigDecimal.ZERO) < 0;
    }
}

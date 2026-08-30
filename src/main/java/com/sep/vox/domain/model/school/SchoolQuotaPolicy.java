package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

/**
 * Trần phân phối hạn mức của một trường, cho MỘT loại hạn mức.
 *
 * <p>Trả lời đúng một câu hỏi: trong ví hạn mức mà gói cấp, trường được chia ra cho từng người tối đa
 * bao nhiêu phần trăm. Phần còn lại là khoản DỰ PHÒNG -- không bị giữ ở đâu cả, chỉ là phần ví chưa
 * ai có trần chi để tiêu vào, để dành cấp thêm giữa kỳ cho người thật sự cần.
 *
 * <p>Thuộc về TRƯỜNG, không thuộc kỳ đăng ký: bản ghi hạn mức được dựng lại mỗi kỳ (xem
 * {@code OrderSettlementService.seedQuotaRecords}), nên đặt chính sách ở đó là để nó biến mất sau mỗi
 * lần gia hạn. Đây là cùng lý do khiến số dư tự nạp nằm ở {@link SchoolBalance} chứ không nằm theo kỳ.
 */
public class SchoolQuotaPolicy {

    /** Chia được toàn bộ ví -- mặc định, và là hành vi của hệ thống trước khi có chính sách này. */
    public static final BigDecimal FULLY_DISTRIBUTABLE = BigDecimal.ONE;

    private UUID id;
    private UUID schoolId;
    private QuotaType quotaType;
    /** Từ 0 tới 1. DB có CHECK giữ đúng khoảng này. */
    private BigDecimal distributableRatio;
    private Instant createdAt;
    private Instant updatedAt;

    public SchoolQuotaPolicy() {}

    public SchoolQuotaPolicy(UUID id, UUID schoolId, QuotaType quotaType, BigDecimal distributableRatio,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.quotaType = quotaType;
        this.distributableRatio = distributableRatio;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public SchoolQuotaPolicy(UUID schoolId, QuotaType quotaType, BigDecimal distributableRatio,
            Instant createdAt, Instant updatedAt) {
        this.schoolId = schoolId;
        this.quotaType = quotaType;
        this.distributableRatio = distributableRatio;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Chính sách mặc định cho trường chưa từng đặt gì -- chia được toàn bộ.
     *
     * <p>KHÔNG ghi dòng này xuống DB ở đường đọc: không có dòng và có dòng tỷ lệ 1.0 là cùng một
     * nghĩa, nên tạo sẵn chỉ để đọc là đẻ ra một hàng cho mọi trường chỉ vì có người mở trang.
     */
    public static SchoolQuotaPolicy fullyDistributable(UUID schoolId, QuotaType quotaType) {
        return new SchoolQuotaPolicy(schoolId, quotaType, FULLY_DISTRIBUTABLE, null, null);
    }

    /**
     * Phần ví được phép chia ra cho từng người.
     *
     * <p>Làm tròn XUỐNG về đúng scale của cột tiền (6 chữ số thập phân): làm tròn lên sẽ cho chia
     * nhiều hơn trần một vài phần triệu đồng, tức phá đúng cái ràng buộc mà chính sách này đặt ra.
     */
    public BigDecimal distributableAmountOf(BigDecimal totalAllocatedVnd) {
        if (totalAllocatedVnd == null) {
            return BigDecimal.ZERO;
        }
        return totalAllocatedVnd
            .multiply(distributableRatio == null ? FULLY_DISTRIBUTABLE : distributableRatio)
            .setScale(6, java.math.RoundingMode.DOWN);
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

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getDistributableRatio() {
        return distributableRatio;
    }

    public void setDistributableRatio(BigDecimal distributableRatio) {
        this.distributableRatio = distributableRatio;
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
}

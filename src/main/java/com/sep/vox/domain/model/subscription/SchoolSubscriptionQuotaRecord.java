package com.sep.vox.domain.model.subscription;

import java.math.BigDecimal;
import java.util.UUID;

import com.sep.vox.domain.model.metering.QuotaType;

public class SchoolSubscriptionQuotaRecord {
    private UUID id;
    private UUID schoolSubscriptionId;
    private QuotaType quotaType;
    private BigDecimal totalAllocatedAmountVnd;
    private BigDecimal usedAmountVnd;
    /**
     * Phần của {@code totalAllocatedAmountVnd} đến từ ví tự nạp của trường chứ không phải từ gói --
     * xem V12. Luôn tăng CÙNG total trong một câu lệnh (addFundingFromBalance), nên hai cột không thể
     * lệch nhau; DB có CHECK giữ {@code 0 <= funded <= total}.
     */
    private BigDecimal fundedFromBalanceVnd = BigDecimal.ZERO;
    /**
     * Kỳ nguồn mà ví này còn PHẢI nhận tiền tự nạp chưa tiêu -- một cái hẹn, không phải một số tiền.
     * Chỉ khác null ở kỳ sinh ra từ một lần gia hạn SỚM, và chỉ tới lần job kế tiếp. Xem V13.
     */
    private UUID carryFundingFromSubscriptionId;

    public SchoolSubscriptionQuotaRecord() {}

    public SchoolSubscriptionQuotaRecord(UUID id, UUID schoolSubscriptionId, QuotaType quotaType,
            BigDecimal totalAllocatedAmountVnd, BigDecimal usedAmountVnd, BigDecimal fundedFromBalanceVnd) {
        this.id = id;
        this.schoolSubscriptionId = schoolSubscriptionId;
        this.quotaType = quotaType;
        this.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
        this.usedAmountVnd = usedAmountVnd;
        this.fundedFromBalanceVnd = fundedFromBalanceVnd == null ? BigDecimal.ZERO : fundedFromBalanceVnd;
    }

    public SchoolSubscriptionQuotaRecord(UUID id, UUID schoolSubscriptionId, QuotaType quotaType, BigDecimal totalAllocatedAmountVnd, BigDecimal usedAmountVnd) {
        this(id, schoolSubscriptionId, quotaType, totalAllocatedAmountVnd, usedAmountVnd, BigDecimal.ZERO);
    }

    public SchoolSubscriptionQuotaRecord(UUID schoolSubscriptionId, QuotaType quotaType, BigDecimal totalAllocatedAmountVnd, BigDecimal usedAmountVnd) {
        this(null, schoolSubscriptionId, quotaType, totalAllocatedAmountVnd, usedAmountVnd, BigDecimal.ZERO);
    }

    /**
     * Bản ghi hạn mức của một kỳ MỚI: định mức gói cộng phần tiền tự nạp chưa tiêu mang sang từ kỳ cũ.
     *
     * <p>Factory có tên chứ không phải một constructor quá tải nữa: chữ ký
     * {@code (UUID, QuotaType, BigDecimal, BigDecimal, BigDecimal)} sẽ đứng cạnh
     * {@code (UUID, UUID, QuotaType, BigDecimal, BigDecimal)} và người đọc phải đếm tham số mới biết
     * mình đang gọi cái nào.
     *
     * <p>Đây cũng là nơi DUY NHẤT dựng được một dòng đã có funded > 0, và nó buộc
     * {@code total = định mức gói + carried} -- tức bất biến {@code funded <= total} được giữ bằng
     * chính phép cộng, không phải bằng lời hứa của chỗ gọi.
     */
    public static SchoolSubscriptionQuotaRecord seeded(UUID schoolSubscriptionId, QuotaType quotaType,
            BigDecimal planIncludedAmountVnd, BigDecimal carriedFundingVnd) {
        var included = planIncludedAmountVnd == null ? BigDecimal.ZERO : planIncludedAmountVnd;
        var carried = carriedFundingVnd == null ? BigDecimal.ZERO : carriedFundingVnd.max(BigDecimal.ZERO);
        return new SchoolSubscriptionQuotaRecord(
            null, schoolSubscriptionId, quotaType, included.add(carried), BigDecimal.ZERO, carried);
    }

    /**
     * Bản ghi hạn mức của một kỳ TƯƠNG LAI (gia hạn sớm): đúng định mức gói, chưa mang gì sang, cộng
     * một cái HẸN trỏ về kỳ nguồn.
     *
     * <p>Phần tiền tự nạp CỐ Ý để trống ở đây. Kỳ nguồn vẫn đang chạy, vẫn tiêu được và vẫn nạp thêm
     * được cho tới ranh giới, nên mọi con số chốt tại thời điểm này đều có thể sai đi trước khi kỳ mới
     * kịp bắt đầu -- chốt sớm là nhân đôi tiền hoặc làm bốc hơi tiền, tùy trường làm gì trong quãng đó.
     * Xem V13 và {@code CarryQuotaFundingAtPeriodStartService}.
     */
    public static SchoolSubscriptionQuotaRecord seededPendingCarry(UUID schoolSubscriptionId,
            QuotaType quotaType, BigDecimal planIncludedAmountVnd, UUID carryFundingFromSubscriptionId) {
        var record = seeded(schoolSubscriptionId, quotaType, planIncludedAmountVnd, BigDecimal.ZERO);
        record.setCarryFundingFromSubscriptionId(carryFundingFromSubscriptionId);
        return record;
    }

    /**
     * Phần tiền tự nạp còn CHƯA tiêu của kỳ này -- con số mang sang kỳ sau.
     *
     * <p>Quy ước: <b>tiền của GÓI tiêu trước, tiền tự nạp tiêu sau.</b> Không có cột nào ghi lại từng
     * đồng đã tiêu thuộc nguồn nào, nên phải chọn một quy ước, và quy ước này là quy ước có lợi cho
     * nhà trường: định mức gói dù sao cũng hết hạn cuối kỳ, còn tiền trường tự bỏ ra thì không nên
     * bốc hơi trước nó. Nó cũng khớp với thứ tự mà ConsumeQuotaService vốn đã tiêu ở cấp trên (hạn
     * mức trước, ví sau).
     *
     * <p>Hệ quả: {@code min(funded, total - used)}. Tiêu 12tr trên một ví 15tr có 5tr tự nạp thì còn
     * 3tr -- 2tr tiền tự nạp đã bị tiêu, và chỉ 3tr được mang sang.
     */
    public BigDecimal unspentFundedVnd() {
        var funded = fundedFromBalanceVnd == null ? BigDecimal.ZERO : fundedFromBalanceVnd;
        var remaining = totalAllocatedAmountVnd.subtract(usedAmountVnd);
        return funded.min(remaining).max(BigDecimal.ZERO);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSchoolSubscriptionId() {
        return schoolSubscriptionId;
    }

    public void setSchoolSubscriptionId(UUID schoolSubscriptionId) {
        this.schoolSubscriptionId = schoolSubscriptionId;
    }

    public QuotaType getQuotaType() {
        return quotaType;
    }

    public void setQuotaType(QuotaType quotaType) {
        this.quotaType = quotaType;
    }

    public BigDecimal getTotalAllocatedAmountVnd() {
        return totalAllocatedAmountVnd;
    }

    public void setTotalAllocatedAmountVnd(BigDecimal totalAllocatedAmountVnd) {
        this.totalAllocatedAmountVnd = totalAllocatedAmountVnd;
    }

    public BigDecimal getUsedAmountVnd() {
        return usedAmountVnd;
    }

    public void setUsedAmountVnd(BigDecimal usedAmountVnd) {
        this.usedAmountVnd = usedAmountVnd;
    }

    public BigDecimal getFundedFromBalanceVnd() {
        return fundedFromBalanceVnd;
    }

    public void setFundedFromBalanceVnd(BigDecimal fundedFromBalanceVnd) {
        this.fundedFromBalanceVnd = fundedFromBalanceVnd;
    }

    public UUID getCarryFundingFromSubscriptionId() {
        return carryFundingFromSubscriptionId;
    }

    public void setCarryFundingFromSubscriptionId(UUID carryFundingFromSubscriptionId) {
        this.carryFundingFromSubscriptionId = carryFundingFromSubscriptionId;
    }
}
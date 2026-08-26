package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sep.vox.domain.common.ZoneConstant;
import com.sep.vox.domain.model.subscription.SchoolSubscription;

/**
 * Luật của việc NÂNG CẤP giữa chừng: khi nào được nâng, và bù lại bao nhiêu.
 *
 * <p>Hai câu hỏi đó nằm chung một chỗ vì dùng chung một phép tính -- tỉ lệ thời gian đã trôi qua của
 * kỳ đang chạy -- và vì tách ra thì rất dễ sửa một bên quên bên kia, mà lệch nhau ở đây là lệch tiền.
 *
 * <h2>Vì sao phải có sàn thời gian mới cho nâng cấp</h2>
 *
 * <p>Khoản bù tỉ lệ với phần chưa dùng, còn kỳ mới thì luôn là một chu kỳ ĐẦY ĐỦ. Gọi f là tỉ lệ đã
 * trôi qua lúc nâng cấp: trường trả {@code P × f} và nhận về trọn một kỳ. Không có sàn thì f tiến về
 * 0 -- nâng cấp qua lại giữa hai gói ngay ngày hôm sau, mỗi lần trả gần như 0 đồng mà vẫn luôn có
 * một kỳ đầy đủ trong tay. Dùng vô hạn, thu gần bằng 0.
 *
 * <p>Có sàn f thì mỗi lần nâng cấp phải chờ {@code f × T}, nên chi phí trên một đơn vị thời gian là
 * {@code (P × f) / (f × T) = P / T} -- đúng bằng giá gốc. Nói cách khác sàn KHÔNG phải để phạt ai:
 * nó là thứ đưa việc đổi gói liên tục về đúng mức thu bình thường. Chọn 20% vì con số cụ thể chỉ
 * quyết định mức độ ồn ào (5 lần đổi mỗi kỳ), không quyết định doanh thu.
 */
@Service
public class SubscriptionUpgradePolicyService {

    /** Phải dùng ít nhất 20% kỳ hiện tại mới được nâng cấp -- xem javadoc lớp. */
    private static final BigDecimal MIN_ELAPSED_RATIO = new BigDecimal("0.20");

    private static final DateTimeFormatter DISPLAY_DATE =
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneConstant.BUSINESS_ZONE);

    /**
     * Chặn nâng cấp khi kỳ hiện tại còn quá mới.
     *
     * <p>Soi trên kỳ ĐANG CHẠY, không phải kỳ xa nhất: kỳ xếp hàng chờ chưa bắt đầu nên "đã dùng bao
     * nhiêu" của nó luôn bằng 0, lấy nó làm mốc thì không bao giờ nâng cấp được.
     */
    public void requireUpgradeEligible(SchoolSubscription inForce, Instant at) {
        var total = Duration.between(inForce.getStartDate(), inForce.getEndDate()).toSeconds();
        if (total <= 0) {
            return;
        }
        var elapsed = Duration.between(inForce.getStartDate(), at).toSeconds();
        var minElapsed = MIN_ELAPSED_RATIO.multiply(BigDecimal.valueOf(total)).longValue();
        if (elapsed >= minElapsed) {
            return;
        }

        var eligibleAt = inForce.getStartDate().plusSeconds(minElapsed);
        throw new IllegalStateException(
            "Gói hiện tại mới bắt đầu, chưa thể nâng cấp. Có thể nâng cấp từ ngày "
                + DISPLAY_DATE.format(eligibleAt) + ".");
    }

    /**
     * Tổng khoản bù cho MỌI kỳ sẽ bị đóng khi nâng cấp -- gồm cả kỳ đang chạy lẫn kỳ đã trả tiền và
     * đang xếp hàng chờ.
     *
     * <p>Phải cộng cả kỳ xếp hàng: nâng cấp đóng hết (xem OrderSettlementService.cutOverToUpgrade),
     * nên bỏ sót kỳ nào là đóng một kỳ trường đã trả tiền mà không hoàn lại gì. Kỳ chưa bắt đầu thì
     * phần chưa dùng đúng bằng toàn bộ giá đã trả.
     *
     * <p>Lấy {@code pricePaidSnapshot} chứ không phải giá hiện tại của gói: hoàn lại phải dựa trên số
     * tiền trường THẬT SỰ đã trả, mà gói thì có thể đã đổi giá từ lúc đó.
     *
     * @param cap trần của tổng khoản bù -- giá gói mới. Ràng buộc
     *            chk_orders_discount_amount_vnd_lower_or_equals_than_subtotal_and_charged_fee không
     *            cho giảm giá lớn hơn tiền hàng, và đơn âm tiền thì cổng nào cũng từ chối.
     */
    public BigDecimal calculateUnusedCredit(List<SchoolSubscription> unfinished, BigDecimal cap, Instant at) {
        var total = unfinished.stream()
            .map(subscription -> unusedValueOf(subscription, at))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        return total.min(cap);
    }

    private BigDecimal unusedValueOf(SchoolSubscription subscription, Instant at) {
        if (subscription.getPricePaidSnapshot() == null) {
            return BigDecimal.ZERO;
        }
        var total = Duration.between(subscription.getStartDate(), subscription.getEndDate()).toSeconds();
        var remaining = Duration.between(at, subscription.getEndDate()).toSeconds();
        if (total <= 0 || remaining <= 0) {
            return BigDecimal.ZERO;
        }
        // Kỳ chưa bắt đầu: remaining lớn hơn cả độ dài kỳ, nhưng bù nhiều nhất cũng chỉ là trọn giá.
        remaining = Math.min(remaining, total);

        return subscription.getPricePaidSnapshot()
            .multiply(BigDecimal.valueOf(remaining))
            .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP);
    }
}

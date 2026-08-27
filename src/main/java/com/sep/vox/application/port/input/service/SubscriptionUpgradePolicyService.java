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

    /**
     * Số tiền tối thiểu một đơn nâng cấp phải còn lại để trả -- xem {@link #calculateUnusedCredit}.
     *
     * <p>Con số cụ thể không mang ý nghĩa nghiệp vụ, nó chỉ cần nằm trên mức tối thiểu THẬT của cổng
     * thanh toán. Chỉnh lại theo hạn mức của PayOS/SePay nếu mức đó cao hơn.
     */
    private static final BigDecimal MIN_PAYABLE_VND = new BigDecimal("1000");

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
     * <p>Trần thật là {@code cap - MIN_PAYABLE_VND}, không phải {@code cap}: đơn 0đ cũng không thanh
     * toán được y như đơn âm tiền. Trần bằng đúng cap là chuyện CÓ THẬT chứ không phải giả định -- một
     * trường có kỳ đang chạy CỘNG kỳ đã trả tiền đang xếp hàng thì tổng phần chưa dùng vượt giá một
     * gói lẻ dễ dàng, và khi đó đơn ra đúng 0đ rồi nằm PENDING tới lúc hết hạn: nâng cấp không có
     * đường nào đi tới.
     *
     * <p>Chọn kẹp bớt khoản bù thay vì cho đơn 0đ tự settle: settlePaid nhận vào một PaymentRecord,
     * nên đường kia bắt buộc phải bịa ra một bản ghi thanh toán không có thật, làm hỏng chính chỗ dùng
     * để đối soát với cổng. Và việc "bù thừa thì mất phần thừa" vốn đã là luật ở đây rồi -- đó chính
     * là {@code min(cap)} -- nên giữ lại một khoản nhỏ phải trả là cùng một luật, không phải luật mới.
     *
     * @param cap trần danh nghĩa của tổng khoản bù -- giá gói mới. Ràng buộc
     *            chk_orders_discount_amount_vnd_lower_or_equals_than_subtotal_and_charged_fee không
     *            cho giảm giá lớn hơn tiền hàng, và đơn âm tiền thì cổng nào cũng từ chối.
     */
    public BigDecimal calculateUnusedCredit(List<SchoolSubscription> unfinished, BigDecimal cap, Instant at) {
        var total = unfinished.stream()
            .map(subscription -> unusedValueOf(subscription, at))
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        // max(ZERO) cho trường hợp gói rẻ hơn cả mức tối thiểu: khi đó không bù đồng nào và trường trả
        // trọn giá, vẫn tốt hơn một đơn không thể thanh toán.
        var payableCap = cap.subtract(MIN_PAYABLE_VND).max(BigDecimal.ZERO);
        return total.min(payableCap);
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

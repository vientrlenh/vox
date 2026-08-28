package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sep.vox.application.port.input.service.SubscriptionUpgradePolicyService;
import com.sep.vox.domain.model.subscription.SchoolSubscription;

/**
 * Khoản bù khi nâng cấp phải luôn chừa lại một số tiền THẬT SỰ trả được.
 *
 * <p>Đơn 0đ không thanh toán được y như đơn âm tiền: cổng từ chối, đơn nằm PENDING tới lúc hết hạn, và
 * trường không có đường nào nâng cấp. Trần cũ đặt đúng bằng giá gói mới nên chạm 0 là chuyện có thật --
 * không cần gói lạ, chỉ cần trường có kỳ đang chạy CỘNG một kỳ đã trả tiền đang xếp hàng, vì
 * calculateUnusedCredit cố ý bù cho mọi kỳ chưa kết thúc.
 */
class SubscriptionUpgradeCreditTests {

    private static final BigDecimal MIN_PAYABLE_VND = new BigDecimal("1000");

    private final SubscriptionUpgradePolicyService policy = new SubscriptionUpgradePolicyService();
    private final Instant now = Instant.parse("2026-06-01T00:00:00Z");

    /**
     * Kỳ đang chạy còn nguyên giá trị + một kỳ xếp hàng đã trả đủ: tổng phần chưa dùng vượt hẳn giá
     * gói mới. Bản cũ trả về đúng cap nên đơn ra 0đ.
     */
    @Test
    void should_leave_something_payable_when_unused_credit_exceeds_the_new_plan_price() {
        var newPlanPrice = new BigDecimal("12000000");
        var unfinished = List.of(
            period(new BigDecimal("10000000"), now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(364))),
            queuedPeriod(new BigDecimal("10000000"), now.plus(Duration.ofDays(364)))
        );

        var credit = policy.calculateUnusedCredit(unfinished, newPlanPrice, now);

        assertThat(newPlanPrice.subtract(credit))
            .as("số tiền còn phải trả")
            .isGreaterThanOrEqualTo(MIN_PAYABLE_VND);
        assertThat(credit).isEqualByComparingTo(newPlanPrice.subtract(MIN_PAYABLE_VND));
    }

    /** Bù ít hơn trần thì không bị đụng tới -- kẹp chỉ áp khi thật sự chạm trần. */
    @Test
    void should_not_touch_a_credit_that_is_already_below_the_cap() {
        var newPlanPrice = new BigDecimal("12000000");
        // Nửa kỳ đã trôi qua của một gói 6.000.000 -> bù khoảng 3.000.000, xa trần.
        var unfinished = List.of(
            period(new BigDecimal("6000000"), now.minus(Duration.ofDays(182)), now.plus(Duration.ofDays(183)))
        );

        var credit = policy.calculateUnusedCredit(unfinished, newPlanPrice, now);

        assertThat(credit).isLessThan(newPlanPrice.subtract(MIN_PAYABLE_VND));
        assertThat(credit).isGreaterThan(BigDecimal.ZERO);
    }

    /** Gói rẻ hơn cả mức tối thiểu: không bù đồng nào, trường trả trọn giá -- vẫn hơn một đơn không trả được. */
    @Test
    void should_give_no_credit_when_the_new_plan_costs_less_than_the_minimum_payable() {
        var newPlanPrice = new BigDecimal("500");
        var unfinished = List.of(queuedPeriod(new BigDecimal("10000000"), now.plus(Duration.ofDays(300))));

        var credit = policy.calculateUnusedCredit(unfinished, newPlanPrice, now);

        assertThat(credit).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /** Không có kỳ nào chưa kết thúc thì không có gì để bù. */
    @Test
    void should_give_no_credit_when_there_is_nothing_unfinished() {
        assertThat(policy.calculateUnusedCredit(List.of(), new BigDecimal("12000000"), now))
            .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private SchoolSubscription period(BigDecimal pricePaid, Instant start, Instant end) {
        var subscription = new SchoolSubscription();
        subscription.setPricePaidSnapshot(pricePaid);
        subscription.setStartDate(start);
        subscription.setEndDate(end);
        return subscription;
    }

    /** Kỳ đã trả tiền nhưng chưa tới ngày chạy -- phần chưa dùng đúng bằng toàn bộ giá đã trả. */
    private SchoolSubscription queuedPeriod(BigDecimal pricePaid, Instant start) {
        return period(pricePaid, start, start.plus(Duration.ofDays(365)));
    }
}

package com.sep.vox.application.port.input.usecase.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.order.CreateTopUpOrderCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ServiceFeePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Places an order to top up the school's balance. Replaces the old BuyTokensUseCase.
 *
 * <p>The biggest difference from the old flow: this use case grants NOTHING. BuyTokensUseCase added quota IMMEDIATELY and
 * wrote a PAID invoice directly without any gateway confirming the money had been collected (its own comment said so,
 * which is why it had to forbid every online method itself). Now the order just stays PENDING until the money really
 * arrives, so there is no longer any reason to distinguish payment methods at the point of placing the order.
 *
 * <p>A top-up order DELIBERATELY has no order_items: topping up does not buy any "item" to list, and the wallet
 * (school_balances.balance_vnd) is a single number that is not split by QuotaType. The old TokenPurchaseItem list existed
 * only because quota used to be split per type.
 */
@Service
@RequiredArgsConstructor
public class CreateTopUpOrderUseCase implements IUseCase<CreateTopUpOrderCommand, UUID> {

    // orders.total_amount_vnd is numeric(15,0). Capped by the TOTAL (fee included), not by the quota
    // amount alone: exceeding the limit only shows up at the DB layer as "numeric field overflow".
    private static final BigDecimal MAX_TOTAL_AMOUNT_VND = new BigDecimal("999999999999999");

    private final OrderRepository orderRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final ServiceFeePort serviceFeePort;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(CreateTopUpOrderCommand input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();

        BigDecimal creditAmountVnd = validatedCreditAmount(input.creditAmountVnd());

        // The balance is only spent once the plan's quota is exhausted (OVERAGE_CHARGE), so topping up while no plan is running
        // puts money where it cannot be spent. The balance DOES survive across renewals -- blocking here blocks topping up TOO
        // EARLY, it does not attach the money to a plan.
        boolean hasActiveSubscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).isPresent();
        if (!hasActiveSubscription) {
            throw new IllegalArgumentException("Trường chưa có gói đăng ký nào đang hoạt động để nạp thêm.");
        }

        // The service fee is ADDED to the amount to pay, not carved out of the balance: if the school asks for 1,000,000 it
        // receives exactly 1,000,000 in the wallet and pays 1,200,000. The fee is rounded to whole dong because the column is
        // numeric(15,0) and the gateway only accepts integers -- rounding here instead of letting Postgres do it, so the stored
        // number equals the number used to compute the total.
        BigDecimal serviceFeeVnd = creditAmountVnd
            .multiply(serviceFeePort.serviceFeeRatio())
            .setScale(0, RoundingMode.HALF_UP);

        if (creditAmountVnd.add(serviceFeeVnd).compareTo(MAX_TOTAL_AMOUNT_VND) > 0) {
            throw new IllegalArgumentException("Số tiền nạp vượt quá giới hạn cho phép");
        }

        Instant now = Instant.now();
        Order savedOrder = orderRepository.save(Order.forTopUp(
            schoolId,
            "Nạp thêm vào số dư",
            creditAmountVnd,
            serviceFeeVnd,
            now
        ));

        // No order_items are written: see the class Javadoc.
        return savedOrder.getId();
    }

    /**
     * Tiền qua cổng là số nguyên đồng -- VND không còn đơn vị lẻ và PayOS/SePay đều từ chối số thập
     * phân. Bắt ở đây thay vì để Postgres làm tròn im lặng: trường xin nạp 100.000,5đ mà nhận
     * 100.001đ thì số dư lệch với số tiền đã trả, và lệch đúng vào chỗ khó lần ra nhất.
     */
    private static BigDecimal validatedCreditAmount(BigDecimal creditAmountVnd) {
        if (creditAmountVnd == null) {
            throw new IllegalArgumentException("Số tiền nạp không được để trống");
        }
        if (creditAmountVnd.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        if (creditAmountVnd.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Số tiền nạp phải là số nguyên VND, không có phần thập phân");
        }
        return creditAmountVnd;
    }
}

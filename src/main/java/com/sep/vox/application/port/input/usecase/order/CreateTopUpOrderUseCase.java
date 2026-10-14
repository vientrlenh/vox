package com.sep.vox.application.port.input.usecase.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.CreateTopUpOrderCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ServiceFeePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
public class CreateTopUpOrderUseCase implements IUseCase<CreateTopUpOrderCommand, UUID> {

    // orders.total_amount_vnd là numeric(15,0). Chặn theo TỔNG (đã cộng phí) chứ không theo phần tiền
    // hạn mức: vượt ngưỡng chỉ lộ ra ở tận tầng DB dưới dạng "numeric field overflow".
    private static final BigDecimal MAX_TOTAL_AMOUNT_VND = new BigDecimal("999999999999999");

    private final OrderRepository orderRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final ServiceFeePort serviceFeePort;
    private final UserContextPort userContextPort;

    public CreateTopUpOrderUseCase(
            OrderRepository orderRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            ServiceFeePort serviceFeePort,
            UserContextPort userContextPort) {
        this.orderRepository = orderRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.serviceFeePort = serviceFeePort;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateTopUpOrderCommand input) {
        var schoolId = userContextPort.getCurrentSchoolId();

        var creditAmountVnd = validatedCreditAmount(input.creditAmountVnd());

        var hasActiveSubscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).isPresent();
        if (!hasActiveSubscription) {
            throw new IllegalStateException("Trường chưa có gói đăng ký nào đang hoạt động để nạp thêm.");
        }

        var serviceFeeVnd = creditAmountVnd
            .multiply(serviceFeePort.serviceFeeRatio())
            .setScale(0, RoundingMode.HALF_UP);

        if (creditAmountVnd.add(serviceFeeVnd).compareTo(MAX_TOTAL_AMOUNT_VND) > 0) {
            throw new IllegalArgumentException("Số tiền nạp vượt quá giới hạn cho phép");
        }

        var now = Instant.now();
        var savedOrder = orderRepository.save(Order.forTopUp(
            schoolId,
            "Nạp thêm vào số dư",
            creditAmountVnd,
            serviceFeeVnd,
            now,
            userContextPort.getCurrentAuthenticatedUserId()
        ));

        return savedOrder.getId();
    }

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

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

/**
 * Đặt đơn nạp thêm tiền vào số dư của trường. Thay cho BuyTokensUseCase cũ.
 *
 * <p>Khác biệt lớn nhất so với luồng cũ: use case này KHÔNG cấp gì cả. BuyTokensUseCase cộng hạn mức
 * NGAY và ghi thẳng một hóa đơn PAID mà không cổng nào xác nhận đã thu được tiền (chính comment của
 * nó nói vậy, nên nó phải tự cấm mọi phương thức online). Giờ đơn chỉ nằm PENDING cho tới khi tiền
 * về thật, nên không còn lý do gì phải phân biệt phương thức thanh toán ở khâu đặt đơn.
 *
 * <p>Đơn nạp thêm CỐ Ý không có order_items: nạp tiền không mua "món" nào để mà liệt kê, và ví
 * (school_balances.balance_vnd) là một con số duy nhất không chia theo QuotaType. Danh sách
 * TokenPurchaseItem cũ tồn tại chỉ vì hạn mức ngày xưa tách theo từng loại.
 */
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

        // Số dư chỉ bị tiêu khi hạn mức kèm gói đã cạn (OVERAGE_CHARGE), nên nạp lúc chưa có gói nào
        // đang chạy là nạp tiền vào chỗ không tiêu được. Số dư VẪN sống xuyên qua các lần gia hạn --
        // chặn ở đây là chặn nạp SỚM, không phải gắn tiền vào gói.
        var hasActiveSubscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).isPresent();
        if (!hasActiveSubscription) {
            throw new IllegalStateException("Trường chưa có gói đăng ký nào đang hoạt động để nạp thêm.");
        }

        // Phí dịch vụ cộng THÊM vào số tiền phải trả, không trích ra khỏi số dư: trường xin 1.000.000đ
        // thì nhận đúng 1.000.000đ vào ví và trả 1.200.000đ. Làm tròn phí về đồng nguyên vì cột là
        // numeric(15,0) và cổng chỉ nhận số nguyên -- làm tròn ở đây chứ không để Postgres tự làm, để
        // số ghi xuống đúng bằng số đã dùng để tính tổng.
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

        // Không ghi order_items: xem javadoc của class.
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

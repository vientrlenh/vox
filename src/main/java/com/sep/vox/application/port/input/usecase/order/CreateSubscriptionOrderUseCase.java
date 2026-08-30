package com.sep.vox.application.port.input.usecase.order;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSubscriptionOrderCommand;
import com.sep.vox.application.port.input.service.SubscriptionUpgradePolicyService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ServiceFeePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.domain.model.order.OrderType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.OrderItemRepository;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Đặt đơn mua một CHU KỲ gói cho trường. Thay cho SubmitRequestUseCase cũ.
 *
 * <p>Use case này KHÔNG cấp gói. Nó chỉ ghi nhận ý định mua ở trạng thái PENDING; gói chỉ thực sự
 * được tạo khi tiền về (xem OrderSettlementService). Đây là khác biệt lớn nhất so với luồng cũ, nơi
 * ApproveRequestUseCase tạo thẳng subscription ACTIVE mà không có cổng nào xác nhận đã thu được tiền.
 */
@Service
public class CreateSubscriptionOrderUseCase implements IUseCase<CreateSubscriptionOrderCommand, UUID> {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionUpgradePolicyService upgradePolicyService;
    private final ServiceFeePort serviceFeePort;
    private final UserContextPort userContextPort;

    public CreateSubscriptionOrderUseCase(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionUpgradePolicyService upgradePolicyService, 
            ServiceFeePort serviceFeePort, 
            UserContextPort userContextPort) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.upgradePolicyService = upgradePolicyService;
        this.serviceFeePort = serviceFeePort;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateSubscriptionOrderCommand input) {
        // Quyền đã chặn ở @PreAuthorize("hasRole('SCHOOL_ADMIN')"); ở đây chỉ còn lấy xem là trường
        // NÀO -- và lấy từ token nên không có đường nào đặt đơn hộ trường khác.
        var schoolId = userContextPort.getCurrentSchoolId();

        // Trường đang bị System Admin cưỡng chế đình chỉ (gian lận...) thì không cho đặt đơn mới cho
        // tới khi được gỡ đình chỉ tường minh: settlement sẽ tạo một subscription ACTIVE mới mà không
        // đụng tới bản ghi SUSPENDED, tức là đi vòng qua UnsuspendSubscriptionUseCase.
        //
        // Soi TOÀN BỘ lịch sử là cố ý -- không được lách bằng cách đợi kỳ bị đình chỉ trôi qua. Chế
        // tài vẫn có điểm dừng: SubscriptionExpiryJob chuyển kỳ SUSPENDED đã qua endDate sang EXPIRED
        // (xem SpringDataSchoolSubscriptionRepository.expireOverdue), nên cửa này khóa tối đa tới hết
        // kỳ đã trả tiền chứ không khóa vĩnh viễn.
        var hasSuspended = schoolSubscriptionRepository.findBySchoolId(schoolId).stream()
            .anyMatch(subscription -> subscription.getStatus() == SchoolSubscriptionStatus.SUSPENDED);
        if (hasSuspended) {
            throw new IllegalStateException("Gói đăng ký của trường đang bị đình chỉ, không thể đặt đơn mới.");
        }

        var plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));
        // Luồng cũ chỉ kiểm gói CÓ TỒN TẠI -- gap thật: đơn trỏ vào gói DRAFT (chưa chốt giá, admin
        // còn sửa được) hoặc ARCHIVED (đã ngừng bán) vẫn tạo được, và tới lúc settlement thì trường
        // đã trả tiền cho một gói lẽ ra không bán.
        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalStateException("Gói này hiện không được mở bán.");
        }

        // Chặn TRƯỚC cho ra thông báo đọc được. Nếu bỏ qua, uq_orders_one_open_subscription_order vẫn
        // chặn nhưng dưới dạng DataIntegrityViolationException -- trường nhận lỗi 500 thay vì biết
        // mình đang có một đơn dở dang cần trả hoặc hủy.
        if (orderRepository.findOpenSubscriptionOrderBySchoolId(schoolId).isPresent()) {
            throw new IllegalStateException(
                "Trường đang có một đơn đăng ký chờ thanh toán, hãy hoàn tất hoặc hủy đơn đó trước khi đặt đơn mới.");
        }

        var now = Instant.now();

        // Tối đa MỘT kỳ được xếp hàng chờ. Không chặn gia hạn sớm -- trường vẫn mua trước được ngay
        // ngày đầu, việc phải làm khi ngân sách phải tiêu trong năm tài chính -- chỉ chặn việc xếp
        // chồng nhiều kỳ.
        //
        // Cố ý KHÔNG dùng ngưỡng kiểu "phải dùng hết 80% mới cho gia hạn": ngưỡng đó nhắm sai đích.
        // Nó không ngăn xếp chồng (vẫn xếp được, chỉ muộn hơn) mà lại chặn đúng trường có tiền muốn
        // trả sớm. Chặn theo SỐ KỲ ĐANG CHỜ mới đúng thứ cần chặn: giữ rủi ro khóa giá ở đúng một kỳ.
        if (hasQueuedPeriod(schoolId, now)) {
            throw new IllegalStateException(
                "Trường đã có một kỳ đã thanh toán đang chờ tới ngày chạy, hãy đợi kỳ đó bắt đầu rồi mới mua tiếp.");
        }

        var savedOrder = orderRepository.save(buildOrder(schoolId, plan, now));

        // Đơn gói luôn có ĐÚNG MỘT dòng: mỗi đơn mua một chu kỳ của một gói. Dòng này là chỗ DUY NHẤT
        // ghi lại gói nào được mua (orders không có cột plan_id) nên settlement đọc nó để biết cấp gì.
        orderItemRepository.save(new OrderItem(
            savedOrder.getId(),
            OrderItemType.SUBSCRIPTION,
            plan.getId(),
            plan.getPriceVnd(),
            plan.getPriceVnd(),
            1
        ));

        return savedOrder.getId();
    }

    /**
     * NÂNG CẤP khi trường đang chạy một gói KHÁC, còn lại là SUBSCRIPTION_REQUEST -- gồm cả đăng ký
     * lần đầu lẫn gia hạn đúng gói đang dùng.
     *
     * <p>Gia hạn cố ý KHÔNG có OrderType riêng: settlement của nó giống hệt đăng ký lần đầu (tạo kỳ
     * mới + bộ hạn mức mới), chỉ khác mốc bắt đầu, mà mốc đó suy ra được từ dữ liệu chứ không cần
     * nhãn trên đơn. Nâng cấp thì KHÁC thật: kỳ đang chạy bị cắt ngang và có khoản bù, nên nó xứng
     * đáng một kiểu riêng.
     */
    private Order buildOrder(UUID schoolId, SubscriptionPlan plan, Instant now) {
        var createdBy = userContextPort.getCurrentAuthenticatedUserId();
        var inForce = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).orElse(null);

        if (inForce != null && isUpgradeTo(plan, inForce)) {
            return upgradeOrder(schoolId, plan, now, createdBy, inForce);
        }
        return chainedOrder(schoolId, plan, now, createdBy);
    }

    /**
     * NÂNG CẤP chỉ khi gói mới ĐẮT HƠN gói đang chạy. Gói rẻ hơn hoặc bằng giá đi đường nối tiếp --
     * và đó không phải chuyện đặt tên.
     *
     * <p>Khoản bù bị chặn trên bởi giá gói mới (xem SubscriptionUpgradePolicyService), nên "nâng cấp"
     * xuống một gói rẻ hơn sẽ âm thầm đốt phần chênh: đang dùng gói 12 triệu, đổi sang gói 3 triệu ở
     * ngày thứ hai thì chỉ được bù tối đa 3 triệu, mất trắng gần 9 triệu mà không có chỗ nào báo.
     * Đi đường nối tiếp thì trường dùng hết kỳ đắt tiền đã trả rồi mới sang gói rẻ -- không mất gì.
     *
     * <p>So GIÁ NIÊM YẾT của hai gói, không so pricePaidSnapshot: câu hỏi ở đây là "gói mới có phải
     * hàng cao cấp hơn không", chứ không phải "lần trước trường trả bao nhiêu" -- một trường từng
     * được giảm giá không vì thế mà biến mọi lần mua sau thành nâng cấp.
     */
    private boolean isUpgradeTo(SubscriptionPlan plan, SchoolSubscription inForce) {
        if (inForce.getSubscriptionPlanId().equals(plan.getId())) {
            return false;
        }
        return subscriptionPlanRepository.findById(inForce.getSubscriptionPlanId())
            .map(currentPlan -> plan.getPriceVnd().compareTo(currentPlan.getPriceVnd()) > 0)
            .orElse(false);
    }

    private Order upgradeOrder(UUID schoolId, SubscriptionPlan plan, Instant now, UUID createdBy,
            SchoolSubscription inForce) {
        upgradePolicyService.requireUpgradeEligible(inForce, now);

        // Bù cho MỌI kỳ chưa kết thúc, vì settlement sẽ đóng hết -- kể cả kỳ đã trả tiền đang xếp
        // hàng chờ.
        //
        // Tính ở ĐÂY, lúc đặt đơn, chứ không phải lúc settlement: trường phải nhìn thấy số phải trả
        // trước khi bấm thanh toán, và cổng thu đúng total của đơn. Hệ quả là đơn nằm chờ càng lâu
        // thì khoản bù càng rộng tay so với thực tế lúc đổi gói (tối đa lệch một ngày, theo
        // Order.PENDING_TTL) -- chấp nhận lệch về phía có lợi cho trường, vì tính lại lúc settlement
        // sẽ làm số tiền đã thu không còn khớp đơn, và đó mới là thứ không sửa được.
        var unusedCredit = upgradePolicyService.calculateUnusedCredit(
            schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now), plan.getPriceVnd(), now);

        // Phí dịch vụ cộng thêm (từ phần tiền sau trừ)
        var serviceFeeVnd = plan.getPriceVnd().subtract(unusedCredit)
            .multiply(serviceFeePort.serviceFeeRatio())
            .setScale(0, RoundingMode.HALF_UP);

        return Order.forSubscriptionUpgrade(schoolId,
            "Nâng cấp lên " + plan.getName(), plan.getPriceVnd(), unusedCredit, serviceFeeVnd, now, createdBy);
    }

    /**
     * Có kỳ nào đã trả tiền mà chưa tới ngày chạy không.
     *
     * <p>"Chưa kết thúc" trừ "đang chạy" -- không cần thêm truy vấn nào: kỳ đang chạy nhiều nhất là
     * một, nên còn dư dòng nào trong danh sách chưa-kết-thúc thì đó chính là kỳ đang xếp hàng.
     */
    private boolean hasQueuedPeriod(UUID schoolId, Instant now) {
        var unfinished = schoolSubscriptionRepository.findUnfinishedBySchoolId(schoolId, now).size();
        var inForce = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).isPresent() ? 1 : 0;
        return unfinished - inForce > 0;
    }

    /** Đăng ký mới, gia hạn, hoặc chuyển sang gói rẻ hơn -- tất cả đều nối tiếp kỳ đang chạy. */
    private Order chainedOrder(UUID schoolId, SubscriptionPlan plan, Instant now, UUID createdBy) {
        var serviceFeeVnd = plan.getPriceVnd().multiply(serviceFeePort.serviceFeeRatio()).setScale(0, RoundingMode.HALF_UP);
        return Order.forSubscription(schoolId, OrderType.SUBSCRIPTION_REQUEST,
            "Đăng ký " + plan.getName(), plan.getPriceVnd(), serviceFeeVnd, now, createdBy);
    }
}

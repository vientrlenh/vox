package com.sep.vox.application.port.input.usecase.order;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSubscriptionOrderCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.model.order.OrderItem;
import com.sep.vox.domain.model.order.OrderItemType;
import com.sep.vox.domain.model.order.OrderType;
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
    private final UserContextPort userContextPort;

    public CreateSubscriptionOrderUseCase(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            UserContextPort userContextPort) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
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
        var savedOrder = orderRepository.save(Order.forSubscription(
            schoolId,
            orderTypeFor(schoolId, plan),
            "Đăng ký " + plan.getName(),
            plan.getPriceVnd(),
            now,
            userContextPort.getCurrentAuthenticatedUserId()
        ));

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
     * UPGRADE khi trường đang dùng một gói KHÁC, còn lại là SUBSCRIPTION_REQUEST -- gồm cả đăng ký lần
     * đầu lẫn gia hạn đúng gói đang dùng.
     *
     * <p>Gia hạn cố ý KHÔNG có OrderType riêng: settlement của cả ba trường hợp là một thao tác duy
     * nhất (hết hạn gói cũ nếu có, tạo gói mới + bộ hạn mức mới), nên tách kiểu chỉ để đặt tên sẽ sinh
     * ra một nhánh không khác gì nhánh kia. Muốn đếm số lần gia hạn thì hỏi "trường này đã từng có
     * subscription trên đúng gói đó chưa", chính xác hơn là tin vào nhãn trên đơn.
     */
    private OrderType orderTypeFor(UUID schoolId, SubscriptionPlan requestedPlan) {
        return schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .filter(current -> !current.getSubscriptionPlanId().equals(requestedPlan.getId()))
            .map(current -> OrderType.SUBSCRIPTION_UPGRADE)
            .orElse(OrderType.SUBSCRIPTION_REQUEST);
    }
}

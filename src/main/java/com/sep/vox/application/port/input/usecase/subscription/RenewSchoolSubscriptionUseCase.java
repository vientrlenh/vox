package com.sep.vox.application.port.input.usecase.subscription;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSubscriptionOrderCommand;
import com.sep.vox.application.port.input.command.RenewSchoolSubscriptionCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.input.usecase.order.CreateSubscriptionOrderUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Gia hạn: đặt một đơn mua tiếp một chu kỳ của gói trường đang dùng.
 *
 * <p>Use case này KHÔNG tự tạo đơn -- nó chỉ làm đúng phần riêng của gia hạn (tìm gói đang dùng, đi
 * theo chuỗi thay thế, đối chiếu xác nhận của trường) rồi giao lại cho
 * {@link CreateSubscriptionOrderUseCase}. Gia hạn cố ý KHÔNG có OrderType riêng và KHÔNG có nhánh
 * settlement riêng: với hệ thống, "mua tiếp một chu kỳ" giống hệt "mua lần đầu" -- xem
 * CreateSubscriptionOrderUseCase.orderTypeFor. Nhân bản logic đặt đơn ở đây chỉ tạo ra một bản sao
 * sẽ lệch dần khỏi bản gốc.
 *
 * <p>Đây cũng là thứ thay cho RenewSubscriptionUseCase cũ, vốn tự tạo thẳng subscription mới mà
 * không có cổng nào xác nhận đã thu được tiền, và tự xóa hạn mức cũ -- chính là lỗi "mua token xong
 * gia hạn là mất sạch" mà cả refactor này sinh ra để sửa. Giờ tiền phải về trước, và số dư tự nạp
 * nằm ở ví cấp trường nên không kỳ nào đụng tới được.
 */
@Service
public class RenewSchoolSubscriptionUseCase implements IUseCase<RenewSchoolSubscriptionCommand, UUID> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanResolver subscriptionPlanResolver;
    private final CreateSubscriptionOrderUseCase createSubscriptionOrderUseCase;
    private final UserContextPort userContextPort;

    public RenewSchoolSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanResolver subscriptionPlanResolver,
            CreateSubscriptionOrderUseCase createSubscriptionOrderUseCase,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanResolver = subscriptionPlanResolver;
        this.createSubscriptionOrderUseCase = createSubscriptionOrderUseCase;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(RenewSchoolSubscriptionCommand input) {
        var schoolId = userContextPort.getCurrentSchoolId();

        var subscription = schoolSubscriptionRepository.findMostRecentBySchoolId(schoolId)
            .orElseThrow(() -> new NotFoundException(
                "Trường chưa từng đăng ký gói nào, hãy chọn gói từ danh sách thay vì gia hạn."));

        var currentPlan = subscriptionPlanRepository.findById(subscription.getSubscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));
        var renewalPlan = subscriptionPlanResolver.resolveActivePlan(currentPlan);

        // Đối chiếu với gói trường ĐÃ XÁC NHẬN ở màn xem trước. Không phải thủ tục cho có: giữa lúc
        // trường xem màn đó và lúc bấm gia hạn, System Admin hoàn toàn có thể lưu trữ gói hoặc đổi
        // gói thay thế -- và vì gia hạn TỰ đi theo chuỗi thay thế, không có bước này thì trường trả
        // tiền cho một gói khác hẳn gói vừa nhìn thấy.
        if (!renewalPlan.getId().equals(input.acceptedPlanId())) {
            throw new IllegalStateException(
                "Gói gia hạn đã thay đổi kể từ lúc bạn xem, vui lòng xem lại thông tin gia hạn trước khi tiếp tục.");
        }

        // Từ đây trở đi giống hệt mua mới: gói phải đang mở bán, trường không bị đình chỉ, không có
        // đơn đăng ký nào còn dở dang. Tất cả đã nằm sẵn trong CreateSubscriptionOrderUseCase.
        return createSubscriptionOrderUseCase.execute(new CreateSubscriptionOrderCommand(renewalPlan.getId()));
    }
}

package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ArchiveSubscriptionPlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ArchiveSubscriptionPlanUseCase implements IUseCase<ArchiveSubscriptionPlanCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public ArchiveSubscriptionPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(ArchiveSubscriptionPlanCommand input) {
        var plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        if (plan.getStatus() == SubscriptionPlanStatus.DRAFT) {
            throw new IllegalStateException("Gói đang ở trạng thái nháp thì phải xóa cứng, không lưu trữ.");
        }
        if (plan.getStatus() == SubscriptionPlanStatus.ARCHIVED) {
            throw new IllegalStateException("Gói đã được lưu trữ trước đó.");
        }

        // existsActiveByPlanId đã có sẵn từ trước nhưng chưa từng được gọi -- gap thật: archive vô
        // điều kiện dù đang có trường ACTIVE dùng gói này sẽ khiến trường đó kẹt cứng, không gia hạn
        // được cho tới khi có ai đó tạo gói thay thế (SubscriptionPlanResolver.resolveActivePlan ném
        // lỗi lúc gia hạn). Bắt buộc chọn gói thay thế NGAY LÚC archive nếu đang có trường dùng.
        if (input.replacedByPlanId() == null && schoolSubscriptionRepository.existsActiveByPlanId(plan.getId())) {
            throw new IllegalArgumentException(
                "Gói đang có trường sử dụng, phải chọn gói thay thế hoặc đợi tới khi không còn trường nào dùng gói này trước khi lưu trữ.");
        }

        if (input.replacedByPlanId() != null) {
            if (input.replacedByPlanId().equals(plan.getId())) {
                throw new IllegalArgumentException("Gói thay thế không được trùng với chính gói đang lưu trữ");
            }
            var replacement = subscriptionPlanRepository.findById(input.replacedByPlanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói thay thế"));
            if (replacement.getStatus() != SubscriptionPlanStatus.ACTIVE) {
                throw new IllegalArgumentException("Gói thay thế phải đang ở trạng thái hoạt động");
            }
            // Trường bị ép đổi gói khi gia hạn (SubscriptionPlanResolver.resolveActivePlan) không có
            // cơ hội từ chối giá mới -- bắt buộc gói thay thế phải cùng giá để không đổi mức thu của
            // trường theo quyết định 1 phía của System Admin.
            validateReplacementPlan(replacement, plan);
            plan.setReplacedByPlanId(replacement.getId());
        }
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var now = Instant.now();

        plan.setStatus(SubscriptionPlanStatus.ARCHIVED);
        plan.setUpdatedAt(now);
        plan.setUpdatedBy(userId);
        var savedPlan = subscriptionPlanRepository.save(plan);

        return savedPlan.getId();
    }

    private void validateReplacementPlan(SubscriptionPlan replacement, SubscriptionPlan replaced) {
        if (replacement.getPeriodCount() != replaced.getPeriodCount()) {
            throw new IllegalArgumentException("Giai đoạn của gói đăng ký thay thế không khớp với gói cần thay thế");
        }
        if (replacement.getPriceVnd().compareTo(replaced.getPriceVnd()) != 0) {
            throw new IllegalArgumentException("Gói thay thế phải có giá bằng đúng giá gói đang cần được thay thế");
        }
    }
}

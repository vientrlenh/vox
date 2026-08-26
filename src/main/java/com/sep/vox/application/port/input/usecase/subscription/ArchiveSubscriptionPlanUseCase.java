package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ArchiveSubscriptionPlanCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanReplacementValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ArchiveSubscriptionPlanUseCase implements IUseCase<ArchiveSubscriptionPlanCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanReplacementValidator replacementValidator;
    private final UserContextPort userContextPort;

    public ArchiveSubscriptionPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanReplacementValidator replacementValidator,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.replacementValidator = replacementValidator;
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
        //
        // Vế thứ hai (existsByReplacedByPlanId) vá đúng lỗ hổng đó ở MỘT BƯỚC XA HƠN: gói A đã trỏ
        // thay thế sang B, giờ lưu trữ B mà không chỉ gói tiếp theo. Không trường nào ACTIVE trên B
        // nên vế đầu cho qua, nhưng những trường đang ở A sẽ đi hết chuỗi rồi dừng ở một gói đã
        // ARCHIVED không có lối ra -- vẫn là kẹt cứng, chỉ khó nhìn ra hơn.
        if (input.replacedByPlanId() == null) {
            if (schoolSubscriptionRepository.existsActiveByPlanId(plan.getId())) {
                throw new IllegalArgumentException(
                    "Gói đang có trường sử dụng, phải chọn gói thay thế hoặc đợi tới khi không còn trường nào dùng gói này trước khi lưu trữ.");
            }
            if (subscriptionPlanRepository.existsByReplacedByPlanId(plan.getId())) {
                throw new IllegalArgumentException(
                    "Gói này đang là gói thay thế của một gói khác, phải chọn gói thay thế tiếp theo trước khi lưu trữ.");
            }
        }

        if (input.replacedByPlanId() != null) {
            var replacement = subscriptionPlanRepository.findById(input.replacedByPlanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói thay thế"));
            // Trường bị ép đổi gói khi gia hạn (SubscriptionPlanResolver.resolveActivePlan) không có
            // cơ hội từ chối -- xem SubscriptionPlanReplacementValidator cho bộ luật đầy đủ.
            replacementValidator.requireValidReplacement(replacement, plan);
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

}

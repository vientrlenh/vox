package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.PublishSubscriptionPlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class PublishSubscriptionPlanUseCase implements IUseCase<PublishSubscriptionPlanCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;
    private final UserContextPort userContextPort;

    public PublishSubscriptionPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(PublishSubscriptionPlanCommand input) {
        var plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // ACTIVE thì đã bán rồi, ARCHIVED là đã ngừng bán VĨNH VIỄN -- cho publish lại gói ARCHIVED sẽ
        // hồi sinh một gói mà replacedByPlanId đang trỏ đi nơi khác, thành hai gói cùng sống trên một
        // dây chuyền thay thế. Muốn bán lại thì tạo gói mới.
        if (plan.getStatus() != SubscriptionPlanStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể xuất bản gói đang ở trạng thái nháp.");
        }

        // Cửa cuối trước khi gói bán được cho trường. Không có ràng buộc DB nào bắt gói phải có ít nhất
        // một dòng hạn mức -- CreateSubscriptionPlanUseCase và UpdateSubscriptionPlanUseCase đều tự kiểm
        // ở tầng ứng dụng, nên đường nào không đi qua hai use case đó (seed, script, import về sau) vẫn
        // tạo ra được gói rỗng. Publish là lúc duy nhất còn chặn kịp: gói không hạn mức thì trường trả
        // tiền xong không dùng được gì, mà lỗi chỉ lộ ra lúc chấm bài.
        if (subscriptionPlanQuotaRepository.findBySubscriptionPlanId(plan.getId()).isEmpty()) {
            throw new IllegalStateException("Gói phải có ít nhất một hạn mức trước khi xuất bản.");
        }

        plan.setStatus(SubscriptionPlanStatus.ACTIVE);
        // Không tự tăng version: cột version do Hibernate quản lý qua @Version -- xem
        // UpdateSubscriptionPlanUseCase.
        plan.setUpdatedAt(Instant.now());
        plan.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        var savedPlan = subscriptionPlanRepository.save(plan);

        return savedPlan.getId();
    }
}

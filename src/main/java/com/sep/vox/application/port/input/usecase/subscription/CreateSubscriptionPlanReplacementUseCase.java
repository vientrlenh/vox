package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.CreateSubscriptionPlanReplacementCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Tạo một gói mới (DRAFT) rồi archive NGAY gói đang bán trỏ replacedByPlanId sang gói mới đó -- gộp
 * hai bước "tạo gói" + "ngừng bán, chọn gói thay thế" thành một, cho luồng "Tạo gói thay thế" trên
 * trang quản lý gói.
 *
 * <p>KHÔNG kiểm SubscriptionPlanReplacementValidator ở đây: gói mới còn DRAFT, admin có thể còn sửa
 * hạn mức/thời lượng trước khi xuất bản. Điều khoản (chu kỳ/giá bằng đúng, hạn mức/thời lượng không
 * thấp hơn) được PublishSubscriptionPlanUseCase chốt lại đúng lúc gói này thật sự nhận vai trò thay
 * thế -- xem SubscriptionPlanReplacementValidator.requireCompatibleTerms.
 */
@Service
public class CreateSubscriptionPlanReplacementUseCase
        implements IUseCase<CreateSubscriptionPlanReplacementCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final CreateSubscriptionPlanUseCase createSubscriptionPlanUseCase;
    private final UserContextPort userContextPort;

    public CreateSubscriptionPlanReplacementUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            CreateSubscriptionPlanUseCase createSubscriptionPlanUseCase,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.createSubscriptionPlanUseCase = createSubscriptionPlanUseCase;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(CreateSubscriptionPlanReplacementCommand input) {
        var replacedPlan = subscriptionPlanRepository.findById(input.replacedPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // Chỉ cho gói ĐANG BÁN: gói đã ARCHIVED cần sửa gói thay thế thì đi qua
        // UpdateSubscriptionPlanReplacementUseCase, gói DRAFT thì chưa có ai dùng nên chưa cần thay.
        if (replacedPlan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ tạo được gói thay thế cho gói đang bán.");
        }

        var newPlanId = createSubscriptionPlanUseCase.execute(input.newPlan());

        replacedPlan.setReplacedByPlanId(newPlanId);
        replacedPlan.setStatus(SubscriptionPlanStatus.ARCHIVED);
        replacedPlan.setUpdatedAt(Instant.now());
        replacedPlan.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());
        subscriptionPlanRepository.save(replacedPlan);

        return newPlanId;
    }
}

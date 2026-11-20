package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdateSubscriptionPlanReplacementCommand;
import com.sep.vox.application.port.input.service.SubscriptionPlanReplacementValidator;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

/**
 * Chỉ (hoặc chỉ lại) gói thay thế cho một gói ĐÃ lưu trữ.
 *
 * <p>Tồn tại để sửa một ngõ cụt KHÔNG có đường ra trước đây: replacedByPlanId chỉ đặt được đúng một
 * lần, ngay lúc gọi ArchiveSubscriptionPlanUseCase, mà use case đó lại từ chối chạy trên gói đã
 * ARCHIVED. Nên kịch bản dưới đây khóa chết một trường vĩnh viễn:
 *
 * <ol>
 *   <li>Kỳ thuê bao của trường hết hạn -> subscription chuyển EXPIRED.
 *   <li>System Admin lưu trữ gói, không chọn gói thay thế -- và được phép, vì chốt chặn lúc đó chỉ
 *       đếm trường đang ACTIVE trên gói, mà trường này thì không còn ACTIVE.
 *   <li>Trường quay lại gia hạn -> resolveActivePlan ném "Gói đã ngừng cung cấp và chưa có gói thay thế".
 *   <li>Admin muốn vá bằng cách chỉ gói thay thế -> archive lại thì bị chặn "Gói đã được lưu trữ trước đó."
 * </ol>
 *
 * <p>Tách use case riêng thay vì nới ArchiveSubscriptionPlanUseCase cho chạy hai lần: lưu trữ là
 * chuyện đổi trạng thái gói, còn đây là sửa một con trỏ. Gộp lại thì mỗi lần đọc phải tự đoán lần
 * gọi này mang ý nào, và chốt chặn của bên này sẽ vướng bên kia.
 */
@Service
public class UpdateSubscriptionPlanReplacementUseCase
        implements IUseCase<UpdateSubscriptionPlanReplacementCommand, UUID> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanReplacementValidator replacementValidator;
    private final UserContextPort userContextPort;

    public UpdateSubscriptionPlanReplacementUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanReplacementValidator replacementValidator,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.replacementValidator = replacementValidator;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(UpdateSubscriptionPlanReplacementCommand input) {
        var plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // CHỈ áp cho gói đã lưu trữ: replacedByPlanId của một gói còn bán không có nghĩa gì --
        // resolveActivePlan chỉ đi theo con trỏ đó khi gói đang ARCHIVED. Cho đặt trên gói ACTIVE là
        // tạo ra dữ liệu chẳng bao giờ được đọc, rồi có ngày ai đó tin là nó có tác dụng.
        if (plan.getStatus() != SubscriptionPlanStatus.ARCHIVED) {
            throw new IllegalStateException("Chỉ gói đã lưu trữ mới cần gói thay thế.");
        }

        var replacement = subscriptionPlanRepository.findById(input.replacedByPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói thay thế"));

        // Cùng bộ luật với lúc lưu trữ. Sửa con trỏ sau này KHÔNG được lỏng hơn: trường vẫn bị ép đi
        // theo nó khi gia hạn, và vẫn không có cơ hội từ chối.
        replacementValidator.requireValidReplacement(replacement, plan);

        plan.setReplacedByPlanId(replacement.getId());
        plan.setUpdatedAt(Instant.now());
        plan.setUpdatedBy(userContextPort.getCurrentAuthenticatedUserId());

        return subscriptionPlanRepository.save(plan).getId();
    }
}

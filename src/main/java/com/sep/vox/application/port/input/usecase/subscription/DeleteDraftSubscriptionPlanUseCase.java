package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteDraftSubscriptionPlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class DeleteDraftSubscriptionPlanUseCase implements IUseCase<DeleteDraftSubscriptionPlanCommand, Void> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    public DeleteDraftSubscriptionPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanQuotaRepository = subscriptionPlanQuotaRepository;
    }

    @Override
    @Transactional
    public Void execute(DeleteDraftSubscriptionPlanCommand input) {
        var plan = subscriptionPlanRepository.findById(input.subscriptionPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // Gói đã publish thì có thể đã có trường mua, và kể cả chưa có thì dòng đơn hàng
        // (order_items.item_id) vẫn trỏ về id này -- xóa cứng là làm mồ côi chứng từ. Cột đó cố ý
        // KHÔNG phải khóa ngoại (đa hình theo order_items.type) nên DB không chặn giúp. DRAFT thì
        // chưa từng bán được nên xóa hẳn mới sạch, thay vì để lại rác ARCHIVED không ai dùng.
        if (plan.getStatus() != SubscriptionPlanStatus.DRAFT) {
            throw new IllegalStateException(
                "Chỉ có thể xóa cứng gói đang ở trạng thái nháp. Gói đã xuất bản thì phải lưu trữ (archive) thay vì xóa.");
        }

        // Xóa hạn mức trước rồi mới tới gói. subscription_plan_quotas.subscription_plan_id KHÔNG có
        // khóa ngoại trỏ về gói (bảng chỉ có mỗi primary key), nên không gì ở tầng DB dọn hộ hay chặn
        // hộ: bỏ dòng này là để lại hạn mức mồ côi trỏ vào một gói không còn tồn tại.
        subscriptionPlanQuotaRepository.deleteBySubscriptionPlanId(plan.getId());
        subscriptionPlanRepository.deleteById(plan.getId());

        return null;
    }
}

package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteDraftPlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class DeleteDraftPlanUseCase implements IUseCase<DeleteDraftPlanCommand, Void> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository planQuotaRepository;
    private final UserContextPort userContextPort;

    public DeleteDraftPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanQuotaRepository planQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(DeleteDraftPlanCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var plan = subscriptionPlanRepository.findById(input.planId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        if (plan.getStatus() != PlanStatus.DRAFT) {
            throw new IllegalStateException(
                "Chỉ có thể xóa cứng gói đang ở trạng thái nháp. Gói đã xuất bản thì phải lưu trữ (archive) thay vì xóa.");
        }

        planQuotaRepository.deleteAllByPlanId(plan.getId());
        subscriptionPlanRepository.deleteById(plan.getId());

        return null;
    }
}

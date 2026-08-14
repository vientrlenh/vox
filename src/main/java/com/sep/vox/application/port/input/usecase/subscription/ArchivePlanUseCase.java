package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ArchivePlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ArchivePlanUseCase implements IUseCase<ArchivePlanCommand, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final UserContextPort userContextPort;

    public ArchivePlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionPlanDto execute(ArchivePlanCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var plan = subscriptionPlanRepository.findById(input.planId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        if (plan.getStatus() == PlanStatus.DRAFT) {
            throw new IllegalStateException("Gói đang ở trạng thái nháp thì phải xóa cứng, không lưu trữ.");
        }
        if (plan.getStatus() == PlanStatus.ARCHIVED) {
            throw new IllegalStateException("Gói đã được lưu trữ trước đó.");
        }

        if (input.replacedByPlanId() != null) {
            if (input.replacedByPlanId().equals(plan.getId())) {
                throw new IllegalArgumentException("Gói thay thế không được trùng với chính gói đang lưu trữ");
            }
            var replacement = subscriptionPlanRepository.findById(input.replacedByPlanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy gói thay thế"));
            if (replacement.getStatus() != PlanStatus.ACTIVE) {
                throw new IllegalArgumentException("Gói thay thế phải đang ở trạng thái hoạt động");
            }
            plan.setReplacedByPlanId(replacement.getId());
        }

        plan.setStatus(PlanStatus.ARCHIVED);
        var savedPlan = subscriptionPlanRepository.save(plan);

        return SubscriptionPlanDtoMapper.toDto(savedPlan, planQuotaRepository.findAllByPlanId(savedPlan.getId()));
    }
}

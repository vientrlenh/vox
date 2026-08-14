package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.PublishPlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class PublishPlanUseCase implements IUseCase<PublishPlanCommand, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final UserContextPort userContextPort;

    public PublishPlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionPlanDto execute(PublishPlanCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var plan = subscriptionPlanRepository.findById(input.planId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        if (plan.getStatus() != PlanStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể xuất bản gói đang ở trạng thái nháp.");
        }

        plan.setStatus(PlanStatus.ACTIVE);
        var savedPlan = subscriptionPlanRepository.save(plan);

        return SubscriptionPlanDtoMapper.toDto(savedPlan, planQuotaRepository.findAllByPlanId(savedPlan.getId()));
    }
}

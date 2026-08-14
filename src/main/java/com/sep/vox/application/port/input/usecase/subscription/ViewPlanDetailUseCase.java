package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewPlanDetailQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class ViewPlanDetailUseCase implements IUseCase<ViewPlanDetailQuery, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final UserContextPort userContextPort;

    public ViewPlanDetailUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanDto execute(ViewPlanDetailQuery input) {
        var plan = subscriptionPlanRepository.findById(input.planId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // Chỉ System Admin mới quản lý gói nên được xem mọi trạng thái (DRAFT/ARCHIVED để biết
        // gói nào chưa xuất bản/đã ngừng bán). Các role khác chỉ được xem gói đang bán — ẩn hẳn
        // sự tồn tại của gói DRAFT/ARCHIVED thay vì trả lỗi quyền, đồng nhất với ViewPlansUseCase.
        if (!userContextPort.isSystemAdmin() && plan.getStatus() != PlanStatus.ACTIVE) {
            throw new NotFoundException("Không tìm thấy gói");
        }

        return SubscriptionPlanDtoMapper.toDto(plan, planQuotaRepository.findAllByPlanId(plan.getId()));
    }
}

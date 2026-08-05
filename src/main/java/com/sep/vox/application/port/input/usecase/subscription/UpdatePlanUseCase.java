package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UpdatePlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class UpdatePlanUseCase implements IUseCase<UpdatePlanCommand, SubscriptionPlanDto> {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public UpdatePlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionPlanDto execute(UpdatePlanCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var plan = subscriptionPlanRepository.findById(input.planId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        // Gói đang có trường ACTIVE thì bị khóa hoàn toàn, kể cả field không liên quan tiền/quota:
        // renewal (CreatePaymentLinkForRenewalUseCase / InvoiceSettlementService) đọc giá và
        // quota LIVE từ đúng plan này tại thời điểm gia hạn, không qua snapshot nào — sửa tại chỗ
        // sẽ âm thầm đổi giá/quota cho trường đang gia hạn mà không qua bước xác nhận
        // (chuỗi replacedByPlanId chỉ kích hoạt khi đổi SANG plan khác, tức đổi id, không phải sửa
        // tại chỗ). Muốn đổi gì thì phải archive gói này và tạo gói mới (kèm replacedByPlanId).
        if (schoolSubscriptionRepository.existsActiveByPlanId(plan.getId())) {
            throw new IllegalStateException(
                "Gói đang được ít nhất một trường sử dụng nên không thể chỉnh sửa. Hãy lưu trữ gói này và tạo gói mới thay thế.");
        }

        if (input.name() != null) {
            plan.setName(input.name());
        }
        if (input.tagline() != null) {
            plan.setTagline(input.tagline());
        }
        if (input.pricePerYear() != null) {
            plan.setPricePerYear(input.pricePerYear());
        }
        if (input.validityDays() != null) {
            plan.setValidityDays(input.validityDays());
        }
        if (input.maxTimePerAttemptMin() != null) {
            plan.setMaxTimePerAttemptMin(input.maxTimePerAttemptMin());
        }
        if (input.maxStudentCount() != null) {
            plan.setMaxStudentCount(input.maxStudentCount());
        }
        if (input.popular() != null) {
            plan.setPopular(input.popular());
        }
        plan.setVersion(plan.getVersion() + 1);
        var savedPlan = subscriptionPlanRepository.save(plan);

        var quotas = planQuotaRepository.findAllByPlanId(savedPlan.getId());
        if (input.quotas() != null) {
            planQuotaRepository.deleteAllByPlanId(savedPlan.getId());
            quotas = input.quotas().stream()
                .map(quotaInput -> planQuotaRepository.save(new PlanQuota(
                    savedPlan.getId(),
                    quotaInput.quotaType(),
                    quotaInput.includedQuantity(),
                    quotaInput.tokenUnitPrice()
                )))
                .toList();
        }

        return SubscriptionPlanDtoMapper.toDto(savedPlan, quotas);
    }
}

package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;

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
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class UpdatePlanUseCase implements IUseCase<UpdatePlanCommand, SubscriptionPlanDto> {

    // plan_quota.included_quantity là numeric(18,6) -- chặn sớm ở đây thay vì để tràn số ở DB
    // (numeric field overflow) khi admin gõ nhầm đơn vị (vd dán số VND vào ô USD).
    private static final BigDecimal MAX_INCLUDED_QUANTITY = new BigDecimal("999999999999.999999");

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final UserContextPort userContextPort;

    public UpdatePlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
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

        // Gói đã publish (ACTIVE/ARCHIVED) bị khóa sửa hoàn toàn, kể cả field không liên quan
        // tiền/quota: renewal (CreatePaymentLinkForRenewalUseCase / InvoiceSettlementService) đọc
        // giá và quota LIVE từ đúng plan này, không qua snapshot nào — sửa tại chỗ sẽ âm thầm đổi
        // giá/quota cho trường đang dùng/gia hạn mà không qua bước xác nhận. Chỉ được sửa khi còn
        // DRAFT (chưa publish nên chưa trường nào dùng); muốn đổi gói đã publish thì phải archive
        // và tạo gói mới (kèm replacedByPlanId).
        if (plan.getStatus() != PlanStatus.DRAFT) {
            throw new IllegalStateException(
                "Chỉ có thể chỉnh sửa gói khi đang ở trạng thái nháp. Gói đã xuất bản thì phải lưu trữ và tạo gói mới thay thế.");
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
        if (input.serviceFeeRatio() != null) {
            plan.setServiceFeeRatio(input.serviceFeeRatio());
        }
        plan.setVersion(plan.getVersion() + 1);
        var savedPlan = subscriptionPlanRepository.save(plan);

        var quotas = planQuotaRepository.findAllByPlanId(savedPlan.getId());
        if (input.quotas() != null) {
            for (var quotaInput : input.quotas()) {
                if (quotaInput.includedQuantity() != null && quotaInput.includedQuantity().compareTo(MAX_INCLUDED_QUANTITY) > 0) {
                    throw new IllegalArgumentException(
                        "Hạn mức \"" + quotaInput.quotaType() + "\" vượt quá giới hạn cho phép (tối đa 999,999,999,999 USD)");
                }
            }
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

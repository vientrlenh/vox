package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.DuplicatedException;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.command.CreatePlanCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.model.subscription.PlanQuota;
import com.sep.vox.domain.model.subscription.PlanStatus;
import com.sep.vox.domain.model.subscription.SubscriptionPlan;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

@Service
public class CreatePlanUseCase implements IUseCase<CreatePlanCommand, SubscriptionPlanDto> {

    private static final BigDecimal DEFAULT_SERVICE_FEE_RATIO = new BigDecimal("0.20");
    // plan_quota.included_quantity là numeric(18,6) -- chặn sớm ở đây thay vì để tràn số ở DB
    // (numeric field overflow) khi admin gõ nhầm đơn vị (vd dán số VND vào ô USD).
    private static final BigDecimal MAX_INCLUDED_QUANTITY = new BigDecimal("999999999999.999999");

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final QuotaPricingPort quotaPricingPort;
    private final UserContextPort userContextPort;

    public CreatePlanUseCase(
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            QuotaPricingPort quotaPricingPort,
            UserContextPort userContextPort) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.quotaPricingPort = quotaPricingPort;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionPlanDto execute(CreatePlanCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        if (input.quotas() == null || input.quotas().isEmpty()) {
            throw new IllegalArgumentException("Gói phải có ít nhất một hạn mức");
        }
        var seenQuotaTypes = new HashSet<>();
        for (var quotaInput : input.quotas()) {
            if (quotaInput.includedQuantity() != null && quotaInput.includedQuantity().compareTo(MAX_INCLUDED_QUANTITY) > 0) {
                throw new IllegalArgumentException(
                    "Hạn mức \"" + quotaInput.quotaType() + "\" vượt quá giới hạn cho phép (tối đa 999,999,999,999 USD)");
            }
            if (!seenQuotaTypes.add(quotaInput.quotaType())) {
                throw new IllegalArgumentException("Hạn mức \"" + quotaInput.quotaType() + "\" bị khai báo trùng lặp");
            }
        }
        if (subscriptionPlanRepository.existsByNameIgnoreCase(input.name())) {
            throw new DuplicatedException("Đã tồn tại gói đăng ký với tên: " + input.name());
        }

        var plan = new SubscriptionPlan(
            input.name(),
            input.tagline(),
            input.pricePerYear(),
            input.validityDays(),
            input.maxTimePerAttemptMin(),
            PlanStatus.DRAFT,
            1,
            Instant.now(),
            userContextPort.getCurrentAuthenticatedUserId(),
            input.serviceFeeRatio() != null ? input.serviceFeeRatio() : DEFAULT_SERVICE_FEE_RATIO
        );
        var savedPlan = subscriptionPlanRepository.save(plan);

        var tokenUnitPrice = quotaPricingPort.tokenUnitPriceFor(savedPlan.getServiceFeeRatio());
        var savedQuotas = input.quotas().stream()
            .map(quotaInput -> planQuotaRepository.save(new PlanQuota(
                savedPlan.getId(),
                quotaInput.quotaType(),
                quotaInput.includedQuantity(),
                tokenUnitPrice
            )))
            .toList();

        return SubscriptionPlanDtoMapper.toDto(savedPlan, savedQuotas);
    }
}

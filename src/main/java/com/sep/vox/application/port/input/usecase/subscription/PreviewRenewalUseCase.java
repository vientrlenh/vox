package com.sep.vox.application.port.input.usecase.subscription;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.PreviewRenewalQuery;
import com.sep.vox.application.port.input.service.RenewalProrationService;
import com.sep.vox.application.port.input.service.SubscriptionPlanResolver;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.RenewalPreviewDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

// Cho FE xem trước, TRƯỚC KHI tạo payment link, gói nào sẽ được dùng khi gia hạn — cần thiết vì
// nếu gói hiện tại đã bị archive và được admin gán gói thay thế (xem resolveActivePlan bên dưới),
// việc gia hạn sẽ tự chuyển sang gói thay thế đó. Trường phải xem và xác nhận (acceptedPlanId ở
// CreatePaymentLinkForRenewalUseCase) trước khi thanh toán, thay vì bị âm thầm đổi gói.
@Service
public class PreviewRenewalUseCase implements IUseCase<PreviewRenewalQuery, RenewalPreviewDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SubscriptionPlanResolver subscriptionPlanResolver;
    private final RenewalProrationService renewalProrationService;
    private final UserContextPort userContextPort;

    public PreviewRenewalUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SubscriptionPlanResolver subscriptionPlanResolver,
            RenewalProrationService renewalProrationService,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.subscriptionPlanResolver = subscriptionPlanResolver;
        this.renewalProrationService = renewalProrationService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public RenewalPreviewDto execute(PreviewRenewalQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var subscription = schoolSubscriptionRepository.findById(input.subscriptionId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
        if (!subscription.getSchoolId().equals(input.schoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var currentPlan = subscriptionPlanRepository.findById(subscription.getPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));
        var renewalPlan = subscriptionPlanResolver.resolveActivePlan(currentPlan);
        var planChanged = !renewalPlan.getId().equals(currentPlan.getId());

        var today = LocalDate.now(DateMapper.DEFAULT_INPUT_ZONE);
        var unusedCredit = renewalProrationService.calculateUnusedCredit(
            subscription, renewalPlan.getPricePerYear(), planChanged, today);
        var amountDue = renewalPlan.getPricePerYear().subtract(unusedCredit);

        return new RenewalPreviewDto(
            planChanged,
            SubscriptionPlanDtoMapper.toDto(currentPlan, planQuotaRepository.findAllByPlanId(currentPlan.getId())),
            SubscriptionPlanDtoMapper.toDto(renewalPlan, planQuotaRepository.findAllByPlanId(renewalPlan.getId())),
            unusedCredit,
            amountDue
        );
    }

}

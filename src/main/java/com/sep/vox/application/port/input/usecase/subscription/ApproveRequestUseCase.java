package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ApproveRequestCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.mapper.SubscriptionRequestDtoMapper;
import com.sep.vox.domain.model.subscription.FinancialEvent;
import com.sep.vox.domain.model.subscription.FinancialEventType;
import com.sep.vox.domain.model.subscription.PaymentMethod;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.RequestType;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.FinancialEventRepository;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

@Service
public class ApproveRequestUseCase implements IUseCase<ApproveRequestCommand, SubscriptionRequestDto> {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanQuotaRepository planQuotaRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final FinancialEventRepository financialEventRepository;
    private final UserContextPort userContextPort;

    public ApproveRequestUseCase(
            SubscriptionRequestRepository subscriptionRequestRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            FinancialEventRepository financialEventRepository,
            UserContextPort userContextPort) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.planQuotaRepository = planQuotaRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.financialEventRepository = financialEventRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionRequestDto execute(ApproveRequestCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var request = subscriptionRequestRepository.findById(input.requestId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu không ở trạng thái chờ duyệt");
        }

        var plan = subscriptionPlanRepository.findById(request.getRequestedPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        var now = Instant.now();

        // one ACTIVE subscription per school
        schoolSubscriptionRepository.findActiveBySchoolId(request.getSchoolId()).ifPresent(current -> {
            current.setStatus(SubscriptionStatus.EXPIRED);
            schoolSubscriptionRepository.save(current);
        });

        var startDate = LocalDate.ofInstant(now, DateMapper.DEFAULT_INPUT_ZONE);
        var subscription = new SchoolSubscription(
            request.getSchoolId(),
            plan.getId(),
            startDate,
            startDate.plusDays(plan.getValidityDays()),
            SubscriptionStatus.ACTIVE,
            request.getAmount(),
            null,
            now
        );
        var savedSubscription = schoolSubscriptionRepository.save(subscription);

        planQuotaRepository.findAllByPlanId(plan.getId()).forEach(planQuota ->
            subscriptionQuotaRepository.save(new SubscriptionQuota(
                savedSubscription.getId(),
                planQuota.getQuotaType(),
                planQuota.getIncludedQuantity(),
                BigDecimal.ZERO
            ))
        );

        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedBy(userContextPort.getCurrentAuthenticatedUserId());
        request.setReviewedAt(now);
        var savedRequest = subscriptionRequestRepository.save(request);

        var eventType = request.getRequestType() == RequestType.UPGRADE
            ? FinancialEventType.SUB_UPGRADED
            : FinancialEventType.SUB_PURCHASED;
        financialEventRepository.save(new FinancialEvent(
            request.getSchoolId(),
            savedSubscription.getId(),
            eventType,
            request.getAmount(),
            "VND",
            PaymentMethod.MANUAL,
            userContextPort.getCurrentAuthenticatedUserId(),
            null,
            now
        ));

        return SubscriptionRequestDtoMapper.toDto(savedRequest);
    }
}

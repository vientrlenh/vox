package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitRequestCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.mapper.SubscriptionRequestDtoMapper;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

@Service
public class SubmitRequestUseCase implements IUseCase<SubmitRequestCommand, SubscriptionRequestDto> {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public SubmitRequestUseCase(
            SubscriptionRequestRepository subscriptionRequestRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            UserContextPort userContextPort) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionRequestDto execute(SubmitRequestCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        // Gói đang bị System Admin cưỡng chế đình chỉ (gian lận...) -- không cho gửi yêu cầu mới cho
        // tới khi được gỡ đình chỉ tường minh, tránh vòng qua ApproveRequestUseCase âm thầm tạo gói
        // ACTIVE mới mà không đụng tới bản ghi SUSPENDED.
        var hasSuspendedSubscription = schoolSubscriptionRepository.findAllBySchoolId(input.schoolId()).stream()
            .anyMatch(subscription -> subscription.getStatus() == SubscriptionStatus.SUSPENDED);
        if (hasSuspendedSubscription) {
            throw new IllegalStateException("Gói đăng ký của trường đang bị đình chỉ, không thể gửi yêu cầu mới.");
        }

        var requestedPlan = subscriptionPlanRepository.findById(input.requestedPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        var now = Instant.now();
        var request = new SubscriptionRequest(
            input.schoolId(),
            input.requestType(),
            input.currentPlanId(),
            input.requestedPlanId(),
            requestedPlan.getPricePerYear(),
            RequestStatus.PENDING,
            now,
            null,
            null
        );
        var saved = subscriptionRequestRepository.save(request);

        return SubscriptionRequestDtoMapper.toDto(saved);
    }
}

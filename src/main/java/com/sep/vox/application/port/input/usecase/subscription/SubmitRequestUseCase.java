package com.sep.vox.application.port.input.usecase.subscription;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.input.command.SubmitRequestCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.mapper.SubscriptionRequestDtoMapper;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

@Service
public class SubmitRequestUseCase implements IUseCase<SubmitRequestCommand, SubscriptionRequestDto> {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;

    public SubmitRequestUseCase(
            SubscriptionRequestRepository subscriptionRequestRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionRequestDto execute(SubmitRequestCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var requestedPlan = subscriptionPlanRepository.findById(input.requestedPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        var school = schoolRepository.findById(input.schoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường"));

        if (requestedPlan.getMaxStudentCount() != null
                && school.getStudentCount().value() > requestedPlan.getMaxStudentCount()) {
            throw new PlanLimitExceededException(
                "Số học sinh của trường (" + school.getStudentCount().value()
                    + ") vượt quá giới hạn của gói \"" + requestedPlan.getName()
                    + "\" (tối đa " + requestedPlan.getMaxStudentCount() + " học sinh), vui lòng chọn gói cao hơn"
            );
        }

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

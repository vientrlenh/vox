package com.sep.vox.application.port.input.usecase.subscription;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.SubmitRequestCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.mapper.SubscriptionRequestDtoMapper;
import com.sep.vox.domain.model.subscription.IdempotencyKey;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.domain.repository.IdempotencyKeyRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

@Service
public class SubmitRequestUseCase implements IUseCase<SubmitRequestCommand, SubscriptionRequestDto> {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final UserContextPort userContextPort;

    public SubmitRequestUseCase(
            SubscriptionRequestRepository subscriptionRequestRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            UserContextPort userContextPort) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionRequestDto execute(SubmitRequestCommand input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var existingKey = idempotencyKeyRepository.findByKey(input.idempotencyKey());
        if (existingKey.isPresent()) {
            var replayed = subscriptionRequestRepository.findById(UUID.fromString(existingKey.get().getResultRef()))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu"));
            return SubscriptionRequestDtoMapper.toDto(replayed);
        }

        var requestedPlan = subscriptionPlanRepository.findById(input.requestedPlanId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói"));

        var now = OffsetDateTime.now();
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

        idempotencyKeyRepository.save(new IdempotencyKey(input.idempotencyKey(), saved.getId().toString(), now));

        return SubscriptionRequestDtoMapper.toDto(saved);
    }
}

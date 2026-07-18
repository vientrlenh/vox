package com.sep.vox.application.port.input.usecase.subscription;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RejectRequestCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.mapper.SubscriptionRequestDtoMapper;
import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

@Service
public class RejectRequestUseCase implements IUseCase<RejectRequestCommand, SubscriptionRequestDto> {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final UserContextPort userContextPort;

    public RejectRequestUseCase(SubscriptionRequestRepository subscriptionRequestRepository, UserContextPort userContextPort) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public SubscriptionRequestDto execute(RejectRequestCommand input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var request = subscriptionRequestRepository.findById(input.requestId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu"));
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu không ở trạng thái chờ duyệt");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedBy(userContextPort.getCurrentAuthenticatedUserId());
        request.setReviewedAt(OffsetDateTime.now());
        var saved = subscriptionRequestRepository.save(request);

        return SubscriptionRequestDtoMapper.toDto(saved);
    }
}

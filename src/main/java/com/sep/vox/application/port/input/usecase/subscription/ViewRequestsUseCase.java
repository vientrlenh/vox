package com.sep.vox.application.port.input.usecase.subscription;

import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewRequestsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.mapper.SubscriptionRequestDtoMapper;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.domain.repository.SubscriptionRequestRepository;

@Service
public class ViewRequestsUseCase implements IUseCase<ViewRequestsQuery, PageResult<SubscriptionRequestDto>> {

    private final SubscriptionRequestRepository subscriptionRequestRepository;
    private final UserContextPort userContextPort;

    public ViewRequestsUseCase(SubscriptionRequestRepository subscriptionRequestRepository, UserContextPort userContextPort) {
        this.subscriptionRequestRepository = subscriptionRequestRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SubscriptionRequestDto> execute(ViewRequestsQuery input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var all = subscriptionRequestRepository.findAllByStatus(input.status()).stream()
            .sorted(Comparator.comparing((SubscriptionRequest request) -> request.getSubmittedAt()).reversed())
            .toList();

        var fromIndex = Math.min(input.page() * input.size(), all.size());
        var toIndex = Math.min(fromIndex + input.size(), all.size());
        var pageContent = SubscriptionRequestDtoMapper.toDtoList(all.subList(fromIndex, toIndex));

        return new PageResult<>(pageContent, input.page(), input.size(), all.size(), (int) Math.ceil((double) all.size() / input.size()));
    }
}

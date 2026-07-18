package com.sep.vox.application.port.input.usecase.subscription;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewUsageQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SubscriptionQuotaDto;
import com.sep.vox.domain.mapper.SubscriptionQuotaDtoMapper;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;

@Service
public class ViewUsageUseCase implements IUseCase<ViewUsageQuery, List<SubscriptionQuotaDto>> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final UserContextPort userContextPort;

    public ViewUsageUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionQuotaDto> execute(ViewUsageQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(input.schoolId());
        if (activeSubscription.isEmpty()) {
            return List.of();
        }

        return SubscriptionQuotaDtoMapper.toDtoList(
            subscriptionQuotaRepository.findAllBySubscriptionId(activeSubscription.get().getId())
        );
    }
}

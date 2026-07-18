package com.sep.vox.application.port.input.usecase.subscription;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewSubscriptionHistoryQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.mapper.SchoolSubscriptionDtoMapper;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
public class ViewSubscriptionHistoryUseCase implements IUseCase<ViewSubscriptionHistoryQuery, List<SchoolSubscriptionDto>> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public ViewSubscriptionHistoryUseCase(SchoolSubscriptionRepository schoolSubscriptionRepository, UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolSubscriptionDto> execute(ViewSubscriptionHistoryQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return schoolSubscriptionRepository.findAllBySchoolId(input.schoolId()).stream()
            .sorted(Comparator.comparing((SchoolSubscription subscription) -> subscription.getCreatedAt()).reversed())
            .map(SchoolSubscriptionDtoMapper::toDto)
            .toList();
    }
}

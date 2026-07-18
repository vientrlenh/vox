package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewCurrentSubscriptionQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.mapper.SchoolSubscriptionDtoMapper;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
public class ViewCurrentSubscriptionUseCase implements IUseCase<ViewCurrentSubscriptionQuery, SchoolSubscriptionDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public ViewCurrentSubscriptionUseCase(SchoolSubscriptionRepository schoolSubscriptionRepository, UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolSubscriptionDto execute(ViewCurrentSubscriptionQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return schoolSubscriptionRepository.findActiveBySchoolId(input.schoolId())
            .map(SchoolSubscriptionDtoMapper::toDto)
            .orElse(null);
    }
}

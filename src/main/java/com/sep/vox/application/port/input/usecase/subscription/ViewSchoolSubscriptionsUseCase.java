package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.mapper.SchoolSubscriptionDtoMapper;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
public class ViewSchoolSubscriptionsUseCase implements IUseCase<ViewSchoolSubscriptionsQuery, PageResult<SchoolSubscriptionDto>> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolSubscriptionsUseCase(SchoolSubscriptionRepository schoolSubscriptionRepository, UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolSubscriptionDto> execute(ViewSchoolSubscriptionsQuery input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        var result = schoolSubscriptionRepository.findAllForAdmin(
            input.planId(),
            input.status(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return SchoolSubscriptionDtoMapper.toDtoPage(result);
    }
}

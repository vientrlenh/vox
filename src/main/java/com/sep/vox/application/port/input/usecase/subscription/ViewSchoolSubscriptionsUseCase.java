package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.subscription.ViewSchoolSubscriptionsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolSubscriptionsUseCase implements IUseCase<ViewSchoolSubscriptionsQuery, PageResult<SchoolSubscriptionDto>> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    @Override
    public PageResult<SchoolSubscriptionDto> execute(ViewSchoolSubscriptionsQuery input) {
        if (!userContextPort.isSystemAdmin()) {
            throw new ForbiddenException("Access denied");
        }
        PageResult<SchoolSubscription> result = schoolSubscriptionRepository.findForAdmin(
            input.planId(),
            input.status(),
            input.keyword(),
            input.page(),
            input.size()
        );
        return SchoolSubscriptionDto.toDtoPage(result);
    }
}

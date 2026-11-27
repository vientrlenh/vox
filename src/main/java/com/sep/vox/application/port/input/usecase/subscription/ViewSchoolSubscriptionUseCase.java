package com.sep.vox.application.port.input.usecase.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.subscription.ViewSchoolSubscriptionQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * The RUNNING subscription period of a school, or null if the school has not bought one or it has expired.
 *
 * <p>"Running" = status ACTIVE and today inside the validity range (findActiveBySchoolId), i.e. exactly the
 * period that every quota gate looks at. A school may have several unfinished rows -- the running period plus a
 * paid period waiting in the queue -- so returning "the newest row" would give a period whose turn has not come
 * and misstate the current quota. To see the whole chain use schoolSubscriptionHistory.
 *
 * <p>Same authorization rule as {@link ViewSchoolSubscriptionHistoryUseCase}: a System Admin may see every
 * school, everyone else only their own.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolSubscriptionUseCase implements IUseCase<ViewSchoolSubscriptionQuery, SchoolSubscriptionDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    @Override
    public SchoolSubscriptionDto execute(ViewSchoolSubscriptionQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
        return schoolSubscriptionRepository.findActiveBySchoolId(input.schoolId())
            .map(SchoolSubscriptionDto::toDto)
            .orElse(null);
    }
}

package com.sep.vox.application.port.input.usecase.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.subscription.ViewQuotaUserAllocationsQuery;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationPageResponse;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

/** The read path of the screen where students are given a speaking-practice quota -- paginated, searchable by name. */
@Service
@RequiredArgsConstructor
public class ViewPracticeQuotaUserAllocationsUseCase
        implements IUseCase<ViewQuotaUserAllocationsQuery, QuotaUserAllocationPageResponse> {

    private final DistributeQuotaToUsersService distributeQuotaToUsersService;

    @Override
    @Transactional(readOnly = true)
    public QuotaUserAllocationPageResponse execute(ViewQuotaUserAllocationsQuery input) {
        return distributeQuotaToUsersService.viewPage(
            input.schoolId(), QuotaType.PRACTICE, SchoolRoleCodes.STUDENT,
            input.search(), input.page(), input.size());
    }
}

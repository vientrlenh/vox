package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewQuotaAllocationsQuery;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuotaUserAllocationSummaryDto;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

@Service
public class ViewPracticeQuotaAllocationsUseCase implements IUseCase<ViewQuotaAllocationsQuery, QuotaUserAllocationSummaryDto> {

    private final DistributeQuotaToUsersService distributeQuotaToUsersService;

    public ViewPracticeQuotaAllocationsUseCase(DistributeQuotaToUsersService distributeQuotaToUsersService) {
        this.distributeQuotaToUsersService = distributeQuotaToUsersService;
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaUserAllocationSummaryDto execute(ViewQuotaAllocationsQuery input) {
        return distributeQuotaToUsersService.view(input.schoolId(), QuotaType.PRACTICE, SchoolRoleCodes.STUDENT);
    }
}

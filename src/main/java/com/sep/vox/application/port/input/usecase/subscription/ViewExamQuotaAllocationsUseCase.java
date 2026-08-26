package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewQuotaAllocationsQuery;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

@Service
public class ViewExamQuotaAllocationsUseCase implements IUseCase<ViewQuotaAllocationsQuery, QuotaUserAllocationSummaryResponse> {

    private final DistributeQuotaToUsersService distributeQuotaToUsersService;

    public ViewExamQuotaAllocationsUseCase(DistributeQuotaToUsersService distributeQuotaToUsersService) {
        this.distributeQuotaToUsersService = distributeQuotaToUsersService;
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaUserAllocationSummaryResponse execute(ViewQuotaAllocationsQuery input) {
        return distributeQuotaToUsersService.view(input.schoolId(), QuotaType.EXAM, SchoolRoleCodes.TEACHER);
    }
}

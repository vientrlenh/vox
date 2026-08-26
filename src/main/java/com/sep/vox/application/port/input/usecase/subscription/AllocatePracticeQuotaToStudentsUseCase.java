package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.AllocatePracticeQuotaCommand;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

@Service
public class AllocatePracticeQuotaToStudentsUseCase implements IUseCase<AllocatePracticeQuotaCommand, QuotaUserAllocationSummaryResponse> {

    private final DistributeQuotaToUsersService distributeQuotaToUsersService;

    public AllocatePracticeQuotaToStudentsUseCase(DistributeQuotaToUsersService distributeQuotaToUsersService) {
        this.distributeQuotaToUsersService = distributeQuotaToUsersService;
    }

    @Override
    @Transactional
    public QuotaUserAllocationSummaryResponse execute(AllocatePracticeQuotaCommand input) {
        return distributeQuotaToUsersService.distribute(
            input.schoolId(), QuotaType.PRACTICE, SchoolRoleCodes.STUDENT, input.mode(), input.allocations()
        );
    }
}

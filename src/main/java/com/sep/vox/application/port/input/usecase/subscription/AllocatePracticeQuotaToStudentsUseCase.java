package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.AllocatePracticeQuotaCommand;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.domain.common.DistributionMode;
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
            input.schoolId(), QuotaType.PRACTICE, SchoolRoleCodes.STUDENT, fromString(input.mode()), input.allocations()
        );
    }

    private static DistributionMode fromString(String mode) {
        if (mode == null) 
            return null;
        try {
            return DistributionMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại phân bổ yêu cầu không hợp lệ: " + mode);
        }
    }
}

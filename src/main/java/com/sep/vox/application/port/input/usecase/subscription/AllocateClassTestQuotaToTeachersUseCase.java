package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.AllocateClassTestQuotaCommand;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.QuotaUserAllocationSummaryDto;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

@Service
public class AllocateClassTestQuotaToTeachersUseCase implements IUseCase<AllocateClassTestQuotaCommand, QuotaUserAllocationSummaryDto> {

    private final DistributeQuotaToUsersService distributeQuotaToUsersService;

    public AllocateClassTestQuotaToTeachersUseCase(DistributeQuotaToUsersService distributeQuotaToUsersService) {
        this.distributeQuotaToUsersService = distributeQuotaToUsersService;
    }

    @Override
    @Transactional
    public QuotaUserAllocationSummaryDto execute(AllocateClassTestQuotaCommand input) {
        return distributeQuotaToUsersService.distribute(
                input.schoolId(), QuotaType.CLASS_TEST, SchoolRoleCodes.TEACHER, input.mode(), input.allocations()
        );
    }
}

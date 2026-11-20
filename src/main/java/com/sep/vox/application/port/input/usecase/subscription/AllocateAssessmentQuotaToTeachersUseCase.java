package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.AllocateAssessmentQuotaCommand;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

/**
 * Allocates a PERSONAL quota to each teacher -- the spending ceiling that the school places on the part of the ASSESSMENT wallet each
 * teacher may spend on their own CLASS TEST.
 *
 * <p>quotaType is ASSESSMENT and not CLASS_TEST: CLASS_TEST is no longer a separate wallet (see QuotaType),
 * the money still sits in the school's ASSESSMENT wallet and the allocation here only limits who may draw how much from
 * that wallet. The fact that this ceiling applies ONLY to class tests is decided by the checking side
 * (ClassTestTokenQuotaGuardService / CompleteAssessmentSessionGradingUseCase only pass userId when
 * {@code kind = CLASS_TEST}), not by quotaType.
 */
@Service
public class AllocateAssessmentQuotaToTeachersUseCase implements IUseCase<AllocateAssessmentQuotaCommand, QuotaUserAllocationSummaryResponse> {

    private final DistributeQuotaToUsersService distributeQuotaToUsersService;

    public AllocateAssessmentQuotaToTeachersUseCase(DistributeQuotaToUsersService distributeQuotaToUsersService) {
        this.distributeQuotaToUsersService = distributeQuotaToUsersService;
    }

    @Override
    @Transactional
    public QuotaUserAllocationSummaryResponse execute(AllocateAssessmentQuotaCommand input) {
        return distributeQuotaToUsersService.distribute(
                input.schoolId(), QuotaType.ASSESSMENT, SchoolRoleCodes.TEACHER, input.mode(),
                input.allocations(), input.confirmWalletDraw()
        );
    }
}

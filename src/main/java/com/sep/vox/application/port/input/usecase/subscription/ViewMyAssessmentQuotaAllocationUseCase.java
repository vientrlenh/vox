package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * Lets a teacher view their own personal ASSESSMENT quota -- the only endpoint reading
 * SchoolSubscriptionQuotaUserAllocation used to be callable only by SCHOOL_ADMIN and returned the whole school, without
 * filtering by the caller (see ViewAssessmentQuotaUserAllocationsUseCase). The FE uses this number to warn
 * before publishing/editing/adding students, with the same formula as ClassTestTokenQuotaGuardService.
 *
 * <p>null = no separate allocation, i.e. not blocked per person (only the school's pool applies),
 * matching exactly the {@code .ifPresent} in ConsumeQuotaService and requireWithinUserAllocation.
 */
@Service
@RequiredArgsConstructor
public class ViewMyAssessmentQuotaAllocationUseCase implements IUseCase<Void, SchoolSubscriptionQuotaUserAllocationDto> {

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolSubscriptionQuotaUserAllocationDto execute(Void input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        UUID schoolId = userContextPort.getCurrentSchoolId();
        if (schoolId == null) {
            return null;
        }

        SchoolSubscription subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).orElse(null);
        if (subscription == null) {
            return null;
        }

        return subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscription.getId(), QuotaType.ASSESSMENT, userId)
            .map(SchoolSubscriptionQuotaUserAllocationDto::toDto)
            .orElse(null);
    }
}
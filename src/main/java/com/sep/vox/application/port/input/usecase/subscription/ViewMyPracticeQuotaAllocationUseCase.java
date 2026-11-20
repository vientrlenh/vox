package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * Lets a student view their own personal PRACTICE quota -- the mirror of
 * ViewMyAssessmentQuotaAllocationUseCase (teacher/ASSESSMENT); the only endpoint reading
 * SchoolSubscriptionQuotaUserAllocation of the PRACTICE type used to be callable only by SCHOOL_ADMIN and returned the whole school,
 * without filtering by the caller (see ViewPracticeQuotaUserAllocationsUseCase).
 *
 * <p>null = the school has NOT allocated a separate quota to this student yet, i.e. completely blocked (see
 * SchoolSubscriptionRepository.findPracticeSpendableFundsVnd -- it no longer falls back to the school pool).
 */
@Service
public class ViewMyPracticeQuotaAllocationUseCase implements IUseCase<Void, SchoolSubscriptionQuotaUserAllocationDto> {

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;

    public ViewMyPracticeQuotaAllocationUseCase(
            UserContextPort userContextPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository) {
        this.userContextPort = userContextPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
    }

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
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscription.getId(), QuotaType.PRACTICE, userId)
            .map(SchoolSubscriptionQuotaUserAllocationDto::toDto)
            .orElse(null);
    }
}
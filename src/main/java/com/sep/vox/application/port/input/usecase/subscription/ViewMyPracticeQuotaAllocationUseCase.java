package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * Cho học sinh tự xem hạn mức PRACTICE cá nhân của chính mình -- mirror của
 * ViewMyExamQuotaAllocationUseCase (giáo viên/EXAM), trước đây endpoint duy nhất đọc
 * SchoolSubscriptionQuotaUserAllocation loại PRACTICE chỉ SCHOOL_ADMIN gọi được và trả cả trường,
 * không lọc theo người gọi (xem ViewPracticeQuotaUserAllocationsUseCase).
 *
 * <p>null = trường CHƯA phân hạn mức riêng cho học sinh này, tức bị chặn hoàn toàn (xem
 * SchoolSubscriptionRepository.findPracticeSpendableFundsVnd -- không còn rơi về pool trường).
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
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var schoolId = userContextPort.getCurrentSchoolId();
        if (schoolId == null) {
            return null;
        }

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).orElse(null);
        if (subscription == null) {
            return null;
        }

        return subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscription.getId(), QuotaType.PRACTICE, userId)
            .map(SchoolSubscriptionQuotaUserAllocationDto::toDto)
            .orElse(null);
    }
}
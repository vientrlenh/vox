package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.MyPracticeQuotaAllocationDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;

/**
 * Cho học sinh tự xem hạn mức PRACTICE cá nhân của chính mình -- mirror của
 * ViewMyClassTestQuotaAllocationUseCase (giáo viên/CLASS_TEST), trước đây endpoint duy nhất đọc
 * SubscriptionQuotaUserAllocation loại PRACTICE chỉ SCHOOL_ADMIN gọi được và trả cả trường,
 * không lọc theo người gọi (xem ViewPracticeQuotaAllocationsUseCase).
 *
 * <p>null = không có allocation riêng, tức không bị chặn theo cá nhân (chỉ pool của trường áp dụng).
 */
@Service
public class ViewMyPracticeQuotaAllocationUseCase implements IUseCase<Void, MyPracticeQuotaAllocationDto> {

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;

    public ViewMyPracticeQuotaAllocationUseCase(
            UserContextPort userContextPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository) {
        this.userContextPort = userContextPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MyPracticeQuotaAllocationDto execute(Void input) {
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
            .findBySubscriptionIdAndQuotaTypeAndUserId(subscription.getId(), QuotaType.PRACTICE, userId)
            .map(allocation -> new MyPracticeQuotaAllocationDto(
                allocation.getAllocatedQuantity(), allocation.getUsedQuantity()))
            .orElse(null);
    }
}
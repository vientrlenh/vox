package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.MyClassTestQuotaAllocationDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * Cho giáo viên tự xem hạn mức CLASS_TEST cá nhân của chính mình -- endpoint duy nhất đọc
 * SchoolSubscriptionQuotaUserAllocation trước đây chỉ SCHOOL_ADMIN gọi được và trả cả trường, không
 * lọc theo người gọi (xem ViewClassTestQuotaAllocationsUseCase). FE dùng số này để cảnh báo
 * trước khi publish/sửa/thêm học sinh, cùng công thức với ClassTestTokenQuotaGuardService.
 *
 * <p>null = không có allocation riêng, tức không bị chặn theo cá nhân (chỉ pool của trường áp dụng),
 * khớp đúng {@code .ifPresent} trong ConsumeQuotaUseCase và requireWithinUserAllocation.
 */
@Service
public class ViewMyClassTestQuotaAllocationUseCase implements IUseCase<Void, MyClassTestQuotaAllocationDto> {

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;

    public ViewMyClassTestQuotaAllocationUseCase(
            UserContextPort userContextPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository) {
        this.userContextPort = userContextPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MyClassTestQuotaAllocationDto execute(Void input) {
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
            .findBySubscriptionIdAndQuotaTypeAndUserId(subscription.getId(), QuotaType.CLASS_TEST, userId)
            .map(allocation -> new MyClassTestQuotaAllocationDto(
                allocation.getAllocatedQuantity(), allocation.getUsedQuantity()))
            .orElse(null);
    }
}
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
 * Cho giáo viên tự xem hạn mức EXAM cá nhân của chính mình -- endpoint duy nhất đọc
 * SchoolSubscriptionQuotaUserAllocation trước đây chỉ SCHOOL_ADMIN gọi được và trả cả trường, không
 * lọc theo người gọi (xem ViewExamQuotaUserAllocationsUseCase). FE dùng số này để cảnh báo
 * trước khi publish/sửa/thêm học sinh, cùng công thức với ClassTestTokenQuotaGuardService.
 *
 * <p>null = không có allocation riêng, tức không bị chặn theo cá nhân (chỉ pool của trường áp dụng),
 * khớp đúng {@code .ifPresent} trong ConsumeQuotaService và requireWithinUserAllocation.
 */
@Service
public class ViewMyExamQuotaAllocationUseCase implements IUseCase<Void, SchoolSubscriptionQuotaUserAllocationDto> {

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;

    public ViewMyExamQuotaAllocationUseCase(
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
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscription.getId(), QuotaType.EXAM, userId)
            .map(SchoolSubscriptionQuotaUserAllocationDto::toDto)
            .orElse(null);
    }
}
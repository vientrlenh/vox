package com.sep.vox.application.port.input.usecase.subscription;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaUserAllocationDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * Cho giáo viên tự xem hạn mức ASSESSMENT cá nhân của chính mình -- endpoint duy nhất đọc
 * SchoolSubscriptionQuotaUserAllocation trước đây chỉ SCHOOL_ADMIN gọi được và trả cả trường, không
 * lọc theo người gọi (xem ViewAssessmentQuotaUserAllocationsUseCase). FE dùng số này để cảnh báo
 * trước khi publish/sửa/thêm học sinh, cùng công thức với ClassTestTokenQuotaGuardService.
 *
 * <p>null = không có allocation riêng, tức không bị chặn theo cá nhân (chỉ pool của trường áp dụng),
 * khớp đúng {@code .ifPresent} trong ConsumeQuotaService và requireWithinUserAllocation.
 */
@Service
public class ViewMyAssessmentQuotaAllocationUseCase implements IUseCase<Void, SchoolSubscriptionQuotaUserAllocationDto> {

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;

    public ViewMyAssessmentQuotaAllocationUseCase(
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
        UUID userId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        UUID schoolId = userContextPort.getCurrentSchoolId().orElse(null);
        if (schoolId == null) {
            return null;
        }

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(schoolId).orElse(null);
        if (subscription == null) {
            return null;
        }

        return subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscription.getId(), QuotaType.ASSESSMENT, userId)
            .map(SchoolSubscriptionQuotaUserAllocationDto::toDto)
            .orElse(null);
    }
}
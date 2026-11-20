package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewQuotaUserAllocationsQuery;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationPageResponse;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

/** Đường đọc của màn chia hạn mức thi cho giáo viên -- có phân trang và tìm kiếm theo tên. */
@Service
public class ViewExamQuotaUserAllocationsUseCase
        implements IUseCase<ViewQuotaUserAllocationsQuery, QuotaUserAllocationPageResponse> {

    private final DistributeQuotaToUsersService distributeQuotaToUsersService;

    public ViewExamQuotaUserAllocationsUseCase(DistributeQuotaToUsersService distributeQuotaToUsersService) {
        this.distributeQuotaToUsersService = distributeQuotaToUsersService;
    }

    @Override
    @Transactional(readOnly = true)
    public QuotaUserAllocationPageResponse execute(ViewQuotaUserAllocationsQuery input) {
        return distributeQuotaToUsersService.viewPage(
            input.schoolId(), QuotaType.EXAM, SchoolRoleCodes.TEACHER,
            input.search(), input.page(), input.size());
    }
}

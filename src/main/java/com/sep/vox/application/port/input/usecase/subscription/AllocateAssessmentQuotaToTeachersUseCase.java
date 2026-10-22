package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.AllocateAssessmentQuotaCommand;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolUserRole;
import com.sep.vox.domain.shared.DistributionMode;

/**
 * Chia hạn mức CÁ NHÂN cho từng giáo viên -- trần chi mà nhà trường đặt lên phần ví ASSESSMENT mỗi giáo
 * viên được tiêu cho bài kiểm tra TRÊN LỚP của mình.
 *
 * <p>quotaType là ASSESSMENT chứ không phải CLASS_TEST: CLASS_TEST không còn là ví riêng (xem QuotaType),
 * tiền vẫn nằm trong ví ASSESSMENT của trường và allocation ở đây chỉ giới hạn ai được rút bao nhiêu từ
 * ví đó. Việc trần này CHỈ áp cho bài kiểm tra trên lớp là do phía soi quyết định
 * (ClassTestTokenQuotaGuardService / CompleteAssessmentSessionGradingUseCase chỉ truyền userId khi
 * {@code kind = CLASS_TEST}), không phải do quotaType.
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
                input.schoolId(), QuotaType.ASSESSMENT, SchoolUserRole.TEACHER, fromString(input.mode()),
                input.allocations(), input.confirmWalletDraw()
        );
    }

    private static DistributionMode fromString(String mode) {
        if (mode == null) 
            return null;
        try {
            return DistributionMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại phân bổ yêu cầu không hợp lệ: " + mode);
        }
    }
}

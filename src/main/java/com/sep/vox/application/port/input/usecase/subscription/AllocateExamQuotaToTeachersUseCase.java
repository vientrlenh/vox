package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.command.AllocateExamQuotaCommand;
import com.sep.vox.application.port.input.service.DistributeQuotaToUsersService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.subscription.QuotaUserAllocationSummaryResponse;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

/**
 * Chia hạn mức CÁ NHÂN cho từng giáo viên -- trần chi mà nhà trường đặt lên phần ví EXAM mỗi giáo
 * viên được tiêu cho bài kiểm tra TRÊN LỚP của mình.
 *
 * <p>quotaType là EXAM chứ không phải CLASS_TEST: CLASS_TEST không còn là ví riêng (xem QuotaType),
 * tiền vẫn nằm trong ví EXAM của trường và allocation ở đây chỉ giới hạn ai được rút bao nhiêu từ
 * ví đó. Việc trần này CHỈ áp cho bài kiểm tra trên lớp là do phía soi quyết định
 * (ClassTestTokenQuotaGuardService / CompleteExamSessionGradingUseCase chỉ truyền userId khi
 * {@code kind = CLASS_TEST}), không phải do quotaType.
 */
@Service
public class AllocateExamQuotaToTeachersUseCase implements IUseCase<AllocateExamQuotaCommand, QuotaUserAllocationSummaryResponse> {

    private final DistributeQuotaToUsersService distributeQuotaToUsersService;

    public AllocateExamQuotaToTeachersUseCase(DistributeQuotaToUsersService distributeQuotaToUsersService) {
        this.distributeQuotaToUsersService = distributeQuotaToUsersService;
    }

    @Override
    @Transactional
    public QuotaUserAllocationSummaryResponse execute(AllocateExamQuotaCommand input) {
        return distributeQuotaToUsersService.distribute(
                input.schoolId(), QuotaType.EXAM, SchoolRoleCodes.TEACHER, input.mode(), input.allocations()
        );
    }
}

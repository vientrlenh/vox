package com.sep.vox.application.response.input.subscription;

import java.util.List;

import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;

/**
 * "Đang bị khóa hay không" KHÔNG thuộc về {@link SchoolSubscriptionQuotaRecordDto} vì nó không phải
 * một cột của ví -- nó là một phép so hai cột của chính ví đó, và chỉ có nghĩa với ví EXAM: đó là
 * điều kiện khóa cấp trường (xem SchoolSubscriptionDebtGuardService), không cho publish/sửa bài và
 * không cho vào thi mới cho tới khi hết nợ. Với PRACTICE nó chỉ mang tính thông tin, vì luồng luyện
 * nói chặn cứng ngay lúc tiêu (allowDebt=false trong SubmitPracticeTurnUseCase) nên không bao giờ
 * đi vào trạng thái này.
 */
public record SchoolSubscriptionQuotaRecordResponse(
    SchoolSubscriptionQuotaRecordDto quota,
    boolean isLocked
) {

    public static SchoolSubscriptionQuotaRecordResponse toResponse(SchoolSubscriptionQuotaRecord domain) {
        return new SchoolSubscriptionQuotaRecordResponse(
            SchoolSubscriptionQuotaRecordDto.toDto(domain),
            domain.getUsedAmountVnd().compareTo(domain.getTotalAllocatedAmountVnd()) > 0
        );
    }

    public static List<SchoolSubscriptionQuotaRecordResponse> toResponseList(
            List<SchoolSubscriptionQuotaRecord> domains) {
        return domains.stream().map(SchoolSubscriptionQuotaRecordResponse::toResponse).toList();
    }
}

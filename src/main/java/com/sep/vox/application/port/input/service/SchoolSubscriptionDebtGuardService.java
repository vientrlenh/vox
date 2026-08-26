package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

/**
 * Trường "bị khóa" khi chi phí AI thật đã trừ (ConsumeQuotaUseCase, allowDebt=true) đẩy
 * usedQuantity vượt totalAllocated ở ví EXAM cấp TRƯỜNG -- KHÔNG tính hạn mức cá nhân của từng giáo
 * viên (SchoolSubscriptionQuotaUserAllocation), vì đó là do trường tự chia nội bộ, không phải tiền
 * thật trường đang thiếu.
 *
 * <p>Chỉ soi EXAM chứ không soi PRACTICE: PRACTICE bị chặn cứng ngay lúc tiêu (allowDebt=false
 * trong SubmitPracticeTurnUseCase) nên không bao giờ rơi vào nợ. Trước đây chỗ này OR thêm
 * CLASS_TEST -- vế đó giờ vô nghĩa vì CLASS_TEST không còn là ví riêng, nó đã nằm trong EXAM
 * (xem QuotaType).
 *
 * <p>Không có cờ/bảng riêng để lưu trạng thái khóa -- suy trực tiếp từ usedQuantity/totalAllocated
 * mỗi lần gọi nên luôn chính xác tức thời, và tự "mở khóa" ngay khi trường mua thêm token/gia hạn
 * (addAllocation) đưa usedQuantity về lại trong hạn mức, không cần bước reconcile nào.
 */
@Service
public class SchoolSubscriptionDebtGuardService {

    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;

    public SchoolSubscriptionDebtGuardService(SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository) {
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
    }

    public boolean isSchoolLocked(UUID subscriptionId) {
        return isQuotaOverLimit(subscriptionId, QuotaType.EXAM);
    }

    public void requireSchoolNotLocked(UUID subscriptionId) {
        if (isSchoolLocked(subscriptionId)) {
            throw new PlanLimitExceededException(
                "Trường đang bị khóa do chi phí AI thực tế vượt hạn mức, vui lòng thanh toán hoặc gia hạn/nâng cấp gói để tiếp tục sử dụng"
            );
        }
    }

    // public (không còn private isOverLimit) -- SchoolDebtEvent audit log (mục "nguyên nhân nợ") cần
    // biết chính xác ví nào vừa transition để ghi vào quota_type, thay vì chỉ biết kết quả gộp của
    // isSchoolLocked. Xem CompleteExamSessionGradingUseCase.
    public boolean isQuotaOverLimit(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .map(quota -> quota.getUsedAmountVnd().compareTo(quota.getTotalAllocatedAmountVnd()) > 0)
            .orElse(false);
    }
}

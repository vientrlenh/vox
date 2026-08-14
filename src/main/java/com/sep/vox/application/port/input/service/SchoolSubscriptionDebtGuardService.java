package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;

/**
 * Trường "bị khóa" khi chi phí AI thật đã trừ (ConsumeQuotaUseCase, allowDebt=true) đẩy
 * usedQuantity vượt totalAllocated ở 1 trong 2 bucket cấp TRƯỜNG (GRADING hoặc CLASS_TEST) --
 * KHÔNG tính hạn mức cá nhân của từng giáo viên (SubscriptionQuotaUserAllocation), vì đó là do
 * trường tự chia nội bộ, không phải tiền thật trường đang thiếu.
 *
 * <p>Không có cờ/bảng riêng để lưu trạng thái khóa -- suy trực tiếp từ usedQuantity/totalAllocated
 * mỗi lần gọi nên luôn chính xác tức thời, và tự "mở khóa" ngay khi trường mua thêm token/gia hạn
 * (addAllocation) đưa usedQuantity về lại trong hạn mức, không cần bước reconcile nào.
 */
@Service
public class SchoolSubscriptionDebtGuardService {

    private final SubscriptionQuotaRepository subscriptionQuotaRepository;

    public SchoolSubscriptionDebtGuardService(SubscriptionQuotaRepository subscriptionQuotaRepository) {
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
    }

    public boolean isSchoolLocked(UUID subscriptionId) {
        return isQuotaOverLimit(subscriptionId, QuotaType.GRADING) || isQuotaOverLimit(subscriptionId, QuotaType.CLASS_TEST);
    }

    public void requireSchoolNotLocked(UUID subscriptionId) {
        if (isSchoolLocked(subscriptionId)) {
            throw new PlanLimitExceededException(
                "Trường đang bị khóa do chi phí AI thực tế vượt hạn mức, vui lòng thanh toán hoặc gia hạn/nâng cấp gói để tiếp tục sử dụng"
            );
        }
    }

    // public (không còn private isOverLimit) -- SchoolDebtEvent audit log (mục "nguyên nhân nợ") cần
    // check TỪNG bucket riêng để biết chính xác bucket nào vừa transition, thay vì chỉ biết kết quả
    // OR gộp của isSchoolLocked. Xem CompleteExamSessionGradingUseCase/InvoiceSettlementService/BuyTokensUseCase.
    public boolean isQuotaOverLimit(UUID subscriptionId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .map(quota -> quota.getUsedQuantity().compareTo(quota.getTotalAllocated()) > 0)
            .orElse(false);
    }
}

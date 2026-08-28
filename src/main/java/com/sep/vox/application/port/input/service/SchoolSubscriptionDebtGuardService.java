package com.sep.vox.application.port.input.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.repository.SchoolBalanceRepository;

/**
 * Trường "bị khóa" khi số dư ví tự nạp âm, tức chi phí AI thật đã tiêu quá cả hạn mức kèm gói lẫn
 * số tiền trường đã nạp -- xem {@link SchoolBalance#isInDebt()}.
 *
 * <p>Khóa theo TRƯỜNG chứ không theo gói đăng ký, và đó là lý do tham số ở đây là {@code schoolId}:
 * số dư thuộc về trường và sống xuyên qua mọi lần gia hạn/đổi gói (V2 mục 2), nên nhận
 * {@code subscriptionId} rồi tự tra ngược sẽ ngụ ý sai rằng nợ thuộc về một kỳ đăng ký nào đó.
 *
 * <p>KHÔNG còn tách theo QuotaType. Trước đây nợ là {@code used_amount_vnd > total_allocated_amount_vnd}
 * của TỪNG ví nên hỏi được "ví thi có đang nợ không"; giờ nợ nằm ở số dư -- một túi duy nhất của
 * trường -- nên câu hỏi đó không còn dữ liệu để trả lời. Ví hạn mức chỉ còn diễn tả "gói cấp bao
 * nhiêu / đã tiêu bao nhiêu trong số đó", và used không bao giờ vượt total nữa (xem
 * ConsumeQuotaService).
 *
 * <p>Không có cờ/bảng riêng lưu trạng thái khóa -- suy trực tiếp từ số dư mỗi lần gọi nên luôn chính
 * xác tức thời, và tự "mở khóa" ngay khi trường nạp thêm tiền đưa số dư về không âm, không cần bước
 * reconcile nào.
 */
@Service
public class SchoolSubscriptionDebtGuardService {

    private final SchoolBalanceRepository schoolBalanceRepository;

    public SchoolSubscriptionDebtGuardService(SchoolBalanceRepository schoolBalanceRepository) {
        this.schoolBalanceRepository = schoolBalanceRepository;
    }

    /**
     * Chưa có dòng số dư nào = trường chưa từng nạp và cũng chưa từng tiêu vượt hạn mức = KHÔNG nợ.
     * Ví rỗng và ví số dư 0 là cùng một nghĩa -- xem {@link SchoolBalance#emptyFor}.
     */
    public boolean isSchoolLocked(UUID schoolId) {
        return schoolBalanceRepository.findBySchoolId(schoolId)
            .map(b -> b.isInDebt())
            .orElse(false);
    }

    public void requireSchoolNotLocked(UUID schoolId) {
        if (isSchoolLocked(schoolId)) {
            throw new PlanLimitExceededException(
                "Trường đang bị khóa do chi phí AI thực tế vượt hạn mức, vui lòng thanh toán hoặc gia hạn/nâng cấp gói để tiếp tục sử dụng"
            );
        }
    }
}

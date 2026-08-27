package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.PlanLimitExceededException;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.response.input.exam.ExamTokenEstimateResponse;
import com.sep.vox.domain.model.exam.Exam;
import com.sep.vox.domain.model.exam.ExamKind;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

/**
 * Ước lượng worst-case chi phí AI (duration × số thí sinh × maxAttempt × estimatedCostPerExamSecondUsd)
 * và chặn khi vượt khả năng chi trả của trường, và với bài trên lớp thì thêm (nếu có) hạn mức cá nhân
 * của giáo viên chủ bài -- vì CompleteExamSessionGradingUseCase trừ thật vào đúng 2 chỗ này khi chấm
 * xong, nên publish/sửa bài phải soi trước cả 2.
 *
 * <p>Mọi phép so ở đây tính bằng VND. Ước lượng sinh ra bằng USD (giá vốn nhà cung cấp niêm yết theo
 * USD) nên được quy đổi NGAY tại {@link #computeEstimatedCostVnd}, không mang xuống dưới: hạn mức và
 * số dư đều là cột VND, và so thẳng một con số USD với một con số VND thì vế trái nhỏ hơn vế phải
 * khoảng 26.000 lần -- cửa chặn vẫn đứng đó nhưng không bao giờ đóng.
 *
 * <p>"Trường còn chi được bao nhiêu" = hạn mức kèm gói còn lại + số dư ví tự nạp, KHÔNG chỉ hạn mức.
 * Từ khi ConsumeQuotaService trừ phần vượt hạn mức sang ví tự nạp (bút toán OVERAGE_CHARGE), một
 * trường cạn hạn mức nhưng đã nạp tiền vẫn chi trả được -- soi mỗi hạn mức ở đây là từ chối đúng cái
 * khoản mà lúc chấm xong mình sẽ vui vẻ thu tiền.
 *
 * <p>Trước đây soi 3 chỗ, trong đó có ví CLASS_TEST cấp trường. Ví đó đã bị bỏ: nó là trần chi nằm
 * trong ví thi chứ không phải túi tiền thứ ba, và soi nó ở đây tức là bắt cùng một khoản chi phải
 * lọt qua hai lần kiểm tra trên cùng một số dư. Trần chi theo GIÁO VIÊN thì vẫn còn -- xem
 * requireWithinUserAllocation và QuotaType.
 *
 * <p>estimatedCostPerExamSecondUsd lấy qua QuotaPricingPort -- ưu tiên giá đã tự calibrate từ
 * dữ liệu thật (QuotaPricingCalibrationJob), fallback về hằng số tĩnh .env
 * (QuotaPricingProperties) khi chưa đủ dữ liệu. Đây vẫn chỉ là số ƯỚC TÍNH, KHÔNG phải chi phí
 * thật -- chi phí thật trừ vào quota lấy từ tổng cost_vnd trong ai_usage_records của session,
 * không nhân theo công thức này.
 *
 * <p>Dùng chung cho lúc publish (UpdateExamStatusUseCase), sửa bài đã publish (UpdateExamUseCase),
 * và thêm thí sinh (AddExamCandidateUseCase/ImportExamCandidatesFromClassUseCase) để không lệch
 * logic giữa các nơi.
 */
@Service
public class ClassTestTokenQuotaGuardService {

    // Trùng scale của school_subscription_quota_records.used_amount_vnd numeric(18,6) -- cùng lý do
    // như RecordAiUsageUseCase: rộng hơn thì Postgres cắt tiếp, hẹp hơn thì con số so sánh ở đây đã
    // lệch khỏi con số sẽ bị trừ thật.
    private static final int COST_VND_SCALE = 6;

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final ExamCandidateRepository examCandidateRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final QuotaPricingPort quotaPricingPort;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    public ClassTestTokenQuotaGuardService(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository,
            SchoolSubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolBalanceRepository schoolBalanceRepository,
            QuotaPricingPort quotaPricingPort,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.examCandidateRepository = examCandidateRepository;
        this.schoolBalanceRepository = schoolBalanceRepository;
        this.quotaPricingPort = quotaPricingPort;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
    }

    public void requireWithinTokenQuota(Exam exam) {
        // Chưa tính được thời gian làm bài (chưa có mã đề/câu hỏi) thì ước tính ra 0 token, không có
        // gì để soi. Chấp cả null lẫn 0 vì hai giá trị này cùng một nghĩa -- cùng idiom với
        // Exam.isScheduleWindowShorterThanExamTime và ExamTimeQuotaGuardService. Bỏ nhánh 0 thì kỳ
        // thi chưa có mã đề bị chặn lên lịch chỉ vì trường chưa cấu hình hạn mức, cho một con số 0.
        if (exam.getExamTimeDurationSecond() == null || exam.getExamTimeDurationSecond() <= 0) {
            return;
        }
        var estimatedCostVnd = computeEstimatedCostVnd(exam);

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId())
            .orElseThrow(() -> new PlanLimitExceededException(
                "Trường chưa có gói subscription đang hoạt động, không thể lên lịch kỳ thi"));

        // Trường đang nợ (số dư ví tự nạp âm) thì chặn luôn ở đây, trước cả khi soi ước lượng
        // worst-case -- xem SchoolSubscriptionDebtGuardService.
        schoolSubscriptionDebtGuardService.requireSchoolNotLocked(subscription.getSchoolId());

        requireSchoolFunds(subscription.getId(), exam.getSchoolId(), QuotaType.EXAM, estimatedCostVnd);

        // Kỳ thi tập trung do nhà trường tổ chức nên không tính vào túi riêng của ai; chỉ bài kiểm
        // tra trên lớp mới đụng tới hạn mức cá nhân mà trường cấp cho giáo viên ra đề.
        if (exam.getKind() == ExamKind.CLASS_TEST) {
            requireWithinUserAllocation(subscription.getId(), exam.getCreatedBy(), estimatedCostVnd);
        }
    }

    /**
     * 0 nếu chưa có mã đề (duration null/0) -- không throw, dùng lại được cho cả requireWithinTokenQuota
     * (chặn) và estimateTokenQuota (chỉ hiển thị cảnh báo, không chặn).
     *
     * <p>Quy đổi sang VND ngay ở đây, một lần: giá/giây do QuotaPricingPort niêm yết bằng USD, còn
     * mọi thứ mà con số này sẽ được đem so (hạn mức, số dư, và cả khoản sẽ bị trừ thật lúc chấm xong)
     * đều là VND. Để phép nhân tỷ giá cho từng chỗ gọi tự làm là chừa sẵn chỗ để quên đúng một chỗ.
     */
    public BigDecimal computeEstimatedCostVnd(Exam exam) {
        if (exam.getExamTimeDurationSecond() == null || exam.getExamTimeDurationSecond() <= 0) {
            return BigDecimal.ZERO;
        }
        var candidateCount = examCandidateRepository.countByExamId(exam.getId());
        var estimatedSeconds = BigDecimal.valueOf((long) exam.getExamTimeDurationSecond() * candidateCount * exam.getMaxAttempt());
        return estimatedSeconds
            .multiply(quotaPricingPort.currentEstimatedCostPerExamSecondUsd())
            .multiply(quotaPricingPort.usdToVndRate())
            .setScale(COST_VND_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Ước lượng chi phí + khả năng chi trả còn lại để hiển thị CẢNH BÁO ngay lúc tạo/sửa bài (trước
     * khi publish) -- KHÔNG throw, kể cả khi trường chưa có subscription active hoặc chưa cấu hình
     * hạn mức (trả về remaining = null cho trường hợp đó thay vì lỗi).
     *
     * <p>Dùng CHUNG hai hàm tính với đường chặn ({@link #computeEstimatedCostVnd},
     * {@link #spendableSchoolFundsVnd}) chứ không tự tính lại: cảnh báo mà lệch khỏi cửa chặn thì
     * hoặc dọa người dùng về một khoản vẫn publish được, hoặc để họ bấm publish rồi mới ăn lỗi.
     */
    public ExamTokenEstimateResponse estimateTokenQuota(Exam exam) {
        var estimatedCostVnd = computeEstimatedCostVnd(exam);
        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(exam.getSchoolId());
        if (subscription.isEmpty()) {
            return new ExamTokenEstimateResponse(estimatedCostVnd, null, null, false, false);
        }

        var subscriptionId = subscription.get().getId();
        var remainingExam = spendableSchoolFundsVnd(subscriptionId, exam.getSchoolId(), QuotaType.EXAM);
        var wouldExceedExam = remainingExam != null && estimatedCostVnd.compareTo(remainingExam) > 0;

        // Cặp thứ hai giờ là hạn mức CÁ NHÂN của giáo viên chủ bài, không còn là ví CLASS_TEST cấp
        // trường. Vẫn giữ hai cảnh báo riêng vì hai bên chặn vì hai lý do khác nhau và cách xử lý
        // cũng khác: hết tiền cấp trường thì phải nạp thêm/nâng gói, còn hết hạn mức cá nhân thì chỉ
        // cần xin quản trị trường cấp thêm.
        BigDecimal remainingMyClassTest = null;
        var wouldExceedMyClassTest = false;
        if (exam.getKind() == ExamKind.CLASS_TEST) {
            remainingMyClassTest = remainingUserAllocation(subscriptionId, exam.getCreatedBy());
            wouldExceedMyClassTest = remainingMyClassTest != null
                && estimatedCostVnd.compareTo(remainingMyClassTest) > 0;
        }

        return new ExamTokenEstimateResponse(
            estimatedCostVnd, remainingExam, remainingMyClassTest, wouldExceedExam, wouldExceedMyClassTest);
    }

    /**
     * Tổng tiền trường còn chi được cho loại hạn mức này = hạn mức kèm gói còn lại + số dư ví tự nạp.
     * null = trường chưa cấu hình ví này (khác hẳn với "đã cấu hình nhưng còn 0đ").
     *
     * <p>Số dư âm được kẹp về 0 chứ không cộng thẳng: số âm là NỢ, mà nợ đã do
     * {@link SchoolSubscriptionDebtGuardService} chặn ở một cửa riêng với thông báo riêng. Cộng nó
     * vào đây sẽ biến một trường đang bị khóa thành một trường "hết hạn mức", tức báo sai lý do và
     * chỉ sai cách khắc phục.
     */
    private BigDecimal spendableSchoolFundsVnd(UUID subscriptionId, UUID schoolId, QuotaType quotaType) {
        return subscriptionQuotaRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .map(quota -> quota.getTotalAllocatedAmountVnd().subtract(quota.getUsedAmountVnd())
                .add(spendableBalanceVnd(schoolId)))
            .orElse(null);
    }

    /** Chỉ ĐỌC số dư (không phải đường ghi) nên dùng findBySchoolId -- xem javadoc SchoolBalanceRepository. */
    private BigDecimal spendableBalanceVnd(UUID schoolId) {
        return schoolBalanceRepository.findBySchoolId(schoolId)
            .map(balance -> balance.getBalanceVnd())
            .map(balanceVnd -> balanceVnd.max(BigDecimal.ZERO))
            .orElse(BigDecimal.ZERO);
    }

    /** null = giáo viên không có hạn mức cá nhân riêng, tức chỉ tiền của trường áp dụng. */
    private BigDecimal remainingUserAllocation(UUID subscriptionId, UUID teacherId) {
        return subscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscriptionId, QuotaType.EXAM, teacherId)
            .map(allocation -> allocation.getAllocatedAmountVnd().subtract(allocation.getUsedAmountVnd()))
            .orElse(null);
    }

    private void requireSchoolFunds(UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            BigDecimal estimatedCostVnd) {
        var remaining = spendableSchoolFundsVnd(subscriptionId, schoolId, quotaType);
        if (remaining == null) {
            throw new PlanLimitExceededException("Không tìm thấy hạn mức " + quotaType + " của gói đăng ký");
        }
        if (estimatedCostVnd.compareTo(remaining) > 0) {
            throw new PlanLimitExceededException(
                "Chi phí ước tính cần dùng (" + estimatedCostVnd + "đ) vượt quá số tiền trường còn chi được cho "
                    + quotaType + " (" + remaining + "đ, gồm hạn mức kèm gói và số dư ví), vui lòng nạp thêm tiền"
                    + " hoặc nâng cấp gói"
            );
        }
    }

    /**
     * Trần chi CÁ NHÂN mà trường tự chia nội bộ. KHÔNG cộng số dư ví trường vào đây: đây là một giới
     * hạn nội bộ chứ không phải một túi tiền, nên cộng tiền của trường vào sẽ xóa sạch chính cái trần
     * mà quản trị trường vừa đặt ra -- xem ConsumeQuotaService.consumeUserAllocation và QuotaType.
     */
    private void requireWithinUserAllocation(UUID subscriptionId, UUID teacherId, BigDecimal estimatedCostVnd) {
        var remaining = remainingUserAllocation(subscriptionId, teacherId);
        if (remaining != null && estimatedCostVnd.compareTo(remaining) > 0) {
            throw new PlanLimitExceededException(
                "Chi phí ước tính cần dùng (" + estimatedCostVnd
                    + "đ) vượt quá hạn mức bài kiểm tra trên lớp cá nhân còn lại (" + remaining
                    + "đ), vui lòng liên hệ quản trị trường để cấp thêm hạn mức"
            );
        }
    }
}

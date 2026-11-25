package com.sep.vox.application.port.input.usecase.subscription;

import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.subscription.FundQuotaFromBalanceCommand;
import com.sep.vox.application.port.input.service.SchoolSubscriptionDebtGuardService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.subscription.QuotaFundingResponse;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Chuyển tiền từ ví TỰ NẠP của trường sang ví HẠN MỨC của một loại quota.
 *
 * <p><b>Vì sao tiền đi vào ví của TRƯỜNG chứ không vào trần chi của một người.</b> Phương án "duyệt
 * bao nhiêu thì trừ ví bấy nhiêu" trừ tiền HAI LẦN: dòng phân bổ không giữ tiền, nên lúc người đó
 * tiêu thật thì {@code tryConsume} vẫn hỏng (ví hạn mức vẫn cạn) và
 * {@code ConsumeQuotaService.chargeOverage} trừ ví lần nữa. Ví mất 800k cho 300k chi phí AI có thật,
 * còn {@code school_ai_spend_entries} chỉ ghi 300k -- hai sổ nói hai con số. Chi tiết trong V12.
 *
 * <p>Nạp vào ví hạn mức thì {@code tryConsume} thấy còn chỗ, {@code chargeOverage} không chạy, và
 * KHÔNG một dòng nào của ConsumeQuotaService phải sửa. Phần chưa tiêu ở lại ví của trường để chia cho
 * người khác, thay vì mắc kẹt trên tên một học sinh không bao giờ vào luyện.
 *
 * <p><b>MỘT CHIỀU, VÀ HIỆN CHƯA GỠ ĐƯỢC.</b> Không có đường chuyển ngược về ví, cũng không chuyển
 * được sang loại quota kia. Nhầm ví thì <b>không có cách sửa</b> -- và đừng tưởng một dòng ADJUSTMENT
 * là cách: nó chỉ CỘNG tiền trở lại ví, trong khi mọi lệnh ghi school_subscription_quota_records
 * ({@code addAllocation}, {@code addFundingFromBalance}) đều chỉ biết cộng, không câu nào hạ được
 * total_allocated_amount_vnd/funded_from_balance_vnd xuống. Làm vậy là trường vừa lấy lại tiền vừa
 * giữ nguyên ví hạn mức đã phình: sinh tiền từ hư không.
 *
 * <p>Gỡ thật thì phải là một đường đi ĐÔI trong cùng transaction -- bút toán cộng ví ĐI KÈM một lệnh
 * hạ ví hạn mức, kẹp sàn ở phần đã tiêu để không đẩy funded xuống dưới 0 hay total xuống dưới used.
 * Chưa có đường đó, nên tuyến phòng thủ duy nhất là giao diện: phải nói to loại ví trước khi xác nhận.
 *
 * <p><b>Thứ tự trong method là bắt buộc, không phải tuỳ tiện</b> -- xem javadoc của
 * {@link SchoolBalanceRepository}: KHÓA dòng số dư trước, tính ở Java, rồi save + ghi bút toán, tất cả
 * trong CÙNG một transaction. Đọc bằng {@code findBySchoolId} rồi trừ sẽ để hai quản trị viên bấm cùng
 * lúc cùng thấy 2tr và cùng tiêu nó.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FundQuotaFromBalanceUseCase implements IUseCase<FundQuotaFromBalanceCommand, QuotaFundingResponse> {

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    @Override
    @Transactional
    public QuotaFundingResponse execute(FundQuotaFromBalanceCommand input) {
        requireSchoolAdminAccess(input.schoolId());

        QuotaType quotaType = input.quotaType();
        BigDecimal amountVnd = requirePositiveAmount(input.amountVnd());
        UUID actorId = userContextPort.getCurrentAuthenticatedUserId();

        SchoolSubscription subscription = requireActiveSubscription(input.schoolId());
        SchoolSubscriptionQuotaRecord pool = requirePool(subscription.getId(), quotaType);
        requireSchoolNotInDebt(input.schoolId());

        BigDecimal balanceAfterVnd = withdrawFromBalance(input, subscription, amountVnd, actorId);

        // After the entry, and in the same transaction: the money leaving the wallet and the money entering the quota
        // wallet live or die together. One statement adds both total and funded -- see addFundingFromBalance.
        quotaRecordRepository.addFundingFromBalance(pool.getId(), amountVnd);

        log.info("Trường {} nạp {}đ từ ví tự nạp vào hạn mức {} (kỳ {}), số dư còn {}đ, người thực hiện {}",
            input.schoolId(), amountVnd, quotaType, subscription.getId(), balanceAfterVnd, actorId);

        return fundingResponse(pool.getId(), amountVnd, balanceAfterVnd);
    }

    private SchoolSubscription requireActiveSubscription(UUID schoolId) {
        return schoolSubscriptionRepository.findActiveBySchoolId(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("Trường chưa có gói subscription đang hoạt động"));
    }

    private SchoolSubscriptionQuotaRecord requirePool(UUID subscriptionId, QuotaType quotaType) {
        return quotaRecordRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạn mức của gói đăng ký"));
    }

    // A school in debt is blocked here, BEFORE looking at the balance -- debt means a negative balance, so the "enough
    // money?" comparison in withdrawFromBalance would also reject, but with the sentence "insufficient balance" instead of
    // one that names the real problem. Same reason ClassTestTokenQuotaGuardService clamps a negative balance to 0 and lets
    // SchoolSubscriptionDebtGuardService answer separately: reporting the wrong reason only points to the wrong remedy.
    private void requireSchoolNotInDebt(UUID schoolId) {
        schoolSubscriptionDebtGuardService.requireSchoolNotLocked(schoolId);
    }

    private BigDecimal withdrawFromBalance(
            FundQuotaFromBalanceCommand input, SchoolSubscription subscription, BigDecimal amountVnd, UUID actorId) {
        Instant now = Instant.now();
        // Lock the balance row. Everything below reads/writes the record holding this lock.
        SchoolBalance balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(input.schoolId(), now);

        if (balance.getBalanceVnd().compareTo(amountVnd) < 0) {
            throw new IllegalArgumentException(
                "Số dư ví tự nạp không đủ: cần " + amountVnd + "đ nhưng chỉ còn "
                    + balance.getBalanceVnd() + "đ");
        }

        // balanceAfterVnd comes from apply() itself rather than subtracting again here: the entry and the aggregate
        // balance must be one number, and recomputing opens exactly the door for them to drift apart (see
        // SchoolBalance.apply).
        BigDecimal balanceAfterVnd = balance.apply(amountVnd.negate(), now);
        schoolBalanceRepository.save(balance);
        schoolBalanceEntryRepository.save(SchoolBalanceEntry.forQuotaFunding(
            input.schoolId(), subscription.getId(), input.quotaType(), amountVnd, balanceAfterVnd,
            actorId, blankToNull(input.reason()), now));
        return balanceAfterVnd;
    }

    private QuotaFundingResponse fundingResponse(UUID poolId, BigDecimal amountVnd, BigDecimal balanceAfterVnd) {
        SchoolSubscriptionQuotaRecord funded = quotaRecordRepository.findById(poolId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạn mức của gói đăng ký"));
        return new QuotaFundingResponse(
            SchoolSubscriptionQuotaRecordDto.toDto(funded), amountVnd, balanceAfterVnd);
    }

    /**
     * Chỉ quản trị của CHÍNH trường đó (hoặc System Admin). Cùng phép kiểm với
     * {@code DistributeQuotaToUsersService.requireSchoolAdminAccess} -- vai trò đã do @PreAuthorize ở
     * controller chặn, phần còn lại ở đây là chặn một quản trị trường thao tác lên trường khác.
     */
    private void requireSchoolAdminAccess(UUID schoolId) {
        if (schoolId == null) {
            throw new IllegalArgumentException("Thiếu mã trường");
        }
        if (!userContextPort.isSystemAdmin() && !schoolId.equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    /**
     * Chặn 0 và số âm. Số âm sẽ là một đường CỘNG tiền vào ví mà không có đơn hàng nào đứng sau (V2:
     * "không cho cộng tiền từ hư không"), còn 0 thì sinh một bút toán vi phạm
     * chk_school_balance_entries_quota_funding_traceable -- chặn ở đây để lỗi ra thành câu tiếng Việt
     * thay vì một DataIntegrityViolationException.
     */
    private static BigDecimal requirePositiveAmount(BigDecimal amountVnd) {
        if (amountVnd == null || amountVnd.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền nạp vào hạn mức phải lớn hơn 0");
        }
        return amountVnd;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

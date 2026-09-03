package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.FundQuotaFromBalanceCommand;
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
 * <p><b>MỘT CHIỀU.</b> Không có đường chuyển ngược về ví, cũng không chuyển được sang loại quota kia;
 * nhầm ví thì chỉ System Admin gỡ được bằng một dòng ADJUSTMENT. Đó là lý do giao diện phải nói to
 * loại ví trước khi xác nhận.
 *
 * <p><b>Thứ tự trong method là bắt buộc, không phải tuỳ tiện</b> -- xem javadoc của
 * {@link SchoolBalanceRepository}: KHÓA dòng số dư trước, tính ở Java, rồi save + ghi bút toán, tất cả
 * trong CÙNG một transaction. Đọc bằng {@code findBySchoolId} rồi trừ sẽ để hai quản trị viên bấm cùng
 * lúc cùng thấy 2tr và cùng tiêu nó.
 */
@Service
public class FundQuotaFromBalanceUseCase implements IUseCase<FundQuotaFromBalanceCommand, QuotaFundingResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FundQuotaFromBalanceUseCase.class);

    private final UserContextPort userContextPort;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository quotaRecordRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService;

    public FundQuotaFromBalanceUseCase(
            UserContextPort userContextPort,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository quotaRecordRepository,
            SchoolBalanceRepository schoolBalanceRepository,
            SchoolBalanceEntryRepository schoolBalanceEntryRepository,
            SchoolSubscriptionDebtGuardService schoolSubscriptionDebtGuardService) {
        this.userContextPort = userContextPort;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.quotaRecordRepository = quotaRecordRepository;
        this.schoolBalanceRepository = schoolBalanceRepository;
        this.schoolBalanceEntryRepository = schoolBalanceEntryRepository;
        this.schoolSubscriptionDebtGuardService = schoolSubscriptionDebtGuardService;
    }

    @Override
    @Transactional
    public QuotaFundingResponse execute(FundQuotaFromBalanceCommand input) {
        requireSchoolAdminAccess(input.schoolId());

        var quotaType = parseQuotaType(input.quotaType());
        var amountVnd = requirePositiveAmount(input.amountVnd());
        var actorId = userContextPort.getCurrentAuthenticatedUserId();

        var subscription = schoolSubscriptionRepository.findActiveBySchoolId(input.schoolId())
            .orElseThrow(() -> new NotFoundException("Trường chưa có gói subscription đang hoạt động"));
        var pool = quotaRecordRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscription.getId(), quotaType)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));

        // Trường đang nợ thì chặn ở đây, TRƯỚC khi soi số dư -- nợ nghĩa là số dư âm, nên phép so
        // "đủ tiền không" bên dưới cũng sẽ từ chối, nhưng với câu "không đủ số dư" thay vì câu nói
        // đúng vấn đề. Cùng lý do mà ClassTestTokenQuotaGuardService kẹp số dư âm về 0 rồi để
        // SchoolSubscriptionDebtGuardService trả lời riêng: báo sai lý do là chỉ sai cách khắc phục.
        schoolSubscriptionDebtGuardService.requireSchoolNotLocked(input.schoolId());

        var now = Instant.now();
        // Khóa dòng số dư. Mọi thứ bên dưới đọc/ghi trên bản ghi đang giữ khóa này.
        var balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(input.schoolId(), now);

        if (balance.getBalanceVnd().compareTo(amountVnd) < 0) {
            throw new IllegalArgumentException(
                "Số dư ví tự nạp không đủ: cần " + amountVnd + "đ nhưng chỉ còn "
                    + balance.getBalanceVnd() + "đ");
        }

        // balanceAfterVnd lấy từ chính apply() chứ không tự trừ lại ở đây: bút toán và số dư tổng hợp
        // buộc phải là một con số, và tính lại là mở đúng cửa cho chúng trôi khỏi nhau (xem
        // SchoolBalance.apply).
        var balanceAfterVnd = balance.apply(amountVnd.negate(), now);
        schoolBalanceRepository.save(balance);
        schoolBalanceEntryRepository.save(SchoolBalanceEntry.forQuotaFunding(
            input.schoolId(), subscription.getId(), quotaType, amountVnd, balanceAfterVnd,
            actorId, blankToNull(input.reason()), now));

        // Sau bút toán, và trong cùng transaction: tiền rời ví và tiền vào ví hạn mức cùng sống hoặc
        // cùng chết. Một câu lệnh cộng cả total lẫn funded -- xem addFundingFromBalance.
        quotaRecordRepository.addFundingFromBalance(pool.getId(), amountVnd);

        LOGGER.info("Trường {} nạp {}đ từ ví tự nạp vào hạn mức {} (kỳ {}), số dư còn {}đ, người thực hiện {}",
            input.schoolId(), amountVnd, quotaType, subscription.getId(), balanceAfterVnd, actorId);

        var funded = quotaRecordRepository.findById(pool.getId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));
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

    private static QuotaType parseQuotaType(String quotaType) {
        try {
            return QuotaType.valueOf(quotaType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Loại hạn mức không hợp lệ: " + quotaType);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.QuotaDebtConfigPort;
import com.sep.vox.application.response.input.subscription.ConsumeQuotaResponse;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Trừ chi phí AI vào hạn mức kèm gói, và khi hạn mức đã cạn thì trừ tiếp vào ví tự nạp của trường.
 *
 * <p>NỢ = {@code school_balances.balance_vnd < 0}, KHÔNG còn là
 * {@code used_amount_vnd > total_allocated_amount_vnd}. Đây là điểm đổi cốt lõi của refactor: hai cột
 * của ví hạn mức giờ chỉ diễn tả "gói cấp bao nhiêu / đã tiêu bao nhiêu trong số đó", nên used KHÔNG
 * BAO GIỜ được vượt total nữa -- phần vượt đã chuyển thành một bút toán trừ trên sổ cái. Xem V2 mục 6
 * ("phần âm ở đây CHÍNH LÀ nợ, thay cho điều kiện used_quantity > total_allocated cũ").
 *
 * <p>Chỉ phần VƯỢT mới đi vào school_balance_entries. Phần còn nằm trong hạn mức đã được đếm ở
 * used_amount_vnd rồi; ghi lại lần nữa ở sổ cái là đếm hai lần cùng một đồng tiền và phá bất biến
 * {@code SUM(entries.amount_vnd) = balance_vnd} (V2 mục 7).
 *
 * <p>Internal service-to-service (gọi từ luồng chấm bài / luyện nói), không end-user-facing -- không
 * kiểm tra school-scoping qua UserContextPort ở đây.
 */
@Service
public class ConsumeQuotaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumeQuotaService.class);

    private final SchoolSubscriptionQuotaRecordRepository schoolSubscriptionQuotaRecordRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository schoolSubscriptionQuotaUserAllocationRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolBalanceRepository schoolBalanceRepository;
    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolDebtNotificationService schoolDebtNotificationService;
    private final QuotaDebtConfigPort quotaDebtConfig;

    public ConsumeQuotaService(
            SchoolSubscriptionQuotaRecordRepository schoolSubscriptionQuotaRecordRepository,
            SchoolSubscriptionQuotaUserAllocationRepository schoolSubscriptionQuotaUserAllocationRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolBalanceRepository schoolBalanceRepository,
            SchoolBalanceEntryRepository schoolBalanceEntryRepository,
            SchoolDebtNotificationService schoolDebtNotificationService,
            QuotaDebtConfigPort quotaDebtConfig) {
        this.schoolSubscriptionQuotaRecordRepository = schoolSubscriptionQuotaRecordRepository;
        this.schoolSubscriptionQuotaUserAllocationRepository = schoolSubscriptionQuotaUserAllocationRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.schoolBalanceRepository = schoolBalanceRepository;
        this.schoolBalanceEntryRepository = schoolBalanceEntryRepository;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
        this.quotaDebtConfig = quotaDebtConfig;
    }

    /**
     * Trừ chi phí của một phiên THI. Ghi nhận ĐỦ khoản chi dù có vượt hạn mức: chi phí AI thật đã phát
     * sinh rồi, từ chối ở đây chỉ làm mất dấu khoản tiền chứ không lấy lại được nó.
     *
     * <p>Không nhận {@code quotaType}: phiên thi luôn trừ vào ví EXAM. Để nó thành tham số là mở cửa
     * cho việc ghi một khoản trừ EXAM kèm id phiên luyện nói (hoặc ngược lại), tức một dòng sổ cái tự
     * mâu thuẫn mà không ràng buộc nào ở tầng Java chặn được.
     *
     * @param amountVnd  chi phí đã quy sang VND -- KHÔNG phải cost_usd
     * @param costUsd    chi phí gốc nhà cung cấp tính, giữ để đối soát ngược với ai_usage_records
     * @param fxRateUsed tỷ giá đã dùng để ra amountVnd, chốt lại vì tỷ giá đổi hằng ngày
     */
    @Transactional
    public ConsumeQuotaResponse consumeExamAllowingDebt(
            UUID subscriptionId, UUID examSessionId, BigDecimal amountVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID userId) {
        return consumeAllowingDebt(subscriptionId, examSessionId, null, QuotaType.EXAM,
            amountVnd, costUsd, fxRateUsed, userId);
    }

    /**
     * Trừ chi phí của một phiên LUYỆN NÓI -- luôn ví PRACTICE.
     *
     * <p>Cũng cho phép tiêu sang số dư như đường thi, và vì cùng một lý do: lúc gọi tới đây thì Azure
     * đã tính tiền xong rồi (turn.turnCostUsd là chi phí THẬT của lượt vừa nói, Python tính đồng bộ
     * trong request submit_turn). Chặn ở đây không giữ lại được đồng nào, chỉ làm khoản chi đó biến
     * mất khỏi sổ sách và VOX âm thầm gánh.
     *
     * <p>Việc học sinh có được nói tiếp hay không là câu hỏi KHÁC, trả lời bằng
     * {@link ConsumeQuotaResponse#fundsExhausted()} chứ không bằng exception -- xem
     * SubmitTurnResultDto.quotaExhausted.
     */
    @Transactional
    public ConsumeQuotaResponse consumePracticeAllowingDebt(
            UUID subscriptionId, UUID practiceSessionId, BigDecimal amountVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID userId) {
        return consumeAllowingDebt(subscriptionId, null, practiceSessionId, QuotaType.PRACTICE,
            amountVnd, costUsd, fxRateUsed, userId);
    }

    private ConsumeQuotaResponse consumeAllowingDebt(
            UUID subscriptionId, UUID examSessionId, UUID practiceSessionId, QuotaType quotaType,
            BigDecimal amountVnd, BigDecimal costUsd, BigDecimal fxRateUsed, UUID userId) {
        var quota = requireQuota(subscriptionId, quotaType);

        // tryConsume là UPDATE có điều kiện nên tự nó đã là chốt chặn -- không đọc-rồi-so ở Java.
        // Lọt qua = khoản này nằm gọn trong hạn mức: không đụng số dư, không sinh bút toán nào.
        var split = schoolSubscriptionQuotaRecordRepository.tryConsume(quota.getId(), amountVnd)
            ? new ChargeSplit(amountVnd, BigDecimal.ZERO, currentBalanceVnd(subscriptionId), false)
            : chargeOverage(subscriptionId, examSessionId, practiceSessionId, quotaType, quota,
                amountVnd, costUsd, fxRateUsed);

        consumeUserAllocation(subscriptionId, quotaType, userId, amountVnd);
        return buildResponse(subscriptionId, quota.getId(), split);
    }

    /**
     * Chia khoản chi làm hai: phần hạn mức còn lại tiêu nốt cho đầy, phần vượt trừ vào ví tự nạp.
     *
     * <p>Khóa dòng số dư TRƯỚC rồi mới đọc lại hạn mức: đó là đường ghi duy nhất theo javadoc của
     * {@link SchoolBalanceRepository} (khóa dòng -> tính ở Java -> save + ghi entry, cùng transaction),
     * và cũng chính khóa đó tuần tự hóa mọi khoản ghi nợ của cùng một trường.
     *
     * <p>Khóa số dư KHÔNG khóa dòng hạn mức, nên {@code remainingVnd} vẫn có thể lệch nếu có
     * addAllocation (gia hạn/nạp thêm) chen ngang. Lệch đó vô hại về tiền: tổng trừ luôn đúng bằng
     * {@code amountVnd} dù chia thế nào, chỉ ranh giới giữa "trừ vào hạn mức" và "trừ vào ví" là xê
     * dịch. Hai lần kẹp bên dưới giữ cho phần lệch đó không bao giờ đẩy used vượt total.
     */
    private ChargeSplit chargeOverage(UUID subscriptionId, UUID examSessionId, UUID practiceSessionId,
            QuotaType quotaType, SchoolSubscriptionQuotaRecord quota, BigDecimal amountVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed) {

        var schoolId = requireSchoolId(subscriptionId);
        var now = Instant.now();
        var balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(schoolId, now);

        var current = requireQuota(subscriptionId, quotaType);
        var remainingVnd = current.getTotalAllocatedAmountVnd()
            .subtract(current.getUsedAmountVnd())
            .max(BigDecimal.ZERO)
            .min(amountVnd);
        var overageVnd = amountVnd.subtract(remainingVnd);

        if (remainingVnd.signum() > 0) {
            schoolSubscriptionQuotaRecordRepository.addUsage(quota.getId(), remainingVnd);
        }

        // Có addAllocation (gia hạn/nạp thêm) chen vào giữa lúc tryConsume hỏng và lần đọc lại trên,
        // nên khoản chi lại vừa đủ nằm trong hạn mức. Không có phần vượt thì KHÔNG ghi bút toán:
        // chk_school_balance_entries_overage_traceable đòi amount_vnd < 0, một dòng 0đ vừa vi phạm
        // ràng buộc vừa vô nghĩa trên sao kê.
        if (overageVnd.signum() <= 0) {
            return new ChargeSplit(remainingVnd, BigDecimal.ZERO, balance.getBalanceVnd(), false);
        }

        var balanceBeforeVnd = balance.getBalanceVnd();
        var balanceAfterVnd = balance.apply(overageVnd.negate(), now);
        schoolBalanceRepository.save(balance);
        schoolBalanceEntryRepository.save(examSessionId != null
            ? SchoolBalanceEntry.forExamOverageCharge(
                schoolId, subscriptionId, examSessionId, quotaType, overageVnd, balanceAfterVnd,
                costUsd, fxRateUsed, now)
            : SchoolBalanceEntry.forPracticeOverageCharge(
                schoolId, subscriptionId, practiceSessionId, quotaType, overageVnd, balanceAfterVnd,
                costUsd, fxRateUsed, now));

        // Truyền CẢ HAI nguồn, y như lúc ghi bút toán ở ngay trên. Bản trước chỉ truyền examSessionId
        // trong khi practiceSessionId nằm ngay trong tầm với, nên mọi dòng CAP_EXCEEDED do luyện nói
        // gây ra đều mất dấu vết.
        checkDebtCapTransition(subscriptionId, schoolId, quotaType, examSessionId, practiceSessionId,
            overageVnd, current.getTotalAllocatedAmountVnd(), balanceBeforeVnd, balanceAfterVnd, now);

        // crossedIntoDebt lấy TRƯỚC/SAU của cùng một dòng đang giữ khóa -- không phải hai lần đọc rời
        // nhau, nên không thể lệch với những gì vừa thực sự xảy ra ở trên.
        var crossedIntoDebt = balanceBeforeVnd.signum() >= 0 && balanceAfterVnd.signum() < 0;

        // Báo "trường vừa bị khoá" NGAY TẠI ĐÂY chứ không để chỗ gọi tự làm.
        //
        // Trước đây việc này nằm ở CompleteExamSessionGradingUseCase, tức chỉ đường CHẤM THI có --
        // luyện nói cũng đẩy trường vào nợ y hệt mà không sinh dòng LOCKED nào, không gửi mail cho
        // ai. Trường vẫn bị khoá thật (guard suy từ dấu số dư, không quan tâm nguồn nào gây ra) nên
        // hiệu trưởng mất quyền mở ca thi mà không có chỗ nào giải thích vì sao.
        //
        // Đặt ở đây thì mọi nguồn trừ tiền hiện tại và tương lai đều được báo, và dòng sự kiện commit
        // CÙNG transaction với bút toán đã gây ra nó -- không còn cửa sổ nào để một cái vào sổ mà cái
        // kia không.
        if (crossedIntoDebt) {
            schoolDebtNotificationService.publishSchoolLockedDueToDebt(
                subscriptionId, schoolId, quotaType, examSessionId, practiceSessionId, amountVnd,
                current.getTotalAllocatedAmountVnd(), balanceAfterVnd.negate().max(BigDecimal.ZERO), now
            );
        }

        return new ChargeSplit(remainingVnd, overageVnd, balanceAfterVnd, crossedIntoDebt);
    }

    /**
     * Trần chi CÁ NHÂN mà trường tự chia nội bộ (giáo viên ra đề kiểm tra trên lớp, học sinh luyện
     * nói). Không có allocation row = trường không chia riêng cho ai = không bị chặn theo cá nhân.
     *
     * <p>Đây là một GIỚI HẠN, không phải một số dư: nó không giữ tiền và không bao giờ sinh bút toán
     * -- vượt trần cá nhân chỉ làm used vượt allocated ở đúng dòng allocation đó, không đụng tới ví
     * trường. Xem QuotaType.
     *
     * <p>Chỉ GHI NHẬN, KHÔNG chặn -- và không còn tham số allowDebt để chọn giữa hai kiểu. Cả hai
     * đường gọi tới đây (thi và luyện nói) đều chạy SAU khi Azure đã tính tiền, nên "từ chối vì vượt
     * trần cá nhân" ở bước này không giữ lại được đồng nào mà chỉ làm khoản chi thật biến mất khỏi
     * bộ đếm của chính người đã tiêu nó. Việc CHẶN theo trần cá nhân xảy ra ở cửa trước khi tốn tiền:
     * ClassTestTokenQuotaGuardService.requireWithinUserAllocation cho bài kiểm tra trên lớp, và
     * SchoolSubscriptionRepository.findPracticeSpendableFundsVnd cho luyện nói.
     */
    private void consumeUserAllocation(UUID subscriptionId, QuotaType quotaType, UUID userId,
            BigDecimal amountVnd) {
        if (userId == null) {
            return;
        }
        schoolSubscriptionQuotaUserAllocationRepository
            .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(subscriptionId, quotaType, userId)
            .ifPresent(allocation ->
                schoolSubscriptionQuotaUserAllocationRepository.addUsage(allocation.getId(), amountVnd));
    }

    /**
     * Chỉ CẢNH BÁO (log + notification SYSTEM_ADMIN), KHÔNG chặn -- chi phí thật đã ghi nhận đủ ở trên
     * rồi. Mục đích là phát hiện sớm nếu pipeline đo chi phí AI có bug làm nợ tăng bất thường, không
     * phải để giới hạn số nợ tối đa.
     *
     * <p>So sánh số dư TRƯỚC/SAU để chỉ báo đúng 1 lần lúc CHUYỂN từ dưới trần sang vượt trần. Cả hai
     * con số đều lấy thẳng từ {@link SchoolBalance#apply} nên không cần đọc lại DB lần thứ hai -- khác
     * bản cũ phải fetch before/after quanh một bulk update và vì thế phải bật {@code clearAutomatically}.
     */
    private void checkDebtCapTransition(UUID subscriptionId, UUID schoolId, QuotaType quotaType,
            UUID examSessionId, UUID practiceSessionId, BigDecimal overageVnd, BigDecimal totalAllocatedVnd,
            BigDecimal balanceBeforeVnd, BigDecimal balanceAfterVnd, Instant now) {
        var cap = totalAllocatedVnd.multiply(quotaDebtConfig.capRatio());
        var debtBeforeVnd = balanceBeforeVnd.negate().max(BigDecimal.ZERO);
        var debtAfterVnd = balanceAfterVnd.negate().max(BigDecimal.ZERO);

        if (debtAfterVnd.compareTo(cap) <= 0 || debtBeforeVnd.compareTo(cap) > 0) {
            return;
        }

        LOGGER.warn(
            "Nợ vượt trần cảnh báo: subscriptionId={} quotaType={} examSessionId={} practiceSessionId={} debtVnd={} capVnd={}",
            subscriptionId, quotaType, examSessionId, practiceSessionId, debtAfterVnd, cap
        );

        schoolDebtNotificationService.publishDebtCapExceeded(
            subscriptionId, schoolId, quotaType, examSessionId, practiceSessionId, overageVnd,
            totalAllocatedVnd, totalAllocatedVnd.add(debtAfterVnd), debtAfterVnd, cap, now
        );
    }

    /** Kết quả chia khoản chi giữa ví hạn mức và số dư -- phần nội bộ của {@link ConsumeQuotaResponse}. */
    private record ChargeSplit(
        BigDecimal chargedToQuotaVnd,
        BigDecimal chargedToBalanceVnd,
        BigDecimal balanceAfterVnd,
        boolean crossedIntoDebt
    ) {
    }

    /**
     * Đọc lại ví hạn mức SAU khi trừ rồi gộp với phần đã tính được lúc còn giữ khóa số dư.
     *
     * <p>{@code fundsExhausted} phải soi CẢ HAI túi: ví hạn mức cạn mà số dư còn tiền thì trường vẫn
     * tiêu tiếp được, nên chỉ nhìn một bên sẽ đóng phiên luyện nói sớm hơn thực tế.
     */
    private ConsumeQuotaResponse buildResponse(UUID subscriptionId, UUID quotaId, ChargeSplit split) {
        var quota = schoolSubscriptionQuotaRecordRepository.findById(quotaId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));

        var quotaRemainingVnd = quota.getTotalAllocatedAmountVnd().subtract(quota.getUsedAmountVnd());
        var fundsExhausted = quotaRemainingVnd.signum() <= 0 && split.balanceAfterVnd().signum() <= 0;

        return new ConsumeQuotaResponse(
            SchoolSubscriptionQuotaRecordDto.toDto(quota),
            split.chargedToQuotaVnd(),
            split.chargedToBalanceVnd(),
            split.balanceAfterVnd(),
            split.crossedIntoDebt(),
            fundsExhausted
        );
    }

    private SchoolSubscriptionQuotaRecord requireQuota(UUID subscriptionId, QuotaType quotaType) {
        return schoolSubscriptionQuotaRecordRepository
            .findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));
    }

    private UUID requireSchoolId(UUID subscriptionId) {
        return schoolSubscriptionRepository.findById(subscriptionId)
            .map(s -> s.getSchoolId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy gói đăng ký"));
    }

    /** Bản CHỈ ĐỌC -- chỉ dùng cho đường KHÔNG ghi số dư (xem javadoc SchoolBalanceRepository). */
    private BigDecimal currentBalanceVnd(UUID subscriptionId) {
        return schoolBalanceRepository.findBySchoolId(requireSchoolId(subscriptionId))
            .map(b -> b.getBalanceVnd())
            .orElse(BigDecimal.ZERO);
    }
}

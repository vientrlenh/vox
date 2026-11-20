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
import com.sep.vox.application.port.output.QuotaUsageWarningConfigPort;
import com.sep.vox.application.response.input.subscription.ConsumeQuotaResponse;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.school.SchoolAiSpendEntry;
import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.model.school.SchoolBalanceEntry;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolAiSpendEntryRepository;
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
    private final SchoolQuotaUsageNotificationService schoolQuotaUsageNotificationService;
    private final QuotaUsageWarningConfigPort quotaUsageWarningConfig;
    private final SchoolAiSpendEntryRepository schoolAiSpendEntryRepository;

    public ConsumeQuotaService(
            SchoolSubscriptionQuotaRecordRepository schoolSubscriptionQuotaRecordRepository,
            SchoolSubscriptionQuotaUserAllocationRepository schoolSubscriptionQuotaUserAllocationRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolBalanceRepository schoolBalanceRepository,
            SchoolBalanceEntryRepository schoolBalanceEntryRepository,
            SchoolDebtNotificationService schoolDebtNotificationService,
            QuotaDebtConfigPort quotaDebtConfig,
            SchoolQuotaUsageNotificationService schoolQuotaUsageNotificationService,
            QuotaUsageWarningConfigPort quotaUsageWarningConfig,
            SchoolAiSpendEntryRepository schoolAiSpendEntryRepository) {
        this.schoolSubscriptionQuotaRecordRepository = schoolSubscriptionQuotaRecordRepository;
        this.schoolSubscriptionQuotaUserAllocationRepository = schoolSubscriptionQuotaUserAllocationRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.schoolBalanceRepository = schoolBalanceRepository;
        this.schoolBalanceEntryRepository = schoolBalanceEntryRepository;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
        this.quotaDebtConfig = quotaDebtConfig;
        this.schoolQuotaUsageNotificationService = schoolQuotaUsageNotificationService;
        this.quotaUsageWarningConfig = quotaUsageWarningConfig;
        this.schoolAiSpendEntryRepository = schoolAiSpendEntryRepository;
    }

    /**
     * Deducts the cost of an ASSESSMENT session. Records the FULL expense even if it exceeds the quota: the real AI cost has already
     * been incurred, and refusing here would only lose track of the money without getting it back.
     *
     * <p>Does not take {@code quotaType}: an assessment session always deducts from the ASSESSMENT wallet. Making it a parameter would open the door
     * to writing an ASSESSMENT deduction carrying the id of a speaking practice session (or the other way round), i.e. a self-contradicting
     * ledger row that no constraint at the Java layer can block.
     *
     * @param amountVnd  cost converted to VND -- NOT cost_usd
     * @param costUsd    the original cost charged by the provider, kept for reconciliation against ai_usage_records
     * @param fxRateUsed the exchange rate used to get amountVnd, recorded because the rate changes daily
     */
    @Transactional
    public ConsumeQuotaResponse consumeAssessmentAllowingDebt(
            UUID subscriptionId, UUID assessmentSessionId, BigDecimal amountVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed, UUID userId) {
        return consumeAllowingDebt(subscriptionId, assessmentSessionId, null, QuotaType.ASSESSMENT,
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
            UUID subscriptionId, UUID assessmentSessionId, UUID practiceSessionId, QuotaType quotaType,
            BigDecimal amountVnd, BigDecimal costUsd, BigDecimal fxRateUsed, UUID userId) {
        SchoolSubscriptionQuotaRecord quota = requireQuota(subscriptionId, quotaType);

        // tryConsume là UPDATE có điều kiện nên tự nó đã là chốt chặn -- không đọc-rồi-so ở Java.
        // Lọt qua = khoản này nằm gọn trong hạn mức: không đụng số dư, không sinh bút toán nào.
        ChargeSplit split;
        if (schoolSubscriptionQuotaRecordRepository.tryConsume(quota.getId(), amountVnd)) {
            checkUsageWarningTransition(subscriptionId, quotaType, quota.getTotalAllocatedAmountVnd(),
                quota.getUsedAmountVnd(), quota.getUsedAmountVnd().add(amountVnd), Instant.now());
            split = new ChargeSplit(amountVnd, BigDecimal.ZERO, currentBalanceVnd(subscriptionId), false);
        } else {
            split = chargeOverage(subscriptionId, assessmentSessionId, practiceSessionId, quotaType, quota,
                amountVnd, costUsd, fxRateUsed);
        }

        consumeUserAllocation(subscriptionId, quotaType, userId, amountVnd);
        recordSpend(subscriptionId, assessmentSessionId, practiceSessionId, quotaType, amountVnd, userId);
        return buildResponse(subscriptionId, quota.getId(), split);
    }

    /**
     * Ghi vào sổ chi phí AI của trường -- ĐỦ số tiền, không chỉ phần vượt hạn mức.
     *
     * <p>Đặt ở đây, SAU cả hai nhánh, vì đây là chỗ duy nhất biết đồng thời trường, loại ví, người
     * tiêu, số tiền và phiên. Để chỗ gọi tự ghi thì mỗi nguồn tiêu tiền mới lại là một cơ hội quên,
     * và đường luyện nói vắng mặt khỏi mọi báo cáo suốt thời gian qua chính là hệ quả của kiểu quên
     * đó (xem V10).
     *
     * <p>KHÁC {@code school_balance_entries} ở chỗ nó ghi cả khoản nằm gọn trong hạn mức: sổ kia là
     * sổ cái của VÍ nên chỉ nhận phần vượt, còn sổ này trả lời "trường đã tiêu bao nhiêu". Cộng hai
     * sổ lại là đếm hai lần cùng một đồng.
     *
     * <p>{@code userId} null là một CÂU TRẢ LỜI -- kỳ thi tập trung không thuộc trần chi của ai.
     */
    private void recordSpend(UUID subscriptionId, UUID assessmentSessionId, UUID practiceSessionId,
            QuotaType quotaType, BigDecimal amountVnd, UUID userId) {
        // Ràng buộc chk_..._amount_positive từ chối dòng 0 đồng, và một lần trừ 0 đồng cũng không
        // phải sự kiện đáng ghi -- chặn ở đây thay vì để DB ném giữa luồng chấm bài.
        if (amountVnd == null || amountVnd.signum() <= 0) {
            return;
        }

        UUID schoolId = requireSchoolId(subscriptionId);
        Instant now = Instant.now();
        schoolAiSpendEntryRepository.save(assessmentSessionId != null
            ? SchoolAiSpendEntry.forAssessment(schoolId, subscriptionId, assessmentSessionId, userId, amountVnd, now)
            : SchoolAiSpendEntry.forPractice(
                schoolId, subscriptionId, practiceSessionId, userId, amountVnd, now));
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
    private ChargeSplit chargeOverage(UUID subscriptionId, UUID assessmentSessionId, UUID practiceSessionId,
            QuotaType quotaType, SchoolSubscriptionQuotaRecord quota, BigDecimal amountVnd,
            BigDecimal costUsd, BigDecimal fxRateUsed) {

        UUID schoolId = requireSchoolId(subscriptionId);
        Instant now = Instant.now();
        SchoolBalance balance = schoolBalanceRepository.findBySchoolIdForUpdateOrCreate(schoolId, now);

        SchoolSubscriptionQuotaRecord current = requireQuota(subscriptionId, quotaType);
        BigDecimal remainingVnd = current.getTotalAllocatedAmountVnd()
            .subtract(current.getUsedAmountVnd())
            .max(BigDecimal.ZERO)
            .min(amountVnd);
        BigDecimal overageVnd = amountVnd.subtract(remainingVnd);

        if (remainingVnd.signum() > 0) {
            schoolSubscriptionQuotaRecordRepository.addUsage(quota.getId(), remainingVnd);
            checkUsageWarningTransition(subscriptionId, quotaType, current.getTotalAllocatedAmountVnd(),
                current.getUsedAmountVnd(), current.getUsedAmountVnd().add(remainingVnd), now);
        }

        // Có addAllocation (gia hạn/nạp thêm) chen vào giữa lúc tryConsume hỏng và lần đọc lại trên,
        // nên khoản chi lại vừa đủ nằm trong hạn mức. Không có phần vượt thì KHÔNG ghi bút toán:
        // chk_school_balance_entries_overage_traceable đòi amount_vnd < 0, một dòng 0đ vừa vi phạm
        // ràng buộc vừa vô nghĩa trên sao kê.
        if (overageVnd.signum() <= 0) {
            return new ChargeSplit(remainingVnd, BigDecimal.ZERO, balance.getBalanceVnd(), false);
        }

        BigDecimal balanceBeforeVnd = balance.getBalanceVnd();
        BigDecimal balanceAfterVnd = balance.apply(overageVnd.negate(), now);
        schoolBalanceRepository.save(balance);
        schoolBalanceEntryRepository.save(assessmentSessionId != null
            ? SchoolBalanceEntry.forAssessmentOverageCharge(
                schoolId, subscriptionId, assessmentSessionId, quotaType, overageVnd, balanceAfterVnd,
                costUsd, fxRateUsed, now)
            : SchoolBalanceEntry.forPracticeOverageCharge(
                schoolId, subscriptionId, practiceSessionId, quotaType, overageVnd, balanceAfterVnd,
                costUsd, fxRateUsed, now));

        // Pass BOTH sources, exactly like when writing the ledger entry right above. The previous version only passed assessmentSessionId
        // while practiceSessionId was within reach, so every CAP_EXCEEDED row caused by speaking practice
        // lost its trace.
        checkDebtCapTransition(subscriptionId, schoolId, quotaType, assessmentSessionId, practiceSessionId,
            overageVnd, current.getTotalAllocatedAmountVnd(), balanceBeforeVnd, balanceAfterVnd, now);

        // crossedIntoDebt lấy TRƯỚC/SAU của cùng một dòng đang giữ khóa -- không phải hai lần đọc rời
        // nhau, nên không thể lệch với những gì vừa thực sự xảy ra ở trên.
        boolean crossedIntoDebt = balanceBeforeVnd.signum() >= 0 && balanceAfterVnd.signum() < 0;

        // Announces "the school has just been locked" RIGHT HERE rather than leaving it to the caller.
        //
        // This used to live in CompleteAssessmentSessionGradingUseCase, i.e. only the ASSESSMENT GRADING path had it --
        // speaking practice pushes a school into debt just the same without producing any LOCKED row or sending an email to
        // anyone. The school was still really locked (the guard derives from the sign of the balance, regardless of which source caused it), so
        // the principal lost the right to open assessment sessions with nowhere to explain why.
        //
        // Placed here, every deduction source, present and future, gets announced, and the event row commits in the
        // SAME transaction as the ledger entry that caused it -- there is no window where one gets recorded and the
        // other does not.
        if (crossedIntoDebt) {
            schoolDebtNotificationService.publishSchoolLockedDueToDebt(
                subscriptionId, schoolId, quotaType, assessmentSessionId, practiceSessionId, amountVnd,
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
            UUID assessmentSessionId, UUID practiceSessionId, BigDecimal overageVnd, BigDecimal totalAllocatedVnd,
            BigDecimal balanceBeforeVnd, BigDecimal balanceAfterVnd, Instant now) {
        BigDecimal cap = totalAllocatedVnd.multiply(quotaDebtConfig.capRatio());
        BigDecimal debtBeforeVnd = balanceBeforeVnd.negate().max(BigDecimal.ZERO);
        BigDecimal debtAfterVnd = balanceAfterVnd.negate().max(BigDecimal.ZERO);

        if (debtAfterVnd.compareTo(cap) <= 0 || debtBeforeVnd.compareTo(cap) > 0) {
            return;
        }

        LOGGER.warn(
            "Nợ vượt trần cảnh báo: subscriptionId={} quotaType={} assessmentSessionId={} practiceSessionId={} debtVnd={} capVnd={}",
            subscriptionId, quotaType, assessmentSessionId, practiceSessionId, debtAfterVnd, cap
        );

        schoolDebtNotificationService.publishDebtCapExceeded(
            subscriptionId, schoolId, quotaType, assessmentSessionId, practiceSessionId, overageVnd,
            totalAllocatedVnd, totalAllocatedVnd.add(debtAfterVnd), debtAfterVnd, cap, now
        );
    }

    /**
     * Chỉ CẢNH BÁO SỚM (log + notification SCHOOL_ADMIN), KHÔNG chặn -- hạn mức CHƯA cạn, đây chỉ là
     * báo trước để trường cân nhắc gia hạn/nạp thêm trước khi bị tính vào nợ (xem
     * {@link #checkDebtCapTransition}).
     *
     * <p>So sánh {@code used/total} TRƯỚC/SAU để chỉ báo đúng 1 lần lúc CHUYỂN từ dưới ngưỡng sang
     * vượt ngưỡng -- cùng kỹ thuật crossing-detection với checkDebtCapTransition, nên tự "reset" đúng
     * khi trường gia hạn/nạp thêm làm {@code totalAllocatedVnd} tăng và tỉ lệ tụt xuống dưới ngưỡng.
     */
    private void checkUsageWarningTransition(UUID subscriptionId, QuotaType quotaType,
            BigDecimal totalAllocatedVnd, BigDecimal usedBeforeVnd, BigDecimal usedAfterVnd, Instant now) {
        if (totalAllocatedVnd.signum() <= 0) {
            return;
        }

        BigDecimal threshold = totalAllocatedVnd.multiply(quotaUsageWarningConfig.warningRatio());
        if (usedAfterVnd.compareTo(threshold) < 0 || usedBeforeVnd.compareTo(threshold) >= 0) {
            return;
        }

        LOGGER.info(
            "Hạn mức vượt ngưỡng cảnh báo sớm: subscriptionId={} quotaType={} usedVnd={} totalVnd={}",
            subscriptionId, quotaType, usedAfterVnd, totalAllocatedVnd
        );

        schoolQuotaUsageNotificationService.publishQuotaUsageWarning(
            subscriptionId, requireSchoolId(subscriptionId), quotaType, totalAllocatedVnd, usedAfterVnd, now
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
        SchoolSubscriptionQuotaRecord quota = schoolSubscriptionQuotaRecordRepository.findById(quotaId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));

        BigDecimal quotaRemainingVnd = quota.getTotalAllocatedAmountVnd().subtract(quota.getUsedAmountVnd());
        boolean fundsExhausted = quotaRemainingVnd.signum() <= 0 && split.balanceAfterVnd().signum() <= 0;

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

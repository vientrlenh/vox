package com.sep.vox.application.port.input.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolSubscriptionQuotaRecordDto;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionQuotaRecord;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolBalanceEntryRepository;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaUserAllocationRepository;

// Internal service-to-service use case (called from the exam-session flow), not end-user-facing —
// no UserContextPort school-scoping check here
@Service
public class ConsumeQuotaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumeQuotaService.class);

    private final SchoolSubscriptionQuotaRecordRepository schoolSubscriptionQuotaRecordRepository;
    private final SchoolSubscriptionQuotaUserAllocationRepository schoolSubscriptionQuotaUserAllocationRepository;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolBalanceRepository schoolBalanceRepository; 
    private final SchoolBalanceEntryRepository schoolBalanceEntryRepository;
    private final SchoolDebtNotificationService schoolDebtNotificationService;

    public ConsumeQuotaService(
            SchoolSubscriptionQuotaRecordRepository schoolSubscriptionQuotaRecordRepository,
            SchoolSubscriptionQuotaUserAllocationRepository schoolSubscriptionQuotaUserAllocationRepository,
            SchoolSubscriptionRepository schoolSubscriptionRepository, 
            SchoolBalanceRepository schoolBalanceRepository, 
            SchoolBalanceEntryRepository schoolBalanceEntryRepository, 
            SchoolDebtNotificationService schoolDebtNotificationService) {
        this.schoolSubscriptionQuotaRecordRepository = schoolSubscriptionQuotaRecordRepository;
        this.schoolSubscriptionQuotaUserAllocationRepository = schoolSubscriptionQuotaUserAllocationRepository;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository; 
        this.schoolBalanceRepository = schoolBalanceRepository; 
        this.schoolBalanceEntryRepository = schoolBalanceEntryRepository;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
    }

    @Transactional
    public SchoolSubscriptionQuotaRecordDto execute(UUID subscriptionId, UUID examSessionId, QuotaType quotaType, BigDecimal amountVnd, UUID userId, boolean allowDebt) {
        var quota = schoolSubscriptionQuotaRecordRepository.findBySchoolSubscriptionIdAndQuotaType(subscriptionId, quotaType)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));

        if (allowDebt) {
            // Chi phí AI thật đã phát sinh -- luôn ghi nhận đủ, chấp nhận usedQuantity vượt
            // totalAllocated (ghi nợ) thay vì throw + rollback cả CompleteExamSessionGradingUseCase.
            schoolSubscriptionQuotaRecordRepository.addUsage(quota.getId(), amountVnd);


        } else {
            var consumed = schoolSubscriptionQuotaRecordRepository.tryConsume(quota.getId(), amountVnd);
            if (!consumed) {
                throw new QuotaExceededException("Đã vượt quá hạn mức sử dụng");
            }
        }

        // Không còn lọc theo quotaType: cả hai ví giờ đều có thể có hạn mức cá nhân (EXAM cho giáo
        // viên ra đề kiểm tra trên lớp, PRACTICE cho học sinh). Chỗ gọi quyết định khoản này có
        // thuộc túi riêng của ai không bằng cách truyền/không truyền userId -- xem
        // CompleteExamSessionGradingUseCase. .ifPresent bên dưới vẫn giữ nghĩa "không có allocation
        // riêng thì không bị chặn theo cá nhân".
        if (input.userId() != null) {
            schoolSubscriptionQuotaUserAllocationRepository
                .findBySchoolSubscriptionIdAndQuotaTypeAndUserId(input.subscriptionId(), input.quotaType(), input.userId())
                .ifPresent(allocation -> {
                    if (input.allowDebt()) {
                        schoolSubscriptionQuotaUserAllocationRepository.addUsage(allocation.getId(), input.amount());
                    } else {
                        var consumedByUser = schoolSubscriptionQuotaUserAllocationRepository.tryConsume(allocation.getId(), input.amount());
                        if (!consumedByUser) {
                            throw new QuotaExceededException("Đã vượt quá hạn mức cá nhân");
                        }
                    }
                });
        }

        var updated = schoolSubscriptionQuotaRecordRepository.findById(quota.getId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));

        if (input.allowDebt()) {
            checkDebtCapTransition(input, quota, updated);
        }

        return SchoolSubscriptionQuotaRecordDto.toDto(updated);
    }

    // Chỉ CẢNH BÁO (log + notification SYSTEM_ADMIN), KHÔNG chặn -- chi phí thật đã được ghi nhận đủ
    // ở trên rồi. Mục đích là phát hiện sớm nếu pipeline đo chi phí AI có bug làm nợ tăng bất thường,
    // không phải để giới hạn số nợ tối đa. So sánh TRƯỚC/SAU (quota fetch đầu execute() vs updated
    // fetch cuối) để chỉ báo đúng 1 lần lúc CHUYỂN từ dưới trần sang vượt trần, không báo lặp lại mỗi
    // lần trừ thêm trong lúc đã vượt trần từ trước.
    private void checkDebtCapTransition(ConsumeQuotaCommand input, SchoolSubscriptionQuotaRecord before, SchoolSubscriptionQuotaRecord after) {
        var overageBefore = before.getUsedAmountVnd().subtract(before.getTotalAllocatedAmountVnd());
        var overageAfter = after.getUsedAmountVnd().subtract(after.getTotalAllocatedAmountVnd());
        if (overageAfter.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        var cap = after.getTotalAllocatedAmountVnd().multiply(quotaDebtConfig.capRatio());
        if (overageAfter.compareTo(cap) <= 0) {
            return;
        }
        LOGGER.warn(
            "Nợ hạn mức vượt trần cảnh báo: subscriptionId={} quotaType={} examSessionId={} overageUsd={} capUsd={}",
            input.subscriptionId(), input.quotaType(), input.examSessionId(), overageAfter, cap
        );
        if (overageBefore.compareTo(cap) <= 0) {
            var schoolId = schoolSubscriptionRepository.findById(input.subscriptionId())
                .map(subscription -> subscription.getSchoolId())
                .orElse(null);
            if (schoolId != null) {
                schoolDebtNotificationService.publishDebtCapExceeded(
                    input.subscriptionId(), schoolId, input.quotaType(), input.examSessionId(), input.amount(),
                    after.getTotalAllocatedAmountVnd(), after.getUsedAmountVnd(), overageAfter, cap, Instant.now()
                );
            }
        }
    }
}

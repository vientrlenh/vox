package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.QuotaDebtConfigPort;
import com.sep.vox.domain.dto.SubscriptionQuotaDto;
import com.sep.vox.domain.mapper.SubscriptionQuotaDtoMapper;
import com.sep.vox.application.port.input.service.SchoolDebtNotificationService;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionQuota;
import com.sep.vox.domain.model.subscription.TokenUsageEvent;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.TokenUsageEventRepository;

// Internal service-to-service use case (called from the exam-session flow), not end-user-facing —
// no UserContextPort school-scoping check here
@Service
public class ConsumeQuotaUseCase implements IUseCase<ConsumeQuotaCommand, SubscriptionQuotaDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumeQuotaUseCase.class);

    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final TokenUsageEventRepository tokenUsageEventRepository;
    private final QuotaDebtConfigPort quotaDebtConfig;
    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolDebtNotificationService schoolDebtNotificationService;

    public ConsumeQuotaUseCase(
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            TokenUsageEventRepository tokenUsageEventRepository,
            QuotaDebtConfigPort quotaDebtConfig,
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolDebtNotificationService schoolDebtNotificationService) {
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.tokenUsageEventRepository = tokenUsageEventRepository;
        this.quotaDebtConfig = quotaDebtConfig;
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.schoolDebtNotificationService = schoolDebtNotificationService;
    }

    @Override
    @Transactional
    public SubscriptionQuotaDto execute(ConsumeQuotaCommand input) {
        var quota = subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(input.subscriptionId(), input.quotaType())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));

        if (input.allowDebt()) {
            // Chi phí AI thật đã phát sinh -- luôn ghi nhận đủ, chấp nhận usedQuantity vượt
            // totalAllocated (ghi nợ) thay vì throw + rollback cả CompleteExamSessionGradingUseCase.
            subscriptionQuotaRepository.addUsage(quota.getId(), input.amount());
        } else {
            var consumed = subscriptionQuotaRepository.tryConsume(quota.getId(), input.amount());
            if (!consumed) {
                throw new QuotaExceededException("Đã vượt quá hạn mức sử dụng", QuotaExceededException.Scope.SCHOOL);
            }
        }

        if (input.userId() != null && (input.quotaType() == QuotaType.CLASS_TEST || input.quotaType() == QuotaType.PRACTICE)) {
            subscriptionQuotaUserAllocationRepository
                .findBySubscriptionIdAndQuotaTypeAndUserId(input.subscriptionId(), input.quotaType(), input.userId())
                .ifPresent(allocation -> {
                    if (input.allowDebt()) {
                        subscriptionQuotaUserAllocationRepository.addUsage(allocation.getId(), input.amount());
                    } else {
                        var consumedByUser = subscriptionQuotaUserAllocationRepository.tryConsume(allocation.getId(), input.amount());
                        if (!consumedByUser) {
                            throw new QuotaExceededException("Đã vượt quá hạn mức cá nhân", QuotaExceededException.Scope.PERSONAL);
                        }
                    }
                });
        }

        tokenUsageEventRepository.save(new TokenUsageEvent(
            input.subscriptionId(), input.examSessionId(), input.quotaType(), input.amount(), Instant.now()
        ));

        var updated = subscriptionQuotaRepository.findById(quota.getId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));

        if (input.allowDebt()) {
            checkDebtCapTransition(input, quota, updated);
        }

        return SubscriptionQuotaDtoMapper.toDto(updated);
    }

    // Chỉ CẢNH BÁO (log + notification SYSTEM_ADMIN), KHÔNG chặn -- chi phí thật đã được ghi nhận đủ
    // ở trên rồi. Mục đích là phát hiện sớm nếu pipeline đo chi phí AI có bug làm nợ tăng bất thường,
    // không phải để giới hạn số nợ tối đa. So sánh TRƯỚC/SAU (quota fetch đầu execute() vs updated
    // fetch cuối) để chỉ báo đúng 1 lần lúc CHUYỂN từ dưới trần sang vượt trần, không báo lặp lại mỗi
    // lần trừ thêm trong lúc đã vượt trần từ trước.
    private void checkDebtCapTransition(ConsumeQuotaCommand input, SubscriptionQuota before, SubscriptionQuota after) {
        var overageBefore = before.getUsedQuantity().subtract(before.getTotalAllocated());
        var overageAfter = after.getUsedQuantity().subtract(after.getTotalAllocated());
        if (overageAfter.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        var cap = after.getTotalAllocated().multiply(quotaDebtConfig.capRatio());
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
                    after.getTotalAllocated(), after.getUsedQuantity(), overageAfter, cap, Instant.now()
                );
            }
        }
    }
}

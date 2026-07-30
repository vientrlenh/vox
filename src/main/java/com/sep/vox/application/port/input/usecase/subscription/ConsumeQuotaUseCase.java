package com.sep.vox.application.port.input.usecase.subscription;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.port.input.command.ConsumeQuotaCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SubscriptionQuotaDto;
import com.sep.vox.domain.mapper.SubscriptionQuotaDtoMapper;
import com.sep.vox.domain.model.subscription.QuotaType;
import com.sep.vox.domain.model.subscription.TokenUsageEvent;
import com.sep.vox.domain.repository.SubscriptionQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionQuotaUserAllocationRepository;
import com.sep.vox.domain.repository.TokenUsageEventRepository;

// Internal service-to-service use case (called from the exam-session flow), not end-user-facing —
// no UserContextPort school-scoping check here
@Service
public class ConsumeQuotaUseCase implements IUseCase<ConsumeQuotaCommand, SubscriptionQuotaDto> {

    private final SubscriptionQuotaRepository subscriptionQuotaRepository;
    private final SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository;
    private final TokenUsageEventRepository tokenUsageEventRepository;

    public ConsumeQuotaUseCase(
            SubscriptionQuotaRepository subscriptionQuotaRepository,
            SubscriptionQuotaUserAllocationRepository subscriptionQuotaUserAllocationRepository,
            TokenUsageEventRepository tokenUsageEventRepository) {
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.subscriptionQuotaUserAllocationRepository = subscriptionQuotaUserAllocationRepository;
        this.tokenUsageEventRepository = tokenUsageEventRepository;
    }

    @Override
    @Transactional
    public SubscriptionQuotaDto execute(ConsumeQuotaCommand input) {
        var quota = subscriptionQuotaRepository.findBySubscriptionIdAndQuotaType(input.subscriptionId(), input.quotaType())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));

        var consumed = subscriptionQuotaRepository.tryConsume(quota.getId(), input.amount());
        if (!consumed) {
            throw new QuotaExceededException("Đã vượt quá hạn mức sử dụng");
        }

        if (input.userId() != null && (input.quotaType() == QuotaType.CLASS_TEST || input.quotaType() == QuotaType.PRACTICE)) {
            subscriptionQuotaUserAllocationRepository
                .findBySubscriptionIdAndQuotaTypeAndUserId(input.subscriptionId(), input.quotaType(), input.userId())
                .ifPresent(allocation -> {
                    var consumedByUser = subscriptionQuotaUserAllocationRepository.tryConsume(allocation.getId(), input.amount());
                    if (!consumedByUser) {
                        throw new QuotaExceededException("Đã vượt quá hạn mức cá nhân");
                    }
                });
        }

        tokenUsageEventRepository.save(new TokenUsageEvent(
            input.subscriptionId(), input.examSessionId(), input.quotaType(), input.amount(), OffsetDateTime.now()
        ));

        var updated = subscriptionQuotaRepository.findById(quota.getId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy hạn mức của gói đăng ký"));
        return SubscriptionQuotaDtoMapper.toDto(updated);
    }
}

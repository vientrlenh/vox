package com.sep.vox.application.port.input.usecase.subscription;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewTokenUsageTimeseriesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.TokenUsageTimeseriesQueryRepository;
import com.sep.vox.domain.dto.TokenUsageTimeseriesDto;
import com.sep.vox.domain.dto.TokenUsageTimeseriesPointDto;
import com.sep.vox.domain.mapper.SubscriptionQuotaDtoMapper;
import com.sep.vox.domain.model.subscription.TokenUsageGranularity;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;
import com.sep.vox.domain.repository.SchoolSubscriptionQuotaRecordRepository;

@Service
public class ViewTokenUsageTimeseriesUseCase implements IUseCase<ViewTokenUsageTimeseriesQuery, TokenUsageTimeseriesDto> {

    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MAX_RANGE_DAYS = 366;

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository;
    private final TokenUsageTimeseriesQueryRepository tokenUsageTimeseriesQueryRepository;
    private final UserContextPort userContextPort;

    public ViewTokenUsageTimeseriesUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            SchoolSubscriptionQuotaRecordRepository subscriptionQuotaRepository,
            TokenUsageTimeseriesQueryRepository tokenUsageTimeseriesQueryRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.subscriptionQuotaRepository = subscriptionQuotaRepository;
        this.tokenUsageTimeseriesQueryRepository = tokenUsageTimeseriesQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenUsageTimeseriesDto execute(ViewTokenUsageTimeseriesQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }

        var granularity = input.granularity() == null ? TokenUsageGranularity.DAY : input.granularity();
        var dateTo = input.dateTo() == null ? Instant.now() : input.dateTo();
        var dateFrom = input.dateFrom() == null ? dateTo.minus(DEFAULT_RANGE_DAYS, ChronoUnit.DAYS) : input.dateFrom();
        var earliestAllowed = dateTo.minus(MAX_RANGE_DAYS, ChronoUnit.DAYS);
        if (dateFrom.isBefore(earliestAllowed)) {
            dateFrom = earliestAllowed;
        }

        var activeSubscription = schoolSubscriptionRepository.findActiveBySchoolId(input.schoolId());
        if (activeSubscription.isEmpty()) {
            return new TokenUsageTimeseriesDto(granularity.name(), BigDecimal.ZERO, List.of(), List.of());
        }
        var subscriptionId = activeSubscription.get().getId();

        var buckets = tokenUsageTimeseriesQueryRepository.findBucketedUsage(
            subscriptionId, dateFrom, dateTo, granularity.name().toLowerCase());

        var points = buckets.stream()
            .map(b -> new TokenUsageTimeseriesPointDto(b.bucket().toString(), b.quotaType(), b.tokensConsumed()))
            .toList();

        var totalUsed = buckets.stream()
            .map(b -> b.tokensConsumed() == null ? BigDecimal.ZERO : b.tokensConsumed())
            .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        var currentPeriod = SubscriptionQuotaDtoMapper.toDtoList(
            subscriptionQuotaRepository.findAllBySubscriptionId(subscriptionId));

        return new TokenUsageTimeseriesDto(granularity.name(), totalUsed, points, currentPeriod);
    }
}

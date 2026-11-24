package com.sep.vox.interfaces.graphql.dataloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.dto.SubscriptionPlanQuotaDto;
import com.sep.vox.domain.repository.SubscriptionPlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class SubscriptionGraphQLDataLoaderConfig {

    public static final String SUBSCRIPTION_PLAN_BY_SUBSCRIPTION_PLAN_ID = "subscriptionPlanBySubscriptionPlanId";
    public static final String SUBSCRIPTION_PLAN_QUOTAS_BY_SUBSCRIPTION_PLAN_ID = "subscriptionPlanQuotasBySubscriptionPlanId";

    private final BatchLoaderRegistry registry;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanQuotaRepository subscriptionPlanQuotaRepository;

    @PostConstruct
    void registerLoaders() {
        registerSubscriptionPlanBySubscriptionPlanId();
        registerSubscriptionPlanQuotasBySubscriptionPlanId();
    }

    private void registerSubscriptionPlanBySubscriptionPlanId() {
        registry.<UUID, SubscriptionPlanDto>forName(SUBSCRIPTION_PLAN_BY_SUBSCRIPTION_PLAN_ID)
            .registerMappedBatchLoader((Set<UUID> planIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> subscriptionPlanRepository.findByIdIn(planIds).stream()
                    .collect(Collectors.toMap(plan -> plan.getId(), plan -> SubscriptionPlanDto.toDto(plan))))
            );
    }

    private void registerSubscriptionPlanQuotasBySubscriptionPlanId() {
        registry.<UUID, List<SubscriptionPlanQuotaDto>>forName(SUBSCRIPTION_PLAN_QUOTAS_BY_SUBSCRIPTION_PLAN_ID)
            .registerMappedBatchLoader((Set<UUID> planIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<SubscriptionPlanQuotaDto>> result = new HashMap<>();
                    planIds.forEach(id -> result.put(id, List.of()));

                    Map<UUID, List<SubscriptionPlanQuotaDto>> quotasByPlanId = subscriptionPlanQuotaRepository
                        .findBySubscriptionPlanIdIn(planIds).stream()
                        .map(SubscriptionPlanQuotaDto::toDto)
                        .collect(Collectors.groupingBy(quota -> quota.subscriptionPlanId()));

                    result.putAll(quotasByPlanId);
                    return result;
                })
            );
    }
}

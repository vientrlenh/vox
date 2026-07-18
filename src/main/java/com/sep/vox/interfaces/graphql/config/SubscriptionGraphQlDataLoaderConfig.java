package com.sep.vox.interfaces.graphql.config;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.mapper.SubscriptionPlanDtoMapper;
import com.sep.vox.domain.repository.PlanQuotaRepository;
import com.sep.vox.domain.repository.SubscriptionPlanRepository;

import reactor.core.publisher.Mono;

@Configuration
public class SubscriptionGraphQlDataLoaderConfig {

    public SubscriptionGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PlanQuotaRepository planQuotaRepository) {

        registry.<UUID, SubscriptionPlanDto>forName("subscriptionPlanById")
            .registerMappedBatchLoader((Set<UUID> planIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> subscriptionPlanRepository.findByIdIn(planIds).stream()
                    .collect(Collectors.toMap(
                        plan -> plan.getId(),
                        plan -> SubscriptionPlanDtoMapper.toDto(plan, planQuotaRepository.findAllByPlanId(plan.getId()))
                    )))
            );
    }
}

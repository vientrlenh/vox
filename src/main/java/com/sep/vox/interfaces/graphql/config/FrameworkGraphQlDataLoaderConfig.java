package com.sep.vox.interfaces.graphql.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.mapper.FrameworkResultBandDtoMapper;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;

import reactor.core.publisher.Mono;

@Configuration
public class FrameworkGraphQlDataLoaderConfig {

    public FrameworkGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {

        registry.<UUID, List<FrameworkCriterionDto>>forName("criteriaByFrameworkVersion")
        .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UUID, List<FrameworkCriterionDto>> result = new HashMap<>();
                versionIds.forEach(id -> result.put(id, List.of()));

                var criteria = frameworkCriterionRepository.findByFrameworkVersionIdIn(versionIds);

                var criterionIds = criteria.stream().map(c -> c.getId()).toList();
                var allBands = criterionIds.isEmpty()
                    ? List.<FrameworkCriterionBand>of()
                    : frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds);

                var bandsByCriterionId = allBands.stream()
                    .collect(Collectors.groupingBy(b -> b.getFrameworkCriterionId()));

                var criteriaByVersionId = criteria.stream()
                    .map(c -> FrameworkCriterionDto.of(c, bandsByCriterionId.getOrDefault(c.getId(), List.of())))
                    .collect(Collectors.groupingBy(b -> b.frameworkVersionId()));

                result.putAll(criteriaByVersionId);
                return result;
            })
        );

        registry.<UUID, List<FrameworkResultBandDto>>forName("resultBandsByFrameworkVersion")
        .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UUID, List<FrameworkResultBandDto>> result = new HashMap<>();
                versionIds.forEach(id -> result.put(id, List.of()));

                var bandsByVersionId = frameworkResultBandRepository.findByFrameworkVersionIdIn(versionIds)
                    .stream()
                    .map(FrameworkResultBandDtoMapper::toDto)
                    .collect(Collectors.groupingBy(b -> b.frameworkVersionId()));

                result.putAll(bandsByVersionId);
                return result;
            })
        );
    }

}

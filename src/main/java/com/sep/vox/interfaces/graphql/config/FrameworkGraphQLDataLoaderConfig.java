package com.sep.vox.interfaces.graphql.config;

import com.sep.vox.domain.model.framework.FrameworkCriterion;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.FrameworkCriterionBandDto;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.mapper.FrameworkCriterionBandDtoMapper;
import com.sep.vox.domain.mapper.FrameworkCriterionDtoMapper;
import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.mapper.FrameworkResultBandDtoMapper;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;

import reactor.core.publisher.Mono;

@Configuration
public class FrameworkGraphQLDataLoaderConfig {

    public FrameworkGraphQLDataLoaderConfig(
            BatchLoaderRegistry registry,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkResultBandRepository frameworkResultBandRepository) {

        registry.<UUID, List<FrameworkCriterionDto>>forName("criteriaByFrameworkVersion")
        .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UUID, List<FrameworkCriterionDto>> result = new HashMap<>();
                versionIds.forEach(id -> result.put(id, List.of()));

                List<FrameworkCriterion> criteria = frameworkCriterionRepository.findByFrameworkVersionIdIn(versionIds);

                Map<UUID, List<FrameworkCriterionDto>> criteriaByVersionId = criteria.stream()
                    .map(c -> FrameworkCriterionDtoMapper.toDto(c))
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

                Map<UUID, List<FrameworkResultBandDto>> bandsByVersionId = frameworkResultBandRepository.findByFrameworkVersionIdIn(versionIds)
                    .stream()
                    .map(FrameworkResultBandDtoMapper::toDto)
                    .collect(Collectors.groupingBy(b -> b.frameworkVersionId()));

                result.putAll(bandsByVersionId);
                return result;
            })
        );

        registry.<UUID, List<FrameworkCriterionBandDto>>forName("criterionBandsByFrameworkCriterion")
            .registerMappedBatchLoader((Set<UUID> criterionIds, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> {
                    Map<UUID, List<FrameworkCriterionBandDto>> result = new HashMap<UUID, List<FrameworkCriterionBandDto>>();
                    criterionIds.forEach(id -> result.put(id, List.of()));

                    Map<UUID, List<FrameworkCriterionBandDto>> criterionBandsByCriterionId = frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds)
                        .stream()
                        .map(FrameworkCriterionBandDtoMapper::toDto)
                        .collect(Collectors.groupingBy(b -> b.frameworkCriterionId()));

                    result.putAll(criterionBandsByCriterionId);
                    return result;
                })
        );
    }

}

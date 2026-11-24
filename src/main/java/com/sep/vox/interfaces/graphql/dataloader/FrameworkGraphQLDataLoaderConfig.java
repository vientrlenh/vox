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

import com.sep.vox.domain.dto.FrameworkCriterionBandDto;
import com.sep.vox.domain.dto.FrameworkCriterionDto;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.dto.FrameworkResultBandDto;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.mapper.FrameworkCriterionBandDtoMapper;
import com.sep.vox.domain.mapper.FrameworkCriterionDtoMapper;
import com.sep.vox.domain.mapper.FrameworkDtoMapper;
import com.sep.vox.domain.mapper.FrameworkResultBandDtoMapper;
import com.sep.vox.domain.mapper.FrameworkVersionDtoMapper;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class FrameworkGraphQLDataLoaderConfig {

    public static final String FRAMEWORK_BY_FRAMEWORK_ID = "frameworkByFrameworkId";
    public static final String FRAMEWORK_VERSION_BY_FRAMEWORK_VERSION_ID = "frameworkVersionByFrameworkVersionId";
    public static final String FRAMEWORK_RESULT_BAND_BY_FRAMEWORK_RESULT_BAND_ID = "frameworkResultBandByFrameworkResultBandId";
    public static final String FRAMEWORK_CRITERIA_BY_FRAMEWORK_VERSION_ID = "frameworkCriteriaByFrameworkVersionId";
    public static final String FRAMEWORK_RESULT_BANDS_BY_FRAMEWORK_VERSION_ID = "frameworkResultBandsByFrameworkVersionId";
    public static final String FRAMEWORK_CRITERION_BANDS_BY_FRAMEWORK_CRITERION_ID = "frameworkCriterionBandsByFrameworkCriterionId";

    private final BatchLoaderRegistry registry;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;

    @PostConstruct
    void registerLoaders() {
        registerFrameworkByFrameworkId();
        registerFrameworkVersionByFrameworkVersionId();
        registerFrameworkResultBandByFrameworkResultBandId();
        registerFrameworkCriteriaByFrameworkVersionId();
        registerFrameworkResultBandsByFrameworkVersionId();
        registerFrameworkCriterionBandsByFrameworkCriterionId();
    }

    private void registerFrameworkByFrameworkId() {
        registry.<UUID, FrameworkDto>forName(FRAMEWORK_BY_FRAMEWORK_ID)
            .registerMappedBatchLoader((Set<UUID> frameworkIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> frameworkRepository.findByIdIn(frameworkIds).stream()
                    .collect(Collectors.toMap(framework -> framework.getId(), FrameworkDtoMapper::toDto)))
            );
    }

    private void registerFrameworkVersionByFrameworkVersionId() {
        registry.<UUID, FrameworkVersionDto>forName(FRAMEWORK_VERSION_BY_FRAMEWORK_VERSION_ID)
            .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> frameworkVersionRepository.findByIdIn(versionIds).stream()
                    .map(FrameworkVersionDtoMapper::toDto)
                    .collect(Collectors.toMap(version -> version.id(), version -> version)))
            );
    }

    private void registerFrameworkResultBandByFrameworkResultBandId() {
        registry.<UUID, FrameworkResultBandDto>forName(FRAMEWORK_RESULT_BAND_BY_FRAMEWORK_RESULT_BAND_ID)
            .registerMappedBatchLoader((Set<UUID> bandIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> frameworkResultBandRepository.findAllByIds(bandIds.stream().toList()).stream()
                    .map(FrameworkResultBandDtoMapper::toDto)
                    .collect(Collectors.toMap(band -> band.id(), band -> band)))
            );
    }

    private void registerFrameworkCriteriaByFrameworkVersionId() {
        registry.<UUID, List<FrameworkCriterionDto>>forName(FRAMEWORK_CRITERIA_BY_FRAMEWORK_VERSION_ID)
            .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<FrameworkCriterionDto>> result = new HashMap<>();
                    versionIds.forEach(id -> result.put(id, List.of()));

                    result.putAll(frameworkCriterionRepository.findByFrameworkVersionIdIn(versionIds).stream()
                        .map(FrameworkCriterionDtoMapper::toDto)
                        .collect(Collectors.groupingBy(criterion -> criterion.frameworkVersionId())));
                    return result;
                })
            );
    }

    private void registerFrameworkResultBandsByFrameworkVersionId() {
        registry.<UUID, List<FrameworkResultBandDto>>forName(FRAMEWORK_RESULT_BANDS_BY_FRAMEWORK_VERSION_ID)
            .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<FrameworkResultBandDto>> result = new HashMap<>();
                    versionIds.forEach(id -> result.put(id, List.of()));

                    result.putAll(frameworkResultBandRepository.findByFrameworkVersionIdIn(versionIds).stream()
                        .map(FrameworkResultBandDtoMapper::toDto)
                        .collect(Collectors.groupingBy(band -> band.frameworkVersionId())));
                    return result;
                })
            );
    }

    private void registerFrameworkCriterionBandsByFrameworkCriterionId() {
        registry.<UUID, List<FrameworkCriterionBandDto>>forName(FRAMEWORK_CRITERION_BANDS_BY_FRAMEWORK_CRITERION_ID)
            .registerMappedBatchLoader((Set<UUID> criterionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<FrameworkCriterionBandDto>> result = new HashMap<>();
                    criterionIds.forEach(id -> result.put(id, List.of()));

                    result.putAll(frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionIds).stream()
                        .map(FrameworkCriterionBandDtoMapper::toDto)
                        .collect(Collectors.groupingBy(band -> band.frameworkCriterionId())));
                    return result;
                })
            );
    }
}

package com.sep.vox.interfaces.graphql.dataloader;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.application.mapper.rubric.RubricCriterionDtoMapper;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.dto.RubricResultBandDto;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.mapper.RubricResultBandDtoMapper;
import com.sep.vox.domain.mapper.RubricVersionDtoMapper;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.interfaces.graphql.key.RubricCriteriaKey;
import com.sep.vox.interfaces.graphql.key.RubricResultBandsKey;
import com.sep.vox.interfaces.graphql.key.RubricVersionsKey;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class RubricGraphQLDataLoaderConfig {

    public static final String RUBRIC_VERSION_BY_RUBRIC_VERSION_ID = "rubricVersionByRubricVersionId";
    public static final String RUBRIC_VERSIONS_BY_RUBRIC_ID = "rubricVersionsByRubricId";
    public static final String RUBRIC_CRITERIA_BY_RUBRIC_VERSION_ID = "rubricCriteriaByRubricVersionId";
    public static final String RUBRIC_RESULT_BANDS_BY_RUBRIC_VERSION_ID = "rubricResultBandsByRubricVersionId";

    private final BatchLoaderRegistry registry;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricCriterionDtoMapper rubricCriterionDtoMapper;

    @PostConstruct
    void registerLoaders() {
        registerRubricVersionByRubricVersionId();
        registerRubricVersionsByRubricId();
        registerRubricCriteriaByRubricVersionId();
        registerRubricResultBandsByRubricVersionId();
    }

    private void registerRubricVersionByRubricVersionId() {
        registry.<UUID, RubricVersionDto>forName(RUBRIC_VERSION_BY_RUBRIC_VERSION_ID)
            .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> rubricVersionRepository.findByIdIn(versionIds).stream()
                    .map(RubricVersionDtoMapper::toRubricVersionDto)
                    .collect(Collectors.toMap(version -> version.id(), version -> version)))
            );
    }

    private void registerRubricVersionsByRubricId() {
        registry.<RubricVersionsKey, PageResult<RubricVersionDto>>forName(RUBRIC_VERSIONS_BY_RUBRIC_ID)
            .registerMappedBatchLoader((Set<RubricVersionsKey> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<RubricVersionsKey, PageResult<RubricVersionDto>> result = new HashMap<>();
                    keys.forEach(key -> result.put(key, emptyPage(key.page(), key.size())));

                    Map<String, List<RubricVersionsKey>> keysByStatus = keys.stream()
                        .collect(Collectors.groupingBy(key -> key.status() == null ? "NULL" : key.status().name()));
                    keysByStatus.forEach((statusName, statusKeys) ->
                        result.putAll(pageVersionsOfStatus("NULL".equals(statusName) ? null : statusName, statusKeys)));
                    return result;
                })
            );
    }

    private Map<RubricVersionsKey, PageResult<RubricVersionDto>> pageVersionsOfStatus(
            String status, List<RubricVersionsKey> keys) {
        List<UUID> rubricIds = keys.stream().map(key -> key.rubricId()).distinct().toList();
        Map<UUID, List<RubricVersion>> versionsByRubricId = rubricVersionRepository
            .findByRubricIdInAndStatus(rubricIds, status).stream()
            .collect(Collectors.groupingBy(version -> version.getRubricId()));

        Map<RubricVersionsKey, PageResult<RubricVersionDto>> pages = new HashMap<>();
        for (RubricVersionsKey key : keys) {
            List<RubricVersion> versions = versionsByRubricId.getOrDefault(key.rubricId(), List.of());
            pages.put(key, pageOf(versions, Comparator.comparingInt((RubricVersion version) -> version.getVersion()).reversed(),
                RubricVersionDtoMapper::toRubricVersionDto, key.page(), key.size()));
        }
        return pages;
    }

    private void registerRubricCriteriaByRubricVersionId() {
        registry.<RubricCriteriaKey, PageResult<RubricCriterionDto>>forName(RUBRIC_CRITERIA_BY_RUBRIC_VERSION_ID)
            .registerMappedBatchLoader((Set<RubricCriteriaKey> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    List<UUID> versionIds = keys.stream().map(key -> key.versionId()).distinct().toList();
                    Map<UUID, List<RubricCriterion>> criteriaByVersionId = rubricCriterionRepository
                        .findByRubricVersionIdIn(versionIds).stream()
                        .collect(Collectors.groupingBy(criterion -> criterion.getRubricVersionId()));

                    Map<RubricCriteriaKey, PageResult<RubricCriterionDto>> result = new HashMap<>();
                    for (RubricCriteriaKey key : keys) {
                        result.put(key, pageOf(criteriaByVersionId.getOrDefault(key.versionId(), List.of()),
                            Comparator.comparingInt((RubricCriterion criterion) -> criterion.getOrder()),
                            rubricCriterionDtoMapper::toDto, key.page(), key.size()));
                    }
                    return result;
                })
            );
    }

    private void registerRubricResultBandsByRubricVersionId() {
        registry.<RubricResultBandsKey, PageResult<RubricResultBandDto>>forName(RUBRIC_RESULT_BANDS_BY_RUBRIC_VERSION_ID)
            .registerMappedBatchLoader((Set<RubricResultBandsKey> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    List<UUID> versionIds = keys.stream().map(key -> key.versionId()).distinct().toList();
                    Map<UUID, List<RubricResultBand>> bandsByVersionId = rubricResultBandRepository
                        .findByRubricVersionIdIn(versionIds).stream()
                        .collect(Collectors.groupingBy(band -> band.getRubricVersionId()));

                    Map<RubricResultBandsKey, PageResult<RubricResultBandDto>> result = new HashMap<>();
                    for (RubricResultBandsKey key : keys) {
                        result.put(key, pageOf(bandsByVersionId.getOrDefault(key.versionId(), List.of()),
                            Comparator.comparingInt((RubricResultBand band) -> band.getOrder()),
                            RubricResultBandDtoMapper::toDto, key.page(), key.size()));
                    }
                    return result;
                })
            );
    }

    private static <M, D> PageResult<D> pageOf(
            List<M> models, Comparator<M> order, Function<M, D> toDto, int page, int size) {
        int totalElements = models.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        // page - 1: page numbers COUNT FROM 1 (the default in graphqls is `page: Int = 1`, and every JPA adapter
        // also subtracts 1 before building the PageRequest). In-memory paging here used to be 0-based, so page 1
        // skipped the whole first page and the content came back EMPTY even though the parent had rows.
        List<D> content = models.stream()
            .sorted(order)
            .skip((long) (page - 1) * size)
            .limit(size)
            .map(toDto)
            .toList();
        return new PageResult<>(content, page, size, totalElements, totalPages);
    }

    private static <D> PageResult<D> emptyPage(int page, int size) {
        return new PageResult<>(List.of(), page, size, 0, 0);
    }
}

package com.sep.vox.interfaces.graphql.config;

import com.sep.vox.application.port.input.query.key.RubricCriteriaKey;
import com.sep.vox.application.port.input.query.key.RubricCriterionBandsKey;
import com.sep.vox.application.port.input.query.key.RubricResultBandsKey;
import com.sep.vox.application.port.input.query.key.RubricVersionsKey;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.*;
import com.sep.vox.domain.mapper.*;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricCriterionBand;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.repository.*;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class RubricGraphQlDataLoaderConfig {

    public RubricGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            RubricVersionRepository rubricVersionRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricCriterionBandRepository rubricCriterionBandRepository,
            RubricResultBandRepository rubricResultBandRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            FrameworkRepository frameworkRepository
    ) {
        // 1. School View Rubric Version
        registry.<RubricVersionsKey, PageResult<RubricVersionDto>>forName("rubricVersionsDataLoader")
                .registerMappedBatchLoader((Set<RubricVersionsKey> keys, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            Map<RubricVersionsKey, PageResult<RubricVersionDto>> result = new HashMap<>();
                            keys.forEach(k -> result.put(k, new PageResult<>(List.of(), k.page(), k.size(), 0, 0)));
                            var keysByStatus = keys.stream()
                                    .collect(Collectors.groupingBy(k -> k.status() == null ? "NULL" : k.status()));

                            for (var entry : keysByStatus.entrySet()) {
                                String status = "NULL".equals(entry.getKey()) ? null : entry.getKey();
                                var groupedKeys = entry.getValue();
                                var rubricIds = groupedKeys.stream().map(RubricVersionsKey::rubricId).distinct().toList();

                                List<RubricVersion> allVersions = rubricVersionRepository.findByRubricIdInAndStatus(rubricIds, status);
                                Map<UUID, List<RubricVersion>> versionsByRubric = allVersions.stream()
                                        .collect(Collectors.groupingBy(RubricVersion::getRubricId));

                                for (RubricVersionsKey key : groupedKeys) {
                                    List<RubricVersion> rubricVersions = versionsByRubric.getOrDefault(key.rubricId(), List.of());
                                    int totalElements = rubricVersions.size();
                                    int totalPages = (int) Math.ceil((double) totalElements / key.size());

                                    List<RubricVersionDto> pagedDtos = rubricVersions.stream()
                                            .sorted((v1, v2) -> Integer.compare(v2.getVersion(), v1.getVersion()))
                                            .skip((long) key.page() * key.size())
                                            .limit(key.size())
                                            .map(RubricVersionDtoMapper::toRubricVersionDto)
                                            .toList();

                                    result.put(key, new PageResult<>(pagedDtos, key.page(), key.size(), totalElements, totalPages));
                                }
                            }
                            return result;
                        })
                );

        // 2. Lấy criteria của rubric version
        registry.<RubricCriteriaKey, PageResult<RubricCriterionDto>>forName("rubricCriteriaDataLoader")
                .registerMappedBatchLoader((Set<RubricCriteriaKey> keys, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            Map<RubricCriteriaKey, PageResult<RubricCriterionDto>> result = new HashMap<>();
                            keys.forEach(k -> result.put(k, new PageResult<>(List.of(), k.page(), k.size(), 0, 0)));

                            var versionIds = keys.stream().map(RubricCriteriaKey::versionId).distinct().toList();
                            List<RubricCriterion> allCriteria = rubricCriterionRepository.findByRubricVersionIdIn(versionIds);
                            Map<UUID, List<RubricCriterion>> criteriaByVersion = allCriteria.stream()
                                    .collect(Collectors.groupingBy(RubricCriterion::getRubricVersionId));

                            for (RubricCriteriaKey key : keys) {
                                List<RubricCriterion> criteriaList = criteriaByVersion.getOrDefault(key.versionId(), List.of());
                                int totalElements = criteriaList.size();
                                int totalPages = (int) Math.ceil((double) totalElements / key.size());

                                List<RubricCriterionDto> pagedDtos = criteriaList.stream()
                                        .sorted(Comparator.comparingInt(RubricCriterion::getOrder))
                                        .skip((long) key.page() * key.size())
                                        .limit(key.size())
                                        .map(RubricCriterionDtoMapper::toDto)
                                        .toList();

                                result.put(key, new PageResult<>(pagedDtos, key.page(), key.size(), totalElements, totalPages));
                            }
                            return result;
                        })
                );

        // 3. LOADER: LẤY DANH SÁCH THANG ĐIỂM (BANDS) CHO TIÊU CHÍ
        registry.<RubricCriterionBandsKey, PageResult<RubricCriterionBandDto>>forName("rubricCriterionBandsDataLoader")
                .registerMappedBatchLoader((Set<RubricCriterionBandsKey> keys, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            Map<RubricCriterionBandsKey, PageResult<RubricCriterionBandDto>> result = new HashMap<>();
                            keys.forEach(k -> result.put(k, new PageResult<>(List.of(), k.page(), k.size(), 0, 0)));

                            var criterionIds = keys.stream().map(RubricCriterionBandsKey::criterionId).distinct().toList();
                            List<RubricCriterionBand> allBands = rubricCriterionBandRepository.findByCriterionIdIn(criterionIds);
                            Map<UUID, List<RubricCriterionBand>> bandsByCriterion = allBands.stream()
                                    .collect(Collectors.groupingBy(RubricCriterionBand::getCriterionId));

                            for (RubricCriterionBandsKey key : keys) {
                                List<RubricCriterionBand> bandList = bandsByCriterion.getOrDefault(key.criterionId(), List.of());
                                int totalElements = bandList.size();
                                int totalPages = (int) Math.ceil((double) totalElements / key.size());

                                List<RubricCriterionBandDto> pagedDtos = bandList.stream()
                                        .sorted((b1, b2) -> b1.getScoreMin().compareTo(b2.getScoreMin()))
                                        .skip((long) key.page() * key.size())
                                        .limit(key.size())
                                        .map(RubricCriterionBandDtoMapper::toDto)
                                        .toList();

                                result.put(key, new PageResult<>(pagedDtos, key.page(), key.size(), totalElements, totalPages));
                            }
                            return result;
                        })
                );

        // 4. LOADER: LẤY DANH SÁCH KẾT QUẢ THANG ĐIỂM (RESULT BANDS)
        registry.<RubricResultBandsKey, PageResult<RubricResultBandDto>>forName("rubricResultBandsDataLoader")
                .registerMappedBatchLoader((Set<RubricResultBandsKey> keys, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            Map<RubricResultBandsKey, PageResult<RubricResultBandDto>> result = new HashMap<>();
                            keys.forEach(k -> result.put(k, new PageResult<>(List.of(), k.page(), k.size(), 0, 0)));

                            var versionIds = keys.stream().map(RubricResultBandsKey::versionId).distinct().toList();
                            List<RubricResultBand> allResultBands = rubricResultBandRepository.findByRubricVersionIdIn(versionIds);
                            Map<UUID, List<RubricResultBand>> resultBandsByVersion = allResultBands.stream()
                                    .collect(Collectors.groupingBy(RubricResultBand::getRubricVersionId));

                            for (RubricResultBandsKey key : keys) {
                                List<RubricResultBand> bandList = resultBandsByVersion.getOrDefault(key.versionId(), List.of());
                                int totalElements = bandList.size();
                                int totalPages = (int) Math.ceil((double) totalElements / key.size());

                                List<RubricResultBandDto> pagedDtos = bandList.stream()
                                        .sorted((b1, b2) -> Integer.compare(b1.getOrder(), b2.getOrder()))
                                        .skip((long) key.page() * key.size())
                                        .limit(key.size())
                                        .map(RubricResultBandDtoMapper::toDto)
                                        .toList();

                                result.put(key, new PageResult<>(pagedDtos, key.page(), key.size(), totalElements, totalPages));
                            }
                            return result;
                        })
                );

        //  5. LOADER: LẤY CHI TIẾT NGÔN NGỮ (SUPPORTED LANGUAGE) CHO RUBRIC
        registry.<UUID, SupportedLanguageDto>forName("supportedLanguageDataLoader")
                .registerMappedBatchLoader((Set<UUID> languageIds, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            if (languageIds.isEmpty()) return Collections.emptyMap();

                            List<SupportedLanguage> languages = supportedLanguageRepository.findByIdIn(languageIds);

                            return languages.stream()
                                    .collect(Collectors.toMap(
                                            SupportedLanguage::getId,
                                            SupportedLanguageDtoMapper::toDto
                                    ));
                        })
                );

        // 6. LOADER: LẤY CHI TIẾT KHUNG NĂNG LỰC (FRAMEWORK) CHO RUBRIC
        registry.<UUID, FrameworkDto>forName("frameworkDataLoader")
                .registerMappedBatchLoader((Set<UUID> frameworkIds, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            if (frameworkIds.isEmpty()) return Collections.emptyMap();

                            List<Framework> frameworks = frameworkRepository.findByIdIn(frameworkIds);

                            return frameworks.stream()
                                    .collect(Collectors.toMap(
                                            Framework::getId,
                                            FrameworkDtoMapper::toDto
                                    ));
                        })
                );
    }
}
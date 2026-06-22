package com.sep.vox.interfaces.graphql.config;

import com.sep.vox.application.port.input.query.key.RubricCriteriaKey;
import com.sep.vox.application.port.input.query.key.RubricCriterionBandsKey;
import com.sep.vox.application.port.input.query.key.RubricResultBandsKey;
import com.sep.vox.application.port.input.query.key.RubricVersionsKey;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionBandDto;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.dto.RubricResultBandDto;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.mapper.RubricCriterionBandDtoMapper;
import com.sep.vox.domain.mapper.RubricCriterionDtoMapper;
import com.sep.vox.domain.mapper.RubricResultBandDtoMapper;
import com.sep.vox.domain.mapper.RubricVersionDtoMapper;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricCriterionBand;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricCriterionBandRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
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
            RubricResultBandRepository rubricResultBandRepository
    ) {
        // School View Rubric Version
        registry.<RubricVersionsKey, PageResult<RubricVersionDto>>forName("rubricVersionsDataLoader")
                .registerMappedBatchLoader((Set<RubricVersionsKey> keys, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            Map<RubricVersionsKey, PageResult<RubricVersionDto>> result = new HashMap<>();

                            // 1. Gán kết quả rỗng mặc định để tránh lỗi Null
                            keys.forEach(k -> result.put(k, new PageResult<>(List.of(), k.page(), k.size(), 0, 0)));

                            // 2. Gom nhóm theo status
                            var keysByStatus = keys.stream()
                                    .collect(Collectors.groupingBy(k -> k.status() == null ? "NULL" : k.status()));

                            for (var entry : keysByStatus.entrySet()) {
                                String status = "NULL".equals(entry.getKey()) ? null : entry.getKey();
                                var groupedKeys = entry.getValue();

                                // Lấy tất cả Rubric ID
                                var rubricIds = groupedKeys.stream().map(RubricVersionsKey::rubricId).distinct().toList();

                                //  GỌI DATABASE ĐÚNG 1 LẦN DUY NHẤT (O(1))
                                List<RubricVersion> allVersions = rubricVersionRepository.findByRubricIdInAndStatus(rubricIds, status);

                                // Nhóm kết quả trả về theo từng rubricId
                                Map<UUID, List<RubricVersion>> versionsByRubric = allVersions.stream()
                                        .collect(Collectors.groupingBy(RubricVersion::getRubricId));

                                // 3. Phân trang bằng Java (In-memory Pagination)
                                for (RubricVersionsKey key : groupedKeys) {
                                    List<RubricVersion> rubricVersions = versionsByRubric.getOrDefault(key.rubricId(), List.of());

                                    int totalElements = rubricVersions.size();
                                    int totalPages = (int) Math.ceil((double) totalElements / key.size());

                                    // Cắt danh sách theo page và size
                                    List<RubricVersionDto> pagedDtos = rubricVersions.stream()
                                            // Sắp xếp bản mới nhất lên đầu cho chuẩn logic hiển thị Version
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
        // lấy criteria của rubric version
        registry.<RubricCriteriaKey, PageResult<RubricCriterionDto>>forName("rubricCriteriaDataLoader")
                .registerMappedBatchLoader((Set<RubricCriteriaKey> keys, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            Map<RubricCriteriaKey, PageResult<RubricCriterionDto>> result = new HashMap<>();

                            // 1. Gán kết quả rỗng mặc định
                            keys.forEach(k -> result.put(k, new PageResult<>(List.of(), k.page(), k.size(), 0, 0)));

                            // Lấy tất cả Version ID cần quét
                            var versionIds = keys.stream().map(RubricCriteriaKey::versionId).distinct().toList();

                            //  GỌI DATABASE ĐÚNG 1 LẦN DUY NHẤT (O(1))
                            List<RubricCriterion> allCriteria = rubricCriterionRepository.findByRubricVersionIdIn(versionIds);

                            // 2. Nhóm kết quả theo từng versionId
                            Map<UUID, List<RubricCriterion>> criteriaByVersion = allCriteria.stream()
                                    .collect(Collectors.groupingBy(RubricCriterion::getRubricVersionId));

                            // 3. Phân trang trên RAM cho từng Key
                            for (RubricCriteriaKey key : keys) {
                                List<RubricCriterion> criteriaList = criteriaByVersion.getOrDefault(key.versionId(), List.of());

                                int totalElements = criteriaList.size();
                                int totalPages = (int) Math.ceil((double) totalElements / key.size());

                                List<RubricCriterionDto> pagedDtos = criteriaList.stream()
                                        // Sắp xếp tăng dần theo thuộc tính `order`
                                        .sorted(Comparator.comparingInt(RubricCriterion::getOrder))
                                        .skip((long) key.page() * key.size())
                                        .limit(key.size())
                                        .map(RubricCriterionDtoMapper::toDto) // Sửa theo tên class Mapper của bác
                                        .toList();

                                result.put(key, new PageResult<>(pagedDtos, key.page(), key.size(), totalElements, totalPages));
                            }

                            return result;
                        })
                );
        // LOADER: LẤY DANH SÁCH THANG ĐIỂM (BANDS) CHO TIÊU CHÍ
        registry.<RubricCriterionBandsKey, PageResult<RubricCriterionBandDto>>forName("rubricCriterionBandsDataLoader")
                .registerMappedBatchLoader((Set<RubricCriterionBandsKey> keys, BatchLoaderEnvironment env) ->
                        Mono.fromSupplier(() -> {
                            Map<RubricCriterionBandsKey, PageResult<RubricCriterionBandDto>> result = new HashMap<>();

                            // 1. Gán kết quả rỗng mặc định
                            keys.forEach(k -> result.put(k, new PageResult<>(List.of(), k.page(), k.size(), 0, 0)));

                            // Lấy tất cả Criterion ID cần quét
                            var criterionIds = keys.stream().map(RubricCriterionBandsKey::criterionId).distinct().toList();

                            //  GỌI DATABASE ĐÚNG 1 LẦN DUY NHẤT (O(1))
                            List<RubricCriterionBand> allBands = rubricCriterionBandRepository.findByCriterionIdIn(criterionIds);

                            // 2. Nhóm kết quả theo từng criterionId
                            Map<UUID, List<RubricCriterionBand>> bandsByCriterion = allBands.stream()
                                    .collect(Collectors.groupingBy(RubricCriterionBand::getCriterionId));

                            // 3. Phân trang trên RAM
                            for (RubricCriterionBandsKey key : keys) {
                                List<RubricCriterionBand> bandList = bandsByCriterion.getOrDefault(key.criterionId(), List.of());

                                int totalElements = bandList.size();
                                int totalPages = (int) Math.ceil((double) totalElements / key.size());

                                List<RubricCriterionBandDto> pagedDtos = bandList.stream()
                                        // Sắp xếp tăng dần theo điểm sàn (scoreMin) để hiển thị từ thấp đến cao
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
                                        // Sắp xếp tăng dần theo thuộc tính `order`
                                        .sorted((b1, b2) -> Integer.compare(b1.getOrder(), b2.getOrder()))
                                        .skip((long) key.page() * key.size())
                                        .limit(key.size())
                                        .map(RubricResultBandDtoMapper::toDto) // Tên class Mapper DTO của bác
                                        .toList();

                                result.put(key, new PageResult<>(pagedDtos, key.page(), key.size(), totalElements, totalPages));
                            }

                            return result;
                        })
                );
    }
}
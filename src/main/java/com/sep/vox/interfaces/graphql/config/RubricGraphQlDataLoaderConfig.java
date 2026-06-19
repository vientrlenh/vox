package com.sep.vox.interfaces.graphql.config;

import com.sep.vox.application.port.input.query.key.RubricVersionsKey;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.mapper.RubricVersionDtoMapper;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricVersionRepository;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Configuration
public class RubricGraphQlDataLoaderConfig {

    public RubricGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            RubricVersionRepository rubricVersionRepository
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
    }
}
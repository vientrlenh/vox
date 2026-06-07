package com.sep.vox.interfaces.graphql.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.application.mapper.schoolclass.SchoolClassResponseMapper;
import com.sep.vox.application.port.input.query.key.SchoolClassesKey;
import com.sep.vox.application.response.input.schoolclass.SchoolClassResponse;
import com.sep.vox.domain.repository.SchoolClassRepository;

import reactor.core.publisher.Mono;

@Configuration
public class SchoolGraphQlDataLoaderConfig {

    public SchoolGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            SchoolClassRepository schoolClassRepository) {

        registry.<SchoolClassesKey, List<SchoolClassResponse>>forName("schoolClassesBySchool")
        .registerMappedBatchLoader((Set<SchoolClassesKey> keys, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<SchoolClassesKey, List<SchoolClassResponse>> result = new HashMap<SchoolClassesKey, List<SchoolClassResponse>>();

                keys.forEach(key -> result.put(key, List.of()));

                var keysByPage = keys.stream()
                    .collect(Collectors.groupingBy(key -> new PageKey(key.page(), key.size())));

                for (var entry : keysByPage.entrySet()) {
                    var pageKey = entry.getKey();
                    var groupedKeys = entry.getValue();

                    var schoolIds = groupedKeys.stream()
                        .map(SchoolClassesKey::schoolId)
                        .toList();

                    var classesBySchoolId = schoolClassRepository
                        .findBySchoolIdIn(schoolIds, pageKey.page(), pageKey.size())
                        .stream()
                        .map(SchoolClassResponseMapper::toResponse)
                        .collect(Collectors.groupingBy(SchoolClassResponse::schoolId));

                    for (var key : groupedKeys) {
                        result.put(
                            key,
                            classesBySchoolId.getOrDefault(key.schoolId(), List.of())
                        );
                    }
                }

                return result;
            })
        );
    }

    private record PageKey(int page, int size) {
    }
}

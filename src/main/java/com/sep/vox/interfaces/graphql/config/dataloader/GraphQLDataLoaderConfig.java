package com.sep.vox.interfaces.graphql.config.dataloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.interfaces.graphql.dto.key.SchoolClassPageKey;
import com.sep.vox.interfaces.graphql.dto.key.SchoolUserPageKey;

import reactor.core.publisher.Mono;

@Configuration 
public class GraphQLDataLoaderConfig {
    
    public GraphQLDataLoaderConfig(
        BatchLoaderRegistry registry, 
        UserRepository userRepository, 
        SchoolRepository schoolRepository, 
        SchoolClassRepository schoolClassRepository, 
        SchoolUserRepository schoolUserRepository
    ) {
        registry.<UUID, UserDto>forName("userByUserId")
            .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> userRepository.findByIdIn(userIds)
                    .stream()
                    .map(UserDto::toDto)
                    .collect(Collectors.toMap(u -> u.id(), Function.identity()))
            )
        );

        registry.<UUID, SchoolDto>forName("schoolBySchoolId")
            .registerMappedBatchLoader((Set<UUID> schoolIds, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> schoolRepository.findByIdIn(schoolIds)
                    .stream()
                    .map(SchoolDto::toDto)
                    .collect(Collectors.toMap(s -> s.id(), Function.identity()))
            )
        );

        registry.<SchoolClassPageKey, List<SchoolClassDto>>forName("schoolClassPageBySchoolId")
            .registerMappedBatchLoader((Set<SchoolClassPageKey> keys, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> {
                    Map<SchoolClassPageKey, List<SchoolClassDto>> result = new HashMap<>();
                    keys.forEach(key -> result.put(key, List.of()));

                    Map<DataLoaderPageKey, List<SchoolClassPageKey>> pageKeys = keys.stream()
                        .collect(Collectors.groupingBy(key -> new DataLoaderPageKey(
                            key.pageRequest().page(), 
                            key.pageRequest().size()
                        ))
                    );

                    for (Entry<DataLoaderPageKey, List<SchoolClassPageKey>> entry : pageKeys.entrySet()) {
                        DataLoaderPageKey pageKey = entry.getKey();
                        List<SchoolClassPageKey> groupedKeys = entry.getValue();

                        List<UUID> schoolIds = groupedKeys.stream()
                            .map(gk -> gk.schoolId())
                            .toList();

                        Map<UUID, List<SchoolClassDto>> schoolClasses = schoolClassRepository
                            .findBySchoolIdIn(schoolIds, pageKey.page(), pageKey.size())
                            .stream()
                            .map(SchoolClassDto::toDto)
                            .collect(Collectors.groupingBy(c -> c.schoolId()));

                        groupedKeys.forEach(key -> result.put(
                            key, 
                            schoolClasses.getOrDefault(key.schoolId(), List.of())
                        ));
                    }

                    return result;
                })
        );

        registry.<SchoolUserPageKey, List<SchoolUserDto>>forName("schoolUserPageBySchoolId")
            .registerMappedBatchLoader((Set<SchoolUserPageKey> keys, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> {
                    Map<SchoolUserPageKey, List<SchoolUserDto>> result = new HashMap<>();
                    keys.forEach(key -> result.put(key, List.of()));

                    Map<DataLoaderPageKey, List<SchoolUserPageKey>> pageKeys = keys.stream()
                        .collect(Collectors.groupingBy(key -> new DataLoaderPageKey(key.pageRequest().page(), key.pageRequest().size())));
                    
                    for (Entry<DataLoaderPageKey, List<SchoolUserPageKey>> entry : pageKeys.entrySet()) {
                        DataLoaderPageKey pageKey = entry.getKey();
                        List<SchoolUserPageKey> groupedKeys = entry.getValue();

                        List<UUID> schoolIds = groupedKeys.stream()
                            .map(gk -> gk.schoolId())
                            .toList();

                        Map<UUID, List<SchoolUserDto>> schoolUsers = schoolUserRepository.findBySchoolIdIn(schoolIds, pageKey.page(), pageKey.size())
                            .stream()
                            .map(SchoolUserDto::toDto)
                            .collect(Collectors.groupingBy(u -> u.schoolId()));

                        groupedKeys.forEach(key -> result.put(
                            key, 
                            schoolUsers.getOrDefault(key.schoolId(), List.of()
                        )));
                    }
                    return result;
                })
        );

        registry.<UUID, SchoolGradeDto>forName("schoolGradeBySchoolClassId")
            .registerMappedBatchLoader((Set<UUID> schoolGradeIds, BatchLoaderEnvironment env) -> 
                Mono.fromSupplier(() -> {
                    return 
                })
        );
    }
}

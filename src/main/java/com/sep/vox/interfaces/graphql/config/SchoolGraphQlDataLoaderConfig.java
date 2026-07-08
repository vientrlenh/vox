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

import com.sep.vox.application.port.input.query.key.SchoolClassesKey;
import com.sep.vox.application.port.input.query.key.SchoolUsersKey;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.mapper.SchoolGradeDtoMapper;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.mapper.SupportedLanguageDtoMapper;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

import reactor.core.publisher.Mono;

@Configuration
public class SchoolGraphQlDataLoaderConfig {

    public SchoolGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            SchoolClassRepository schoolClassRepository,
            SchoolRepository schoolRepository,
            SchoolGradeRepository schoolGradeRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            UserRepository userRepository, 
            SchoolUserRepository schoolUserRepository) {

        registry.<SchoolClassesKey, List<SchoolClassDto>>forName("schoolClassesBySchool")
        .registerMappedBatchLoader((Set<SchoolClassesKey> keys, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<SchoolClassesKey, List<SchoolClassDto>> result = new HashMap<SchoolClassesKey, List<SchoolClassDto>>();

                keys.forEach(key -> result.put(key, List.of()));

                var keysByPage = keys.stream()
                    .collect(Collectors.groupingBy(key -> new PageKey(key.page(), key.size()))
                );

                for (var entry : keysByPage.entrySet()) {
                    var pageKey = entry.getKey();
                    var groupedKeys = entry.getValue();

                    var schoolIds = groupedKeys.stream()
                        .map(k -> k.schoolId())
                        .toList();

                    var schoolClasses = schoolClassRepository
                        .findBySchoolIdIn(schoolIds, pageKey.page(), pageKey.size())
                        .stream()
                        .map(SchoolClassDtoMapper::toDto)
                        .collect(Collectors.groupingBy(c -> c.schoolId()));

                    groupedKeys.forEach(key -> result.put(
                        key,
                        schoolClasses.getOrDefault(key.schoolId(), List.of())
                    ));
                }

                return result;
            })
        );

        registry.<SchoolUsersKey, List<SchoolUserDto>>forName("schoolUsersBySchool")
        .registerMappedBatchLoader((Set<SchoolUsersKey> keys, BatchLoaderEnvironment env) -> 
            Mono.fromSupplier(() -> {
                Map<SchoolUsersKey, List<SchoolUserDto>> result = new HashMap<>();

                keys.forEach(key -> result.put(key, List.of()));

                var keysByPage = keys.stream().collect(Collectors.groupingBy(key -> new PageKey(key.page(), key.size())));

                for (var entry : keysByPage.entrySet()) {
                    var pageKey = entry.getKey();
                    var groupedKeys = entry.getValue();

                    var schoolIds = groupedKeys.stream()
                        .map(k -> k.schoolId())
                        .toList();

                    var usersBySchoolId = schoolUserRepository.findBySchoolIdIn(schoolIds, pageKey.page(), pageKey.size())
                        .stream()
                        .map(SchoolUserDtoMapper::toSchoolUserDto)
                        .collect(Collectors.groupingBy(u -> u.schoolId()));
                    
                    groupedKeys.forEach(key -> result.put(key, usersBySchoolId.getOrDefault(key.schoolId(), List.of())));
                }
                return result;
            })
        );

        registry.<UUID, SchoolDto>forName("schoolByClass")
        .registerMappedBatchLoader((Set<UUID> schoolIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                return schoolRepository.findByIdIn(schoolIds)
                    .stream()
                    .map(SchoolDtoMapper::toSchoolDto)
                    .collect(Collectors.toMap(s -> s.id(), s -> s));
            })
        );

        registry.<UUID, SchoolGradeDto>forName("schoolGradeByClass")
        .registerMappedBatchLoader((Set<UUID> gradeIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                return schoolGradeRepository.findByIdIn(gradeIds)
                    .stream()
                    .map(SchoolGradeDtoMapper::toSchoolGradeDto)
                    .collect(Collectors.toMap(sg -> sg.id(), sg -> sg));
            })
        );

        registry.<UUID, SchoolGradeDto>forName("schoolGradeById")
        .registerMappedBatchLoader((Set<UUID> gradeIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                return schoolGradeRepository.findByIdIn(gradeIds)
                    .stream()
                    .map(SchoolGradeDtoMapper::toSchoolGradeDto)
                    .collect(Collectors.toMap(grade -> grade.id(), sg -> sg));
            })
        );

        registry.<UUID, SupportedLanguageDto>forName("supportedLanguageByClass")
        .registerMappedBatchLoader((Set<UUID> languageIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                return supportedLanguageRepository.findByIdIn(languageIds)
                    .stream()
                    .map(SupportedLanguageDtoMapper::toDto)
                    .collect(Collectors.toMap(sl -> sl.id(), sl -> sl));
            })
        );

        registry.<UUID, UserDto>forName("userBySchoolClassUser")
        .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> userRepository.findByIdIn(userIds)
                .stream()
                .map(UserDtoMapper::toUserDto)
                .collect(Collectors.toMap(u -> u.id(), user -> user)))
        );

        registry.<UUID, UserDto>forName("userBySchoolUser")
        .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> userRepository.findByIdIn(userIds)
                .stream()
                .map(UserDtoMapper::toUserDto)
                .collect(Collectors.toMap(u -> u.id(),  user -> user)))
        );
    }

}

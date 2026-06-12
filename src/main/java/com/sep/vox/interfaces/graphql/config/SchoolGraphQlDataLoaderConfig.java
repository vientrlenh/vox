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
import com.sep.vox.application.port.input.query.key.SchoolClassGradeKey;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SupportedLanguageDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.mapper.SchoolGradeDtoMapper;
import com.sep.vox.domain.mapper.SupportedLanguageDtoMapper;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
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
            UserRepository userRepository) {

        registry.<SchoolClassesKey, List<SchoolClassDto>>forName("schoolClassesBySchool")
        .registerMappedBatchLoader((Set<SchoolClassesKey> keys, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<SchoolClassesKey, List<SchoolClassDto>> result = new HashMap<SchoolClassesKey, List<SchoolClassDto>>();

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
                        .map(SchoolClassDtoMapper::toDto)
                        .collect(Collectors.groupingBy(SchoolClassDto::schoolId));

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

        registry.<UUID, SchoolDto>forName("schoolById")
        .registerMappedBatchLoader((Set<UUID> schoolIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UUID, SchoolDto> result = new HashMap<>();
                schoolIds.forEach(schoolId -> schoolRepository.findById(schoolId)
                    .map(SchoolDtoMapper::toSchoolDto)
                    .ifPresent(school -> result.put(school.id(), school)));
                return result;
            })
        );

        registry.<SchoolClassGradeKey, SchoolGradeDto>forName("schoolGradeByClass")
        .registerMappedBatchLoader((Set<SchoolClassGradeKey> keys, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<SchoolClassGradeKey, SchoolGradeDto> result = new HashMap<>();
                keys.forEach(key -> schoolGradeRepository.findById(key.schoolGradeId())
                    .filter(grade -> schoolGradeRepository.findBySchoolIdAndCode(key.schoolId(), grade.getCode())
                        .map(schoolGrade -> schoolGrade.getId().equals(grade.getId()))
                        .orElse(false))
                    .map(grade -> SchoolGradeDtoMapper.toDto(grade, key.schoolId()))
                    .ifPresent(grade -> result.put(key, grade)));
                return result;
            })
        );

        registry.<UUID, SupportedLanguageDto>forName("supportedLanguageById")
        .registerMappedBatchLoader((Set<UUID> languageIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> {
                Map<UUID, SupportedLanguageDto> result = new HashMap<>();
                languageIds.forEach(languageId -> supportedLanguageRepository.findById(languageId)
                    .map(SupportedLanguageDtoMapper::toDto)
                    .ifPresent(language -> result.put(language.id(), language)));
                return result;
            })
        );

        registry.<UUID, UserDto>forName("userById")
        .registerMappedBatchLoader((Set<UUID> userIds, BatchLoaderEnvironment env) ->
            Mono.fromSupplier(() -> userRepository.findByIdIn(userIds)
                .stream()
                .map(UserDtoMapper::toUserDto)
                .collect(Collectors.toMap(UserDto::id, user -> user)))
        );
    }

    private record PageKey(int page, int size) {
    }
}

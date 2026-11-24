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

import com.sep.vox.domain.dto.GradeLevelDto;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.dto.SchoolDto;
import com.sep.vox.domain.dto.SchoolGradeDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.mapper.SchoolDtoMapper;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.interfaces.graphql.key.SchoolClassesKey;
import com.sep.vox.interfaces.graphql.key.SchoolUsersKey;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class SchoolGraphQLDataLoaderConfig {

    public static final String SCHOOL_BY_SCHOOL_ID = "schoolBySchoolId";
    public static final String SCHOOL_CLASS_BY_SCHOOL_CLASS_ID = "schoolClassBySchoolClassId";
    public static final String SCHOOL_CLASSES_BY_SCHOOL_ID = "schoolClassesBySchoolId";
    public static final String SCHOOL_USERS_BY_SCHOOL_ID = "schoolUsersBySchoolId";
    public static final String SCHOOL_GRADE_BY_SCHOOL_GRADE_ID = "schoolGradeBySchoolGradeId";
    public static final String GRADE_LEVEL_BY_GRADE_LEVEL_ID = "gradeLevelByGradeLevelId";
    public static final String SCHOOL_ROOM_BY_SCHOOL_ROOM_ID = "schoolRoomBySchoolRoomId";
    public static final String ACTIVE_MEMBER_COUNT_BY_SCHOOL_CLASS_ID = "activeMemberCountBySchoolClassId";

    private final BatchLoaderRegistry registry;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;

    @PostConstruct
    void registerLoaders() {
        registerSchoolBySchoolId();
        registerSchoolClassBySchoolClassId();
        registerSchoolClassesBySchoolId();
        registerSchoolUsersBySchoolId();
        registerSchoolGradeBySchoolGradeId();
        registerGradeLevelByGradeLevelId();
        registerSchoolRoomBySchoolRoomId();
        registerActiveMemberCountBySchoolClassId();
    }

    private void registerSchoolBySchoolId() {
        registry.<UUID, SchoolDto>forName(SCHOOL_BY_SCHOOL_ID)
            .registerMappedBatchLoader((Set<UUID> schoolIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> schoolRepository.findByIdIn(schoolIds)
                    .stream()
                    .map(SchoolDtoMapper::toSchoolDto)
                    .collect(Collectors.toMap(school -> school.id(), school -> school)))
            );
    }

    private void registerSchoolClassBySchoolClassId() {
        registry.<UUID, SchoolClassDto>forName(SCHOOL_CLASS_BY_SCHOOL_CLASS_ID)
            .registerMappedBatchLoader((Set<UUID> schoolClassIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> schoolClassRepository.findAllById(schoolClassIds.stream().toList())
                    .stream()
                    .map(SchoolClassDtoMapper::toDto)
                    .collect(Collectors.toMap(schoolClass -> schoolClass.id(), schoolClass -> schoolClass)))
            );
    }

    private void registerSchoolClassesBySchoolId() {
        registry.<SchoolClassesKey, List<SchoolClassDto>>forName(SCHOOL_CLASSES_BY_SCHOOL_ID)
            .registerMappedBatchLoader((Set<SchoolClassesKey> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> loadSchoolClassesByKeys(keys))
            );
    }

    private Map<SchoolClassesKey, List<SchoolClassDto>> loadSchoolClassesByKeys(Set<SchoolClassesKey> keys) {
        Map<SchoolClassesKey, List<SchoolClassDto>> result = new HashMap<>();
        keys.forEach(key -> result.put(key, List.of()));

        Map<PageKey, List<SchoolClassesKey>> keysByPage = keys.stream()
            .collect(Collectors.groupingBy(key -> new PageKey(key.page(), key.size())));

        keysByPage.forEach((pageKey, groupedKeys) -> {
            Map<UUID, List<SchoolClassDto>> classesBySchoolId = loadSchoolClassesBySchoolId(groupedKeys, pageKey);
            groupedKeys.forEach(key -> result.put(key, classesBySchoolId.getOrDefault(key.schoolId(), List.of())));
        });
        return result;
    }

    private Map<UUID, List<SchoolClassDto>> loadSchoolClassesBySchoolId(
            List<SchoolClassesKey> keys, PageKey pageKey) {
        List<UUID> schoolIds = keys.stream().map(key -> key.schoolId()).toList();
        return schoolClassRepository.findBySchoolIdIn(schoolIds, pageKey.page(), pageKey.size())
            .stream()
            .map(SchoolClassDtoMapper::toDto)
            .collect(Collectors.groupingBy(schoolClass -> schoolClass.schoolId()));
    }

    private void registerSchoolUsersBySchoolId() {
        registry.<SchoolUsersKey, List<SchoolUserDto>>forName(SCHOOL_USERS_BY_SCHOOL_ID)
            .registerMappedBatchLoader((Set<SchoolUsersKey> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> loadSchoolUsersByKeys(keys))
            );
    }

    private Map<SchoolUsersKey, List<SchoolUserDto>> loadSchoolUsersByKeys(Set<SchoolUsersKey> keys) {
        Map<SchoolUsersKey, List<SchoolUserDto>> result = new HashMap<>();
        keys.forEach(key -> result.put(key, List.of()));

        Map<PageKey, List<SchoolUsersKey>> keysByPage = keys.stream()
            .collect(Collectors.groupingBy(key -> new PageKey(key.page(), key.size())));

        keysByPage.forEach((pageKey, groupedKeys) -> {
            Map<UUID, List<SchoolUserDto>> usersBySchoolId = loadSchoolUsersBySchoolId(groupedKeys, pageKey);
            groupedKeys.forEach(key -> result.put(key, usersBySchoolId.getOrDefault(key.schoolId(), List.of())));
        });
        return result;
    }

    private Map<UUID, List<SchoolUserDto>> loadSchoolUsersBySchoolId(List<SchoolUsersKey> keys, PageKey pageKey) {
        List<UUID> schoolIds = keys.stream().map(key -> key.schoolId()).toList();
        return schoolUserRepository.findBySchoolIdIn(schoolIds, pageKey.page(), pageKey.size())
            .stream()
            .map(SchoolUserDtoMapper::toSchoolUserDto)
            .collect(Collectors.groupingBy(schoolUser -> schoolUser.schoolId()));
    }

    private void registerSchoolGradeBySchoolGradeId() {
        registry.<UUID, SchoolGradeDto>forName(SCHOOL_GRADE_BY_SCHOOL_GRADE_ID)
            .registerMappedBatchLoader((Set<UUID> schoolGradeIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> schoolGradeRepository.findByIdIn(schoolGradeIds)
                    .stream()
                    .map(SchoolGradeDto::toDto)
                    .collect(Collectors.toMap(schoolGrade -> schoolGrade.id(), schoolGrade -> schoolGrade)))
            );
    }

    private void registerGradeLevelByGradeLevelId() {
        registry.<UUID, GradeLevelDto>forName(GRADE_LEVEL_BY_GRADE_LEVEL_ID)
            .registerMappedBatchLoader((Set<UUID> gradeLevelIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> gradeLevelRepository.findByIdIn(gradeLevelIds)
                    .stream()
                    .map(GradeLevelDto::toDto)
                    .collect(Collectors.toMap(gradeLevel -> gradeLevel.id(), gradeLevel -> gradeLevel)))
            );
    }

    private void registerSchoolRoomBySchoolRoomId() {
        registry.<UUID, SchoolRoomFromDto>forName(SCHOOL_ROOM_BY_SCHOOL_ROOM_ID)
            .registerMappedBatchLoader((Set<UUID> roomIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> schoolRoomRepository.findByIdIn(roomIds)
                    .stream()
                    .map(SchoolRoomDtoMapper::toDto)
                    .collect(Collectors.toMap(room -> room.id(), room -> room)))
            );
    }

    // MyClass.activeMemberCount is Int!, so the loader must return an entry for EVERY key: a class with no
    // members does not appear in the GROUP BY result, and when the key is missing GraphQL receives null and
    // throws a non-null error.
    private void registerActiveMemberCountBySchoolClassId() {
        registry.<UUID, Integer>forName(ACTIVE_MEMBER_COUNT_BY_SCHOOL_CLASS_ID)
            .registerMappedBatchLoader((Set<UUID> classIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, Integer> counts = schoolClassUserRepository.countActiveBySchoolClassIdIn(classIds);
                    Map<UUID, Integer> result = new HashMap<>();
                    classIds.forEach(classId -> result.put(classId, counts.getOrDefault(classId, 0)));
                    return result;
                })
            );
    }
}

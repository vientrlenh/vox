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

import com.sep.vox.application.port.input.usecase.examevaluation.ResolveExamCandidateAttemptsUseCase;
import com.sep.vox.application.query.dto.ExamCandidateAttempts;
import com.sep.vox.domain.dto.ExamBlueprintDto;
import com.sep.vox.domain.dto.ExamBlueprintVersionDto;
import com.sep.vox.domain.dto.ExamDto;
import com.sep.vox.domain.dto.ExamMemberDto;
import com.sep.vox.domain.dto.ExamPaperDto;
import com.sep.vox.domain.dto.ExamScheduleDto;
import com.sep.vox.domain.dto.ExamSecurePoolDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.mapper.ExamBlueprintDtoMapper;
import com.sep.vox.domain.mapper.ExamBlueprintVersionDtoMapper;
import com.sep.vox.domain.mapper.ExamDtoMapper;
import com.sep.vox.domain.mapper.ExamMemberDtoMapper;
import com.sep.vox.domain.dto.ExamPaperItemDto;
import com.sep.vox.domain.dto.ExamPaperSectionDto;
import com.sep.vox.domain.mapper.ExamPaperDtoMapper;
import com.sep.vox.domain.mapper.ExamPaperItemDtoMapper;
import com.sep.vox.domain.mapper.ExamPaperSectionDtoMapper;
import com.sep.vox.domain.mapper.ExamScheduleDtoMapper;
import com.sep.vox.domain.mapper.ExamSecurePoolDtoMapper;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.repository.ExamBlueprintRepository;
import com.sep.vox.domain.repository.ExamBlueprintVersionRepository;
import com.sep.vox.domain.repository.ExamCandidateRepository;
import com.sep.vox.domain.repository.ExamMemberRepository;
import com.sep.vox.domain.repository.ExamPaperItemRepository;
import com.sep.vox.domain.repository.ExamPaperRepository;
import com.sep.vox.domain.repository.ExamPaperSectionRepository;
import com.sep.vox.domain.repository.ExamRepository;
import com.sep.vox.domain.repository.ExamScheduleRepository;
import com.sep.vox.domain.repository.ExamSecurePoolRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;

import reactor.core.publisher.Mono;

@Configuration
public class ExamGraphQlDataLoaderConfig {

    public ExamGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            SchoolRoomRepository schoolRoomRepository,
            ExamRepository examRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamPaperRepository examPaperRepository,
            ExamBlueprintRepository examBlueprintRepository,
            ExamBlueprintVersionRepository examBlueprintVersionRepository,
            ExamMemberRepository examMemberRepository,
            ExamPaperSectionRepository examPaperSectionRepository,
            ExamPaperItemRepository examPaperItemRepository,
            ExamSecurePoolRepository examSecurePoolRepository,
            ExamCandidateRepository examCandidateRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            ResolveExamCandidateAttemptsUseCase resolveExamCandidateAttemptsUseCase) {

        registry.<UUID, SchoolRoomFromDto>forName("schoolRoomById")
            .registerMappedBatchLoader((Set<UUID> roomIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> schoolRoomRepository.findByIdIn(roomIds)
                    .stream()
                    .map(SchoolRoomDtoMapper::toDto)
                    .collect(Collectors.toMap(SchoolRoomFromDto::id, room -> room)))
            );

        registry.<UUID, ExamDto>forName("examById")
            .registerMappedBatchLoader((Set<UUID> examIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examRepository.findByIdIn(examIds)
                    .stream()
                    .map(ExamDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamDto::id, exam -> exam)))
            );

        registry.<UUID, ExamScheduleDto>forName("examScheduleById")
            .registerMappedBatchLoader((Set<UUID> scheduleIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examScheduleRepository.findByIdIn(scheduleIds)
                    .stream()
                    .map(ExamScheduleDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamScheduleDto::id, schedule -> schedule)))
            );

        registry.<UUID, ExamPaperDto>forName("examPaperById")
            .registerMappedBatchLoader((Set<UUID> paperIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examPaperRepository.findByIdIn(paperIds)
                    .stream()
                    .map(ExamPaperDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamPaperDto::id, paper -> paper)))
            );

        registry.<UUID, ExamCandidateAttempts>forName("examCandidateAttemptsByCandidateId")
            .registerMappedBatchLoader((Set<UUID> candidateIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> resolveExamCandidateAttemptsUseCase.executeBatch(candidateIds))
            );

        registry.<UUID, ExamBlueprintDto>forName("examBlueprintById")
            .registerMappedBatchLoader((Set<UUID> blueprintIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examBlueprintRepository.findByIdIn(blueprintIds)
                    .stream()
                    .map(ExamBlueprintDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamBlueprintDto::id, dto -> dto)))
            );

        registry.<UUID, ExamBlueprintVersionDto>forName("examBlueprintVersionById")
            .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examBlueprintVersionRepository.findByIdIn(versionIds)
                    .stream()
                    .map(ExamBlueprintVersionDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamBlueprintVersionDto::id, dto -> dto)))
            );

        registry.<UUID, List<ExamMemberDto>>forName("examMembersByExamId")
            .registerMappedBatchLoader((Set<UUID> examIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<ExamMemberDto>> result = new HashMap<>();
                    examIds.forEach(id -> result.put(id, List.of()));
                    examMemberRepository.findByExamIdIn(examIds).stream()
                        .map(ExamMemberDtoMapper::toDto)
                        .collect(Collectors.groupingBy(ExamMemberDto::examId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<ExamPaperDto>>forName("examPapersByExamId")
            .registerMappedBatchLoader((Set<UUID> examIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<ExamPaperDto>> result = new HashMap<>();
                    examIds.forEach(id -> result.put(id, List.of()));
                    examPaperRepository.findByExamIdIn(examIds).stream()
                        .map(ExamPaperDtoMapper::toDto)
                        .collect(Collectors.groupingBy(ExamPaperDto::examId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<ExamPaperSectionDto>>forName("examPaperSectionsByPaperId")
            .registerMappedBatchLoader((Set<UUID> paperIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<ExamPaperSectionDto>> result = new HashMap<>();
                    paperIds.forEach(id -> result.put(id, List.of()));
                    examPaperSectionRepository.findByPaperIdIn(paperIds).stream()
                        .map(ExamPaperSectionDtoMapper::toDto)
                        .collect(Collectors.groupingBy(ExamPaperSectionDto::paperId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<ExamPaperItemDto>>forName("examPaperItemsBySectionId")
            .registerMappedBatchLoader((Set<UUID> sectionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<ExamPaperItemDto>> result = new HashMap<>();
                    sectionIds.forEach(id -> result.put(id, List.of()));
                    examPaperItemRepository.findBySectionIdIn(sectionIds).stream()
                        .map(ExamPaperItemDtoMapper::toDto)
                        .collect(Collectors.groupingBy(ExamPaperItemDto::sectionId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, ExamSecurePoolDto>forName("examSecurePoolByExamId")
            .registerMappedBatchLoader((Set<UUID> examIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> examSecurePoolRepository.findByExamIdIn(examIds)
                    .stream()
                    .map(ExamSecurePoolDtoMapper::toDto)
                    .collect(Collectors.toMap(ExamSecurePoolDto::examId, dto -> dto)))
            );

        registry.<UUID, Integer>forName("examCandidateCountByExamId")
            .registerMappedBatchLoader((Set<UUID> examIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, Integer> result = new HashMap<>();
                    examIds.forEach(id -> result.put(id, 0));
                    examCandidateRepository.countByExamIdIn(examIds)
                        .forEach((examId, count) -> result.put(examId, Math.toIntExact(count)));
                    return result;
                })
            );

        registry.<UUID, UUID>forName("examSchoolClassIdByExamId")
            .registerMappedBatchLoader((Set<UUID> examIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, UUID> firstStudentIdByExam = new HashMap<>();
                    examCandidateRepository.findByExamIdIn(examIds).forEach(candidate ->
                        firstStudentIdByExam.putIfAbsent(candidate.getExamId(), candidate.getStudentId())
                    );

                    var activeClassByStudentId = schoolClassUserRepository.findByUserIdIn(firstStudentIdByExam.values())
                        .stream()
                        .filter(SchoolClassUser::isActive)
                        .collect(Collectors.groupingBy(SchoolClassUser::getUserId));

                    Map<UUID, UUID> result = new HashMap<>();
                    firstStudentIdByExam.forEach((examId, studentId) -> {
                        var activeClasses = activeClassByStudentId.get(studentId);
                        if (activeClasses != null && !activeClasses.isEmpty()) {
                            result.put(examId, activeClasses.get(0).getSchoolClassId());
                        }
                    });
                    return result;
                })
            );
    }
}

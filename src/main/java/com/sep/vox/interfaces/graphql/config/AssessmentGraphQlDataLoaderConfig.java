package com.sep.vox.interfaces.graphql.config;

import com.sep.vox.domain.model.school.SchoolClassUser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.application.port.input.usecase.assessmentevaluation.ResolveAssessmentCandidateAttemptsUseCase;
import com.sep.vox.application.query.dto.AssessmentCandidateAttempts;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.dto.AssessmentPaperDto;
import com.sep.vox.domain.dto.AssessmentScheduleDto;
import com.sep.vox.domain.dto.AssessmentSecurePoolDto;
import com.sep.vox.domain.dto.SchoolRoomFromDto;
import com.sep.vox.domain.mapper.AssessmentBlueprintDtoMapper;
import com.sep.vox.domain.mapper.AssessmentBlueprintSlotDtoMapper;
import com.sep.vox.domain.mapper.AssessmentBlueprintVersionDtoMapper;
import com.sep.vox.domain.mapper.AssessmentDtoMapper;
import com.sep.vox.domain.mapper.AssessmentMemberDtoMapper;
import com.sep.vox.domain.dto.AssessmentPaperItemDto;
import com.sep.vox.domain.dto.AssessmentPaperSectionDto;
import com.sep.vox.domain.mapper.AssessmentPaperDtoMapper;
import com.sep.vox.domain.mapper.AssessmentPaperItemDtoMapper;
import com.sep.vox.domain.mapper.AssessmentPaperSectionDtoMapper;
import com.sep.vox.domain.mapper.AssessmentScheduleDtoMapper;
import com.sep.vox.domain.mapper.AssessmentSecurePoolDtoMapper;
import com.sep.vox.domain.mapper.SchoolRoomDtoMapper;
import com.sep.vox.domain.repository.AssessmentBlueprintRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.AssessmentCandidateRepository;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentPaperItemRepository;
import com.sep.vox.domain.repository.AssessmentPaperRepository;
import com.sep.vox.domain.repository.AssessmentPaperSectionRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.AssessmentScheduleRepository;
import com.sep.vox.domain.repository.AssessmentSecurePoolRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRoomRepository;

import reactor.core.publisher.Mono;

@Configuration
public class AssessmentGraphQlDataLoaderConfig {

    public AssessmentGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            SchoolRoomRepository schoolRoomRepository,
            AssessmentRepository assessmentRepository,
            AssessmentScheduleRepository assessmentScheduleRepository,
            AssessmentPaperRepository assessmentPaperRepository,
            AssessmentBlueprintRepository assessmentBlueprintRepository,
            AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository,
            AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository,
            AssessmentMemberRepository assessmentMemberRepository,
            AssessmentPaperSectionRepository assessmentPaperSectionRepository,
            AssessmentPaperItemRepository assessmentPaperItemRepository,
            AssessmentSecurePoolRepository assessmentSecurePoolRepository,
            AssessmentCandidateRepository assessmentCandidateRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            ResolveAssessmentCandidateAttemptsUseCase resolveAssessmentCandidateAttemptsUseCase) {

        registry.<UUID, SchoolRoomFromDto>forName("schoolRoomById")
            .registerMappedBatchLoader((Set<UUID> roomIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> schoolRoomRepository.findByIdIn(roomIds)
                    .stream()
                    .map(SchoolRoomDtoMapper::toDto)
                    .collect(Collectors.toMap(room -> room.id(), room -> room)))
            );

        registry.<UUID, AssessmentDto>forName("assessmentById")
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentRepository.findByIdIn(assessmentIds)
                    .stream()
                    .map(AssessmentDtoMapper::toDto)
                    .collect(Collectors.toMap(assessment -> assessment.id(), assessment -> assessment)))
            );

        registry.<UUID, AssessmentScheduleDto>forName("assessmentScheduleById")
            .registerMappedBatchLoader((Set<UUID> scheduleIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentScheduleRepository.findByIdIn(scheduleIds)
                    .stream()
                    .map(AssessmentScheduleDtoMapper::toDto)
                    .collect(Collectors.toMap(schedule -> schedule.id(), schedule -> schedule)))
            );

        registry.<UUID, AssessmentPaperDto>forName("assessmentPaperById")
            .registerMappedBatchLoader((Set<UUID> paperIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentPaperRepository.findByIdIn(paperIds)
                    .stream()
                    .map(AssessmentPaperDtoMapper::toDto)
                    .collect(Collectors.toMap(paper -> paper.id(), paper -> paper)))
            );

        registry.<UUID, AssessmentCandidateAttempts>forName("assessmentCandidateAttemptsByCandidateId")
            .registerMappedBatchLoader((Set<UUID> candidateIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> resolveAssessmentCandidateAttemptsUseCase.executeBatch(candidateIds))
            );

        registry.<UUID, AssessmentBlueprintDto>forName("assessmentBlueprintById")
            .registerMappedBatchLoader((Set<UUID> blueprintIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentBlueprintRepository.findByIdIn(blueprintIds)
                    .stream()
                    .map(AssessmentBlueprintDtoMapper::toDto)
                    .collect(Collectors.toMap(blueprint -> blueprint.id(), dto -> dto)))
            );

        registry.<UUID, AssessmentBlueprintVersionDto>forName("assessmentBlueprintVersionById")
            .registerMappedBatchLoader((Set<UUID> versionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentBlueprintVersionRepository.findByIdIn(versionIds)
                    .stream()
                    .map(AssessmentBlueprintVersionDtoMapper::toDto)
                    .collect(Collectors.toMap(version -> version.id(), dto -> dto)))
            );

        // The blueprint slot that generated each question in the assessment paper. The mapped loader returns null for
        // a key it cannot find, which is intended and matches the fail-open behavior in
        // UpdateAssessmentPaperItemUseCase: once the slot is deleted, the question is assigned freely.
        registry.<UUID, AssessmentBlueprintSlotDto>forName("assessmentBlueprintSlotById")
            .registerMappedBatchLoader((Set<UUID> slotIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentBlueprintSlotRepository.findByIdIn(slotIds)
                    .stream()
                    .map(AssessmentBlueprintSlotDtoMapper::toDto)
                    .collect(Collectors.toMap(slot -> slot.id(), dto -> dto)))
            );

        registry.<UUID, List<AssessmentMemberDto>>forName("assessmentMembersByAssessmentId")
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<AssessmentMemberDto>> result = new HashMap<>();
                    assessmentIds.forEach(id -> result.put(id, List.of()));
                    assessmentMemberRepository.findByAssessmentIdIn(assessmentIds).stream()
                        .map(AssessmentMemberDtoMapper::toDto)
                        .collect(Collectors.groupingBy(member -> member.assessmentId()))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<AssessmentPaperDto>>forName("assessmentPapersByAssessmentId")
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<AssessmentPaperDto>> result = new HashMap<>();
                    assessmentIds.forEach(id -> result.put(id, List.of()));
                    assessmentPaperRepository.findByAssessmentIdIn(assessmentIds).stream()
                        .map(AssessmentPaperDtoMapper::toDto)
                        .collect(Collectors.groupingBy(paper -> paper.assessmentId()))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<AssessmentPaperSectionDto>>forName("assessmentPaperSectionsByPaperId")
            .registerMappedBatchLoader((Set<UUID> paperIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<AssessmentPaperSectionDto>> result = new HashMap<>();
                    paperIds.forEach(id -> result.put(id, List.of()));
                    assessmentPaperSectionRepository.findByPaperIdIn(paperIds).stream()
                        .map(AssessmentPaperSectionDtoMapper::toDto)
                        .collect(Collectors.groupingBy(section -> section.paperId()))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<AssessmentPaperItemDto>>forName("assessmentPaperItemsBySectionId")
            .registerMappedBatchLoader((Set<UUID> sectionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<AssessmentPaperItemDto>> result = new HashMap<>();
                    sectionIds.forEach(id -> result.put(id, List.of()));
                    assessmentPaperItemRepository.findBySectionIdIn(sectionIds).stream()
                        .map(AssessmentPaperItemDtoMapper::toDto)
                        .collect(Collectors.groupingBy(item -> item.sectionId()))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<AssessmentScheduleDto>>forName("assessmentSchedulesByAssessmentId")
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<AssessmentScheduleDto>> result = new HashMap<>();
                    assessmentIds.forEach(id -> result.put(id, List.of()));
                    assessmentScheduleRepository.findByAssessmentIdIn(assessmentIds).stream()
                        .map(AssessmentScheduleDtoMapper::toDto)
                        .collect(Collectors.groupingBy(schedule -> schedule.assessmentId()))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, AssessmentSecurePoolDto>forName("assessmentSecurePoolByAssessmentId")
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentSecurePoolRepository.findByAssessmentIdIn(assessmentIds)
                    .stream()
                    .map(AssessmentSecurePoolDtoMapper::toDto)
                    .collect(Collectors.toMap(pool -> pool.assessmentId(), dto -> dto)))
            );

        registry.<UUID, Integer>forName("assessmentCandidateCountByAssessmentId")
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, Integer> result = new HashMap<>();
                    assessmentIds.forEach(id -> result.put(id, 0));
                    assessmentCandidateRepository.countByAssessmentIdIn(assessmentIds)
                        .forEach((assessmentId, count) -> result.put(assessmentId, Math.toIntExact(count)));
                    return result;
                })
            );

        registry.<UUID, UUID>forName("assessmentSchoolClassIdByAssessmentId")
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, UUID> firstStudentIdByAssessment = new HashMap<>();
                    assessmentCandidateRepository.findByAssessmentIdIn(assessmentIds).forEach(candidate ->
                        firstStudentIdByAssessment.putIfAbsent(candidate.getAssessmentId(), candidate.getStudentId())
                    );

                    Map<UUID, List<SchoolClassUser>> activeClassByStudentId = schoolClassUserRepository.findByUserIdIn(firstStudentIdByAssessment.values())
                        .stream()
                        .filter(user -> user.isActive())
                        .collect(Collectors.groupingBy(user -> user.getUserId()));

                    Map<UUID, UUID> result = new HashMap<>();
                    firstStudentIdByAssessment.forEach((assessmentId, studentId) -> {
                        List<SchoolClassUser> activeClasses = activeClassByStudentId.get(studentId);
                        if (activeClasses != null && !activeClasses.isEmpty()) {
                            result.put(assessmentId, activeClasses.get(0).getSchoolClassId());
                        }
                    });
                    return result;
                })
            );
    }
}

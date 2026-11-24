package com.sep.vox.interfaces.graphql.dataloader;

import com.sep.vox.domain.model.school.SchoolClassUser;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class AssessmentGraphQLDataLoaderConfig {

    public static final String ASSESSMENT_BY_ASSESSMENT_ID = "assessmentByAssessmentId";
    public static final String ASSESSMENT_SCHEDULE_BY_ASSESSMENT_SCHEDULE_ID = "assessmentScheduleByAssessmentScheduleId";
    public static final String ASSESSMENT_PAPER_BY_ASSESSMENT_PAPER_ID = "assessmentPaperByAssessmentPaperId";
    public static final String ASSESSMENT_CANDIDATE_ATTEMPTS_BY_ASSESSMENT_CANDIDATE_ID = "assessmentCandidateAttemptsByAssessmentCandidateId";
    public static final String ASSESSMENT_BLUEPRINT_BY_ASSESSMENT_BLUEPRINT_ID = "assessmentBlueprintByAssessmentBlueprintId";
    public static final String ASSESSMENT_BLUEPRINT_VERSION_BY_ASSESSMENT_BLUEPRINT_VERSION_ID = "assessmentBlueprintVersionByAssessmentBlueprintVersionId";
    public static final String ASSESSMENT_BLUEPRINT_SLOT_BY_ASSESSMENT_BLUEPRINT_SLOT_ID = "assessmentBlueprintSlotByAssessmentBlueprintSlotId";
    public static final String ASSESSMENT_MEMBERS_BY_ASSESSMENT_ID = "assessmentMembersByAssessmentId";
    public static final String ASSESSMENT_PAPERS_BY_ASSESSMENT_ID = "assessmentPapersByAssessmentId";
    public static final String ASSESSMENT_PAPER_SECTIONS_BY_ASSESSMENT_PAPER_ID = "assessmentPaperSectionsByAssessmentPaperId";
    public static final String ASSESSMENT_PAPER_ITEMS_BY_ASSESSMENT_PAPER_SECTION_ID = "assessmentPaperItemsByAssessmentPaperSectionId";
    public static final String ASSESSMENT_SCHEDULES_BY_ASSESSMENT_ID = "assessmentSchedulesByAssessmentId";
    public static final String ASSESSMENT_SECURE_POOL_BY_ASSESSMENT_ID = "assessmentSecurePoolByAssessmentId";
    public static final String ASSESSMENT_CANDIDATE_COUNT_BY_ASSESSMENT_ID = "assessmentCandidateCountByAssessmentId";
    public static final String ASSESSMENT_SCHOOL_CLASS_ID_BY_ASSESSMENT_ID = "assessmentSchoolClassIdByAssessmentId";

    private final BatchLoaderRegistry registry;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository assessmentScheduleRepository;
    private final AssessmentPaperRepository assessmentPaperRepository;
    private final AssessmentBlueprintRepository assessmentBlueprintRepository;
    private final AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;
    private final AssessmentBlueprintSlotRepository assessmentBlueprintSlotRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final AssessmentPaperSectionRepository assessmentPaperSectionRepository;
    private final AssessmentPaperItemRepository assessmentPaperItemRepository;
    private final AssessmentSecurePoolRepository assessmentSecurePoolRepository;
    private final AssessmentCandidateRepository assessmentCandidateRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final ResolveAssessmentCandidateAttemptsUseCase resolveAssessmentCandidateAttemptsUseCase;

    @PostConstruct
    void registerLoaders() {
        registerAssessmentByAssessmentId();
        registerAssessmentScheduleByAssessmentScheduleId();
        registerAssessmentPaperByAssessmentPaperId();
        registerAssessmentCandidateAttemptsByAssessmentCandidateId();
        registerAssessmentBlueprintByAssessmentBlueprintId();
        registerAssessmentBlueprintVersionByAssessmentBlueprintVersionId();
        registerAssessmentBlueprintSlotByAssessmentBlueprintSlotId();
        registerAssessmentMembersByAssessmentId();
        registerAssessmentPapersByAssessmentId();
        registerAssessmentPaperSectionsByAssessmentPaperId();
        registerAssessmentPaperItemsByAssessmentPaperSectionId();
        registerAssessmentSchedulesByAssessmentId();
        registerAssessmentSecurePoolByAssessmentId();
        registerAssessmentCandidateCountByAssessmentId();
        registerAssessmentSchoolClassIdByAssessmentId();
    }

    private void registerAssessmentByAssessmentId() {
        registry.<UUID, AssessmentDto>forName(ASSESSMENT_BY_ASSESSMENT_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentRepository.findByIdIn(keys)
                    .stream()
                    .map(AssessmentDtoMapper::toDto)
                    .collect(Collectors.toMap(dto -> dto.id(), dto -> dto)))
            );
    }

    private void registerAssessmentScheduleByAssessmentScheduleId() {
        registry.<UUID, AssessmentScheduleDto>forName(ASSESSMENT_SCHEDULE_BY_ASSESSMENT_SCHEDULE_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentScheduleRepository.findByIdIn(keys)
                    .stream()
                    .map(AssessmentScheduleDtoMapper::toDto)
                    .collect(Collectors.toMap(dto -> dto.id(), dto -> dto)))
            );
    }

    private void registerAssessmentPaperByAssessmentPaperId() {
        registry.<UUID, AssessmentPaperDto>forName(ASSESSMENT_PAPER_BY_ASSESSMENT_PAPER_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentPaperRepository.findByIdIn(keys)
                    .stream()
                    .map(AssessmentPaperDtoMapper::toDto)
                    .collect(Collectors.toMap(dto -> dto.id(), dto -> dto)))
            );
    }

    private void registerAssessmentCandidateAttemptsByAssessmentCandidateId() {
        registry.<UUID, AssessmentCandidateAttempts>forName(ASSESSMENT_CANDIDATE_ATTEMPTS_BY_ASSESSMENT_CANDIDATE_ID)
            .registerMappedBatchLoader((Set<UUID> assessmentCandidateIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> resolveAssessmentCandidateAttemptsUseCase.executeBatch(assessmentCandidateIds))
            );
    }

    private void registerAssessmentBlueprintByAssessmentBlueprintId() {
        registry.<UUID, AssessmentBlueprintDto>forName(ASSESSMENT_BLUEPRINT_BY_ASSESSMENT_BLUEPRINT_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentBlueprintRepository.findByIdIn(keys)
                    .stream()
                    .map(AssessmentBlueprintDtoMapper::toDto)
                    .collect(Collectors.toMap(dto -> dto.id(), dto -> dto)))
            );
    }

    private void registerAssessmentBlueprintVersionByAssessmentBlueprintVersionId() {
        registry.<UUID, AssessmentBlueprintVersionDto>forName(ASSESSMENT_BLUEPRINT_VERSION_BY_ASSESSMENT_BLUEPRINT_VERSION_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentBlueprintVersionRepository.findByIdIn(keys)
                    .stream()
                    .map(AssessmentBlueprintVersionDtoMapper::toDto)
                    .collect(Collectors.toMap(dto -> dto.id(), dto -> dto)))
            );
    }

    // The blueprint slot that generated each question in the assessment paper. The mapped loader returns null for
    // a key it cannot find, which is intended and matches the fail-open behavior in
    // UpdateAssessmentPaperItemUseCase: once the slot is deleted, the question is assigned freely.
    private void registerAssessmentBlueprintSlotByAssessmentBlueprintSlotId() {
        registry.<UUID, AssessmentBlueprintSlotDto>forName(ASSESSMENT_BLUEPRINT_SLOT_BY_ASSESSMENT_BLUEPRINT_SLOT_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentBlueprintSlotRepository.findByIdIn(keys)
                    .stream()
                    .map(AssessmentBlueprintSlotDtoMapper::toDto)
                    .collect(Collectors.toMap(dto -> dto.id(), dto -> dto)))
            );
    }

    private void registerAssessmentMembersByAssessmentId() {
        registry.<UUID, List<AssessmentMemberDto>>forName(ASSESSMENT_MEMBERS_BY_ASSESSMENT_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> groupByParentId(keys,
                    assessmentMemberRepository.findByAssessmentIdIn(keys).stream().map(AssessmentMemberDtoMapper::toDto).toList(),
                    dto -> dto.assessmentId()))
            );
    }

    private void registerAssessmentPapersByAssessmentId() {
        registry.<UUID, List<AssessmentPaperDto>>forName(ASSESSMENT_PAPERS_BY_ASSESSMENT_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> groupByParentId(keys,
                    assessmentPaperRepository.findByAssessmentIdIn(keys).stream().map(AssessmentPaperDtoMapper::toDto).toList(),
                    dto -> dto.assessmentId()))
            );
    }

    private void registerAssessmentPaperSectionsByAssessmentPaperId() {
        registry.<UUID, List<AssessmentPaperSectionDto>>forName(ASSESSMENT_PAPER_SECTIONS_BY_ASSESSMENT_PAPER_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> groupByParentId(keys,
                    assessmentPaperSectionRepository.findByPaperIdIn(keys).stream().map(AssessmentPaperSectionDtoMapper::toDto).toList(),
                    dto -> dto.paperId()))
            );
    }

    private void registerAssessmentPaperItemsByAssessmentPaperSectionId() {
        registry.<UUID, List<AssessmentPaperItemDto>>forName(ASSESSMENT_PAPER_ITEMS_BY_ASSESSMENT_PAPER_SECTION_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> groupByParentId(keys,
                    assessmentPaperItemRepository.findBySectionIdIn(keys).stream().map(AssessmentPaperItemDtoMapper::toDto).toList(),
                    dto -> dto.sectionId()))
            );
    }

    private void registerAssessmentSchedulesByAssessmentId() {
        registry.<UUID, List<AssessmentScheduleDto>>forName(ASSESSMENT_SCHEDULES_BY_ASSESSMENT_ID)
            .registerMappedBatchLoader((Set<UUID> keys, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> groupByParentId(keys,
                    assessmentScheduleRepository.findByAssessmentIdIn(keys).stream().map(AssessmentScheduleDtoMapper::toDto).toList(),
                    dto -> dto.assessmentId()))
            );
    }

    private void registerAssessmentSecurePoolByAssessmentId() {
        registry.<UUID, AssessmentSecurePoolDto>forName(ASSESSMENT_SECURE_POOL_BY_ASSESSMENT_ID)
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> assessmentSecurePoolRepository.findByAssessmentIdIn(assessmentIds)
                    .stream()
                    .map(AssessmentSecurePoolDtoMapper::toDto)
                    .collect(Collectors.toMap(pool -> pool.assessmentId(), dto -> dto)))
            );
    }

    private void registerAssessmentCandidateCountByAssessmentId() {
        registry.<UUID, Integer>forName(ASSESSMENT_CANDIDATE_COUNT_BY_ASSESSMENT_ID)
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, Integer> result = new HashMap<>();
                    assessmentIds.forEach(id -> result.put(id, 0));
                    assessmentCandidateRepository.countByAssessmentIdIn(assessmentIds)
                        .forEach((assessmentId, count) -> result.put(assessmentId, Math.toIntExact(count)));
                    return result;
                })
            );
    }

    private void registerAssessmentSchoolClassIdByAssessmentId() {
        registry.<UUID, UUID>forName(ASSESSMENT_SCHOOL_CLASS_ID_BY_ASSESSMENT_ID)
            .registerMappedBatchLoader((Set<UUID> assessmentIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> firstActiveSchoolClassIdByAssessmentId(assessmentIds))
            );
    }

    private Map<UUID, UUID> firstActiveSchoolClassIdByAssessmentId(Set<UUID> assessmentIds) {
        Map<UUID, UUID> firstStudentIdByAssessment = new HashMap<>();
        assessmentCandidateRepository.findFirstCandidateByAssessmentIdIn(assessmentIds).forEach(candidate ->
            firstStudentIdByAssessment.put(candidate.getAssessmentId(), candidate.getStudentId())
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
    }

    private static <V> Map<UUID, List<V>> groupByParentId(Collection<UUID> parentIds, List<V> children,
            Function<V, UUID> parentIdOf) {
        Map<UUID, List<V>> result = new HashMap<>();
        parentIds.forEach(id -> result.put(id, List.of()));
        result.putAll(children.stream().collect(Collectors.groupingBy(parentIdOf)));
        return result;
    }
}

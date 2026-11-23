package com.sep.vox.interfaces.graphql.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.BatchLoaderRegistry;

import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionBankGradeDto;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.mapper.QuestionBankDtoMapper;
import com.sep.vox.domain.mapper.QuestionBankGradeDtoMapper;
import com.sep.vox.domain.mapper.QuestionCollaboratorDtoMapper;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.mapper.QuestionEvaluationGuideDtoMapper;
import com.sep.vox.domain.mapper.QuestionTopicDtoMapper;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankGradeRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class QuestionGraphQlDataLoaderConfig {

    public QuestionGraphQlDataLoaderConfig(
            BatchLoaderRegistry registry,
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository,
            QuestionBankGradeRepository questionBankGradeRepository,
            QuestionRepository questionRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {

        registry.<UUID, QuestionBankDto>forName("questionBankById")
            .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> questionBankRepository.findByIdIn(ids).stream()
                    .map(QuestionBankDtoMapper::toDto)
                    .collect(Collectors.toMap(bank -> bank.id(), dto -> dto)))
            );

        registry.<UUID, QuestionTopicDto>forName("questionTopicById")
            .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> questionTopicRepository.findByIdIn(ids).stream()
                    .map(QuestionTopicDtoMapper::toDto)
                    .collect(Collectors.toMap(topic -> topic.id(), dto -> dto)))
            );

        registry.<UUID, List<QuestionAssetDto>>forName("questionAssetsByQuestionId")
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<QuestionAssetDto>> result = new HashMap<>();
                    questionIds.forEach(id -> result.put(id, List.of()));
                    questionAssetRepository.findByQuestionIdIn(questionIds).stream()
                        .map(QuestionAssetDtoMapper::toDto)
                        .collect(Collectors.groupingBy(asset -> asset.questionId()))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, QuestionEvaluationGuideDto>forName("questionEvaluationGuideByQuestionId")
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> questionEvaluationGuideRepository.findByQuestionIdIn(questionIds).stream()
                    .map(QuestionEvaluationGuideDtoMapper::toDto)
                    .collect(Collectors.toMap(guide -> guide.questionId(), dto -> dto)))
            );

        registry.<UUID, List<QuestionCollaboratorDto>>forName("questionCollaboratorsByQuestionId")
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<QuestionCollaboratorDto>> result = new HashMap<>();
                    questionIds.forEach(id -> result.put(id, List.of()));
                    questionCollaboratorRepository.findByQuestionIdIn(questionIds).stream()
                        .map(QuestionCollaboratorDtoMapper::toDto)
                        .collect(Collectors.groupingBy(collaborator -> collaborator.questionId()))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, List<QuestionBankGradeDto>>forName("questionBankGradesByBankId")
            .registerMappedBatchLoader((Set<UUID> bankIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<QuestionBankGradeDto>> result = new HashMap<>();
                    bankIds.forEach(id -> result.put(id, List.of()));
                    questionBankGradeRepository.findByQuestionBankIdIn(bankIds).stream()
                        .map(QuestionBankGradeDtoMapper::toDto)
                        .collect(Collectors.groupingBy(grade -> grade.questionBankId()))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, QuestionDto>forName("questionByIdAccessible")
            .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    long startedAt = System.nanoTime();
                    UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
                    boolean systemAdmin = userContextPort.isSystemAdmin();
                    UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
                        .map(schoolUser -> schoolUser.getSchoolId())
                        .orElse(null);
                    boolean schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                        .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
                    Map<UUID, QuestionDto> result = questionRepository.findAccessibleByIdIn(ids, currentUserId, currentSchoolId, systemAdmin, schoolAdmin)
                        .stream()
                        .map(QuestionDtoMapper::toQuestionDto)
                        .collect(Collectors.toMap(question -> question.id(), dto -> dto));
                    log.info("[blueprint-perf] loader=questionByIdAccessible batchSize={} tookMs={}",
                        ids.size(), (System.nanoTime() - startedAt) / 1_000_000);
                    return result;
                })
            );

        // Does NOT check permissions -- only use it where view permission has already been confirmed through
        // the parent (e.g. AssessmentBlueprintSlot.fixedQuestion, after ViewAssessmentBlueprintDetailsUseCase.hasAccess has
        // already passed). This avoids repeating the permission OR-tree of questionByIdAccessible.
        registry.<UUID, QuestionDto>forName("questionByIdBasic")
            .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    long startedAt = System.nanoTime();
                    Map<UUID, QuestionDto> result = questionRepository.findByIdIn(ids).stream()
                        .map(QuestionDtoMapper::toQuestionDto)
                        .collect(Collectors.toMap(question -> question.id(), dto -> dto));
                    log.info("[blueprint-perf] loader=questionByIdBasic batchSize={} tookMs={}",
                        ids.size(), (System.nanoTime() - startedAt) / 1_000_000);
                    return result;
                })
            );
    }
}

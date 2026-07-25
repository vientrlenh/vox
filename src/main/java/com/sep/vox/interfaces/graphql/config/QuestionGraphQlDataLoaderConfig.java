package com.sep.vox.interfaces.graphql.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dataloader.BatchLoaderEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class QuestionGraphQlDataLoaderConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionGraphQlDataLoaderConfig.class);

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
                    .collect(Collectors.toMap(QuestionBankDto::id, dto -> dto)))
            );

        registry.<UUID, QuestionTopicDto>forName("questionTopicById")
            .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> questionTopicRepository.findByIdIn(ids).stream()
                    .map(QuestionTopicDtoMapper::toDto)
                    .collect(Collectors.toMap(QuestionTopicDto::id, dto -> dto)))
            );

        registry.<UUID, List<QuestionAssetDto>>forName("questionAssetsByQuestionId")
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<QuestionAssetDto>> result = new HashMap<>();
                    questionIds.forEach(id -> result.put(id, List.of()));
                    questionAssetRepository.findByQuestionIdIn(questionIds).stream()
                        .map(QuestionAssetDtoMapper::toDto)
                        .collect(Collectors.groupingBy(QuestionAssetDto::questionId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, QuestionEvaluationGuideDto>forName("questionEvaluationGuideByQuestionId")
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> questionEvaluationGuideRepository.findByQuestionIdIn(questionIds).stream()
                    .map(QuestionEvaluationGuideDtoMapper::toDto)
                    .collect(Collectors.toMap(QuestionEvaluationGuideDto::questionId, dto -> dto)))
            );

        registry.<UUID, List<QuestionCollaboratorDto>>forName("questionCollaboratorsByQuestionId")
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<QuestionCollaboratorDto>> result = new HashMap<>();
                    questionIds.forEach(id -> result.put(id, List.of()));
                    questionCollaboratorRepository.findByQuestionIdIn(questionIds).stream()
                        .map(QuestionCollaboratorDtoMapper::toDto)
                        .collect(Collectors.groupingBy(QuestionCollaboratorDto::questionId))
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
                        .collect(Collectors.groupingBy(QuestionBankGradeDto::questionBankId))
                        .forEach(result::put);
                    return result;
                })
            );

        registry.<UUID, QuestionDto>forName("questionByIdAccessible")
            .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    var startedAt = System.nanoTime();
                    var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
                    var systemAdmin = userContextPort.isSystemAdmin();
                    var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
                        .map(schoolUser -> schoolUser.getSchoolId())
                        .orElse(null);
                    var schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                        .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
                    var result = questionRepository.findAccessibleByIdIn(ids, currentUserId, currentSchoolId, systemAdmin, schoolAdmin)
                        .stream()
                        .map(QuestionDtoMapper::toQuestionDto)
                        .collect(Collectors.toMap(QuestionDto::id, dto -> dto));
                    LOGGER.info("[blueprint-perf] loader=questionByIdAccessible batchSize={} tookMs={}",
                        ids.size(), (System.nanoTime() - startedAt) / 1_000_000);
                    return result;
                })
            );

        // KHÔNG check quyền — chỉ dùng ở nơi quyền xem đã được xác nhận qua parent
        // (vd: ExamBlueprintSlot.fixedQuestion, sau khi ViewExamBlueprintDetailsUseCase.hasAccess
        // đã pass rồi). Tránh lặp lại OR-tree phân quyền của questionByIdAccessible.
        registry.<UUID, QuestionDto>forName("questionByIdBasic")
            .registerMappedBatchLoader((Set<UUID> ids, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    var startedAt = System.nanoTime();
                    var result = questionRepository.findByIdIn(ids).stream()
                        .map(QuestionDtoMapper::toQuestionDto)
                        .collect(Collectors.toMap(QuestionDto::id, dto -> dto));
                    LOGGER.info("[blueprint-perf] loader=questionByIdBasic batchSize={} tookMs={}",
                        ids.size(), (System.nanoTime() - startedAt) / 1_000_000);
                    return result;
                })
            );
    }
}

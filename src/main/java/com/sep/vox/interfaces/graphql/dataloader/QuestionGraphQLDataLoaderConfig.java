package com.sep.vox.interfaces.graphql.dataloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Slf4j
public class QuestionGraphQLDataLoaderConfig {

    public static final String QUESTION_BANK_BY_QUESTION_BANK_ID = "questionBankByQuestionBankId";
    public static final String QUESTION_TOPIC_BY_QUESTION_TOPIC_ID = "questionTopicByQuestionTopicId";
    public static final String QUESTION_ASSETS_BY_QUESTION_ID = "questionAssetsByQuestionId";
    public static final String QUESTION_EVALUATION_GUIDE_BY_QUESTION_ID = "questionEvaluationGuideByQuestionId";
    public static final String QUESTION_COLLABORATORS_BY_QUESTION_ID = "questionCollaboratorsByQuestionId";
    public static final String QUESTION_BANK_GRADES_BY_QUESTION_BANK_ID = "questionBankGradesByQuestionBankId";
    public static final String ACCESSIBLE_QUESTION_BY_QUESTION_ID = "accessibleQuestionByQuestionId";
    public static final String QUESTION_BY_QUESTION_ID_WITHOUT_ACCESS_CHECK = "questionByQuestionIdWithoutAccessCheck";

    private final BatchLoaderRegistry registry;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final QuestionBankGradeRepository questionBankGradeRepository;
    private final QuestionRepository questionRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @PostConstruct
    void registerLoaders() {
        registerQuestionBankByQuestionBankId();
        registerQuestionTopicByQuestionTopicId();
        registerQuestionAssetsByQuestionId();
        registerQuestionEvaluationGuideByQuestionId();
        registerQuestionCollaboratorsByQuestionId();
        registerQuestionBankGradesByQuestionBankId();
        registerAccessibleQuestionByQuestionId();
        registerQuestionByQuestionIdWithoutAccessCheck();
    }

    private void registerQuestionBankByQuestionBankId() {
        registry.<UUID, QuestionBankDto>forName(QUESTION_BANK_BY_QUESTION_BANK_ID)
            .registerMappedBatchLoader((Set<UUID> questionBankIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> questionBankRepository.findByIdIn(questionBankIds).stream()
                    .map(QuestionBankDtoMapper::toDto)
                    .collect(Collectors.toMap(bank -> bank.id(), dto -> dto)))
            );
    }

    private void registerQuestionTopicByQuestionTopicId() {
        registry.<UUID, QuestionTopicDto>forName(QUESTION_TOPIC_BY_QUESTION_TOPIC_ID)
            .registerMappedBatchLoader((Set<UUID> questionTopicIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> questionTopicRepository.findByIdIn(questionTopicIds).stream()
                    .map(QuestionTopicDtoMapper::toDto)
                    .collect(Collectors.toMap(topic -> topic.id(), dto -> dto)))
            );
    }

    private void registerQuestionAssetsByQuestionId() {
        registry.<UUID, List<QuestionAssetDto>>forName(QUESTION_ASSETS_BY_QUESTION_ID)
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
    }

    private void registerQuestionEvaluationGuideByQuestionId() {
        registry.<UUID, QuestionEvaluationGuideDto>forName(QUESTION_EVALUATION_GUIDE_BY_QUESTION_ID)
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> questionEvaluationGuideRepository.findByQuestionIdIn(questionIds).stream()
                    .map(QuestionEvaluationGuideDtoMapper::toDto)
                    .collect(Collectors.toMap(guide -> guide.questionId(), dto -> dto)))
            );
    }

    private void registerQuestionCollaboratorsByQuestionId() {
        registry.<UUID, List<QuestionCollaboratorDto>>forName(QUESTION_COLLABORATORS_BY_QUESTION_ID)
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
    }

    private void registerQuestionBankGradesByQuestionBankId() {
        registry.<UUID, List<QuestionBankGradeDto>>forName(QUESTION_BANK_GRADES_BY_QUESTION_BANK_ID)
            .registerMappedBatchLoader((Set<UUID> questionBankIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    Map<UUID, List<QuestionBankGradeDto>> result = new HashMap<>();
                    questionBankIds.forEach(id -> result.put(id, List.of()));
                    questionBankGradeRepository.findByQuestionBankIdIn(questionBankIds).stream()
                        .map(QuestionBankGradeDtoMapper::toDto)
                        .collect(Collectors.groupingBy(grade -> grade.questionBankId()))
                        .forEach(result::put);
                    return result;
                })
            );
    }

    private void registerAccessibleQuestionByQuestionId() {
        registry.<UUID, QuestionDto>forName(ACCESSIBLE_QUESTION_BY_QUESTION_ID)
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    long startedAt = System.nanoTime();
                    Map<UUID, QuestionDto> result = findAccessibleQuestionsByQuestionId(questionIds);
                    log.info("[blueprint-perf] loader={} batchSize={} tookMs={}",
                        ACCESSIBLE_QUESTION_BY_QUESTION_ID, questionIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
                    return result;
                })
            );
    }

    private Map<UUID, QuestionDto> findAccessibleQuestionsByQuestionId(Set<UUID> questionIds) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean systemAdmin = userContextPort.isSystemAdmin();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        return questionRepository.findAccessibleByIdIn(questionIds, currentUserId, currentSchoolId, systemAdmin, schoolAdmin)
            .stream()
            .map(QuestionDtoMapper::toQuestionDto)
            .collect(Collectors.toMap(question -> question.id(), dto -> dto));
    }

    /**
     * Does NOT check permissions. Only use it where view permission has already been confirmed through the parent
     * (for example {@code AssessmentBlueprintSlot.fixedQuestion}, after
     * {@code ViewAssessmentBlueprintDetailsUseCase.hasAccess} has already passed). This avoids repeating the
     * permission OR-tree of {@link #ACCESSIBLE_QUESTION_BY_QUESTION_ID}.
     */
    private void registerQuestionByQuestionIdWithoutAccessCheck() {
        registry.<UUID, QuestionDto>forName(QUESTION_BY_QUESTION_ID_WITHOUT_ACCESS_CHECK)
            .registerMappedBatchLoader((Set<UUID> questionIds, BatchLoaderEnvironment env) ->
                Mono.fromSupplier(() -> {
                    long startedAt = System.nanoTime();
                    Map<UUID, QuestionDto> result = questionRepository.findByIdIn(questionIds).stream()
                        .map(QuestionDtoMapper::toQuestionDto)
                        .collect(Collectors.toMap(question -> question.id(), dto -> dto));
                    log.info("[blueprint-perf] loader={} batchSize={} tookMs={}",
                        QUESTION_BY_QUESTION_ID_WITHOUT_ACCESS_CHECK, questionIds.size(), (System.nanoTime() - startedAt) / 1_000_000);
                    return result;
                })
            );
    }
}

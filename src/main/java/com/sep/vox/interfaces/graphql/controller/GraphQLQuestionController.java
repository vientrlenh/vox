package com.sep.vox.interfaces.graphql.controller;

import com.sep.vox.interfaces.graphql.dataloader.GraphQLDataLoaders;
import com.sep.vox.interfaces.graphql.dataloader.UserGraphQLDataLoaderConfig;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.query.question.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.query.question.ViewQuestionStatusCountsQuery;
import com.sep.vox.application.port.input.query.question.ViewQuestionsQuery;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionStatusCountsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsForAssessmentPaperUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.QuestionStatusCountInfo;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.interfaces.shared.PageArguments;
import com.sep.vox.interfaces.graphql.dataloader.QuestionGraphQLDataLoaderConfig;

@Controller
@RequiredArgsConstructor
public class GraphQLQuestionController {

    private final ViewQuestionsUseCase viewQuestionsUseCase;
    private final ViewQuestionsForAssessmentPaperUseCase viewQuestionsForAssessmentPaperUseCase;
    private final ViewQuestionDetailsUseCase viewQuestionDetailsUseCase;
    private final ViewQuestionStatusCountsUseCase viewQuestionStatusCountsUseCase;
    private final UserContextPort userContextPort;

    @QueryMapping(name = "questionStatusCounts")
    public List<QuestionStatusCountInfo> questionStatusCounts(
            @Argument(name = "questionBankId") UUID questionBankId,
            @Argument(name = "questionTopicId") UUID questionTopicId,
            @Argument(name = "topicName") String topicName,
            @Argument(name = "type") QuestionType type,
            @Argument(name = "sharing") QuestionSharing sharing,
            @Argument(name = "scope") String scope,
            @Argument(name = "keyword") String keyword) {
        return viewQuestionStatusCountsUseCase.execute(new ViewQuestionStatusCountsQuery(
            questionBankId,
            questionTopicId,
            topicName,
            type,
            sharing,
            scope,
            keyword
        ));
    }

    @QueryMapping(name = "questions")
    public PageResult<QuestionDto> questions(
            @Argument(name = "questionBankId") UUID questionBankId,
            @Argument(name = "questionTopicId") UUID questionTopicId,
            @Argument(name = "topicName") String topicName,
            @Argument(name = "status") QuestionStatus status,
            @Argument(name = "type") QuestionType type,
            @Argument(name = "sharing") QuestionSharing sharing,
            @Argument(name = "scope") String scope,
            @Argument(name = "assetType") String assetType,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        PageArguments.validate(page, size);
        ViewQuestionsQuery query = toViewQuestionsQuery(questionBankId, questionTopicId, topicName, status, type, sharing,
            scope, assetType, keyword, page, size);
        return viewQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "questionsForAssessmentPaper")
    public PageResult<QuestionDto> questionsForAssessmentPaper(
            @Argument(name = "questionBankId") UUID questionBankId,
            @Argument(name = "questionTopicId") UUID questionTopicId,
            @Argument(name = "topicName") String topicName,
            @Argument(name = "status") QuestionStatus status,
            @Argument(name = "type") QuestionType type,
            @Argument(name = "sharing") QuestionSharing sharing,
            @Argument(name = "scope") String scope,
            @Argument(name = "assetType") String assetType,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        PageArguments.validate(page, size);
        ViewQuestionsQuery query = toViewQuestionsQuery(questionBankId, questionTopicId, topicName, status, type, sharing,
            scope, assetType, keyword, page, size);
        return viewQuestionsForAssessmentPaperUseCase.execute(query);
    }

    private ViewQuestionsQuery toViewQuestionsQuery(UUID questionBankId, UUID questionTopicId, String topicName,
            QuestionStatus status, QuestionType type, QuestionSharing sharing, String scope, String assetType,
            String keyword, int page, int size) {
        return new ViewQuestionsQuery(
            questionBankId,
            questionTopicId,
            topicName,
            status,
            type,
            sharing,
            assetType,
            scope,
            keyword,
            page,
            size
        );
    }

    @QueryMapping(name = "question")
    public QuestionDto question(@Argument(name = "id") UUID id) {
        return viewQuestionDetailsUseCase.execute(new ViewQuestionDetailsQuery(id));
    }

    @SchemaMapping(typeName = "Question", field = "bank")
    public CompletableFuture<QuestionBankDto> bank(QuestionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, QuestionBankDto> loader = GraphQLDataLoaders.require(env, QuestionGraphQLDataLoaderConfig.QUESTION_BANK_BY_QUESTION_BANK_ID);
        return loader.load(source.questionBankId());
    }

    @SchemaMapping(typeName = "Question", field = "topic")
    public CompletableFuture<QuestionTopicDto> topic(QuestionDto source, DataFetchingEnvironment env) {
        if (source.questionTopicId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, QuestionTopicDto> loader = GraphQLDataLoaders.require(env, QuestionGraphQLDataLoaderConfig.QUESTION_TOPIC_BY_QUESTION_TOPIC_ID);
        return loader.load(source.questionTopicId());
    }

    @SchemaMapping(typeName = "QuestionTopic", field = "bank")
    public CompletableFuture<QuestionBankDto> topicBank(QuestionTopicDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, QuestionBankDto> loader = GraphQLDataLoaders.require(env, QuestionGraphQLDataLoaderConfig.QUESTION_BANK_BY_QUESTION_BANK_ID);
        return loader.load(source.questionBankId());
    }

    @SchemaMapping(typeName = "Question", field = "assets")
    public CompletableFuture<List<QuestionAssetDto>> assets(QuestionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<QuestionAssetDto>> loader = GraphQLDataLoaders.require(env, QuestionGraphQLDataLoaderConfig.QUESTION_ASSETS_BY_QUESTION_ID);
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Question", field = "evaluationGuide")
    public CompletableFuture<QuestionEvaluationGuideDto> evaluationGuide(QuestionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, QuestionEvaluationGuideDto> loader = GraphQLDataLoaders.require(env, QuestionGraphQLDataLoaderConfig.QUESTION_EVALUATION_GUIDE_BY_QUESTION_ID);
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Question", field = "collaborators")
    public CompletableFuture<List<QuestionCollaboratorDto>> collaborators(QuestionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<QuestionCollaboratorDto>> loader = GraphQLDataLoaders.require(env, QuestionGraphQLDataLoaderConfig.QUESTION_COLLABORATORS_BY_QUESTION_ID);
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Question", field = "usableInAssessment")
    public CompletableFuture<Boolean> usableInAssessment(QuestionDto source, DataFetchingEnvironment env) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (currentUserId.equals(source.createdBy()) || QuestionSharing.SCHOOL_SHARED.name().equals(source.sharing())) {
            return CompletableFuture.completedFuture(true);
        }
        DataLoader<UUID, List<QuestionCollaboratorDto>> loader = GraphQLDataLoaders.require(env, QuestionGraphQLDataLoaderConfig.QUESTION_COLLABORATORS_BY_QUESTION_ID);
        return loader.load(source.id()).thenApply(collaborators -> collaborators.stream()
            .filter(collaborator -> collaborator.userId().equals(currentUserId))
            .findFirst()
            .map(collaborator -> !QuestionCollaboratorPermission.READ_ONLY.name().equals(collaborator.permission()))
            .orElse(false));
    }

    @SchemaMapping(typeName = "Question", field = "createdByUser")
    public CompletableFuture<UserDto> createdByUser(QuestionDto source, DataFetchingEnvironment env) {
        if (source.createdBy() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, UserDto> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.USER_BY_USER_ID);
        return loader.load(source.createdBy());
    }

    @SchemaMapping(typeName = "QuestionCollaborator", field = "user")
    public CompletableFuture<UserDto> collaboratorUser(QuestionCollaboratorDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = GraphQLDataLoaders.require(env, UserGraphQLDataLoaderConfig.USER_BY_USER_ID);
        return loader.load(source.userId());
    }
}

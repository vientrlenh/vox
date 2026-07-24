package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import graphql.schema.DataFetchingEnvironment;

import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionsQuery;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsForExamPaperUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
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

@Controller("graphqlQuestionController")
public class QuestionController {

    private final ViewQuestionsUseCase viewQuestionsUseCase;
    private final ViewQuestionsForExamPaperUseCase viewQuestionsForExamPaperUseCase;
    private final ViewQuestionDetailsUseCase viewQuestionDetailsUseCase;
    private final UserContextPort userContextPort;

    public QuestionController(
            ViewQuestionsUseCase viewQuestionsUseCase,
            ViewQuestionsForExamPaperUseCase viewQuestionsForExamPaperUseCase,
            ViewQuestionDetailsUseCase viewQuestionDetailsUseCase,
            UserContextPort userContextPort) {
        this.viewQuestionsUseCase = viewQuestionsUseCase;
        this.viewQuestionsForExamPaperUseCase = viewQuestionsForExamPaperUseCase;
        this.viewQuestionDetailsUseCase = viewQuestionDetailsUseCase;
        this.userContextPort = userContextPort;
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
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        var query = new ViewQuestionsQuery(
            questionBankId,
            questionTopicId,
            topicName,
            status,
            type,
            sharing,
            scope,
            keyword,
            page,
            size
        );
        return viewQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "questionsForExamPaper")
    public PageResult<QuestionDto> questionsForExamPaper(
            @Argument(name = "questionBankId") UUID questionBankId,
            @Argument(name = "questionTopicId") UUID questionTopicId,
            @Argument(name = "topicName") String topicName,
            @Argument(name = "status") QuestionStatus status,
            @Argument(name = "type") QuestionType type,
            @Argument(name = "sharing") QuestionSharing sharing,
            @Argument(name = "scope") String scope,
            @Argument(name = "keyword") String keyword,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        validatePage(page, size);
        var query = new ViewQuestionsQuery(
            questionBankId,
            questionTopicId,
            topicName,
            status,
            type,
            sharing,
            scope,
            keyword,
            page,
            size
        );
        return viewQuestionsForExamPaperUseCase.execute(query);
    }

    @QueryMapping(name = "question")
    public QuestionDto question(@Argument(name = "id") UUID id) {
        return viewQuestionDetailsUseCase.execute(new ViewQuestionDetailsQuery(id));
    }

    @SchemaMapping(typeName = "Question", field = "bank")
    public CompletableFuture<QuestionBankDto> bank(QuestionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, QuestionBankDto> loader = env.getDataLoader("questionBankById");
        return loader.load(source.questionBankId());
    }

    @SchemaMapping(typeName = "Question", field = "topic")
    public CompletableFuture<QuestionTopicDto> topic(QuestionDto source, DataFetchingEnvironment env) {
        if (source.questionTopicId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        DataLoader<UUID, QuestionTopicDto> loader = env.getDataLoader("questionTopicById");
        return loader.load(source.questionTopicId());
    }

    @SchemaMapping(typeName = "QuestionTopic", field = "bank")
    public CompletableFuture<QuestionBankDto> topicBank(QuestionTopicDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, QuestionBankDto> loader = env.getDataLoader("questionBankById");
        return loader.load(source.questionBankId());
    }

    @SchemaMapping(typeName = "Question", field = "assets")
    public CompletableFuture<List<QuestionAssetDto>> assets(QuestionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<QuestionAssetDto>> loader = env.getDataLoader("questionAssetsByQuestionId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Question", field = "evaluationGuide")
    public CompletableFuture<QuestionEvaluationGuideDto> evaluationGuide(QuestionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, QuestionEvaluationGuideDto> loader = env.getDataLoader("questionEvaluationGuideByQuestionId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Question", field = "collaborators")
    public CompletableFuture<List<QuestionCollaboratorDto>> collaborators(QuestionDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, List<QuestionCollaboratorDto>> loader = env.getDataLoader("questionCollaboratorsByQuestionId");
        return loader.load(source.id());
    }

    @SchemaMapping(typeName = "Question", field = "usableInExam")
    public CompletableFuture<Boolean> usableInExam(QuestionDto source, DataFetchingEnvironment env) {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (currentUserId.equals(source.createdBy()) || QuestionSharing.SCHOOL_SHARED.name().equals(source.sharing())) {
            return CompletableFuture.completedFuture(true);
        }
        DataLoader<UUID, List<QuestionCollaboratorDto>> loader = env.getDataLoader("questionCollaboratorsByQuestionId");
        return loader.load(source.id()).thenApply(collaborators -> collaborators.stream()
            .filter(collaborator -> collaborator.userId().equals(currentUserId))
            .findFirst()
            .map(collaborator -> !QuestionCollaboratorPermission.READ_ONLY.name().equals(collaborator.permission()))
            .orElse(false));
    }

    @SchemaMapping(typeName = "QuestionCollaborator", field = "user")
    public CompletableFuture<UserDto> collaboratorUser(QuestionCollaboratorDto source, DataFetchingEnvironment env) {
        DataLoader<UUID, UserDto> loader = env.getDataLoader("userById");
        return loader.load(source.userId());
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
    }
}

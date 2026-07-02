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

import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionsQuery;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsForExamPaperUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicDetailsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.QuestionAssetDtoMapper;
import com.sep.vox.domain.mapper.QuestionCollaboratorDtoMapper;
import com.sep.vox.domain.mapper.QuestionEvaluationGuideDtoMapper;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;

@Controller("graphqlQuestionController")
public class QuestionController {

    private final ViewQuestionsUseCase viewQuestionsUseCase;
    private final ViewQuestionsForExamPaperUseCase viewQuestionsForExamPaperUseCase;
    private final ViewQuestionDetailsUseCase viewQuestionDetailsUseCase;
    private final ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase;
    private final ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;

    public QuestionController(
            ViewQuestionsUseCase viewQuestionsUseCase,
            ViewQuestionsForExamPaperUseCase viewQuestionsForExamPaperUseCase,
            ViewQuestionDetailsUseCase viewQuestionDetailsUseCase,
            ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase,
            ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase,
            QuestionAssetRepository questionAssetRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionCollaboratorRepository questionCollaboratorRepository) {
        this.viewQuestionsUseCase = viewQuestionsUseCase;
        this.viewQuestionsForExamPaperUseCase = viewQuestionsForExamPaperUseCase;
        this.viewQuestionDetailsUseCase = viewQuestionDetailsUseCase;
        this.viewQuestionBankDetailsUseCase = viewQuestionBankDetailsUseCase;
        this.viewQuestionTopicDetailsUseCase = viewQuestionTopicDetailsUseCase;
        this.questionAssetRepository = questionAssetRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionCollaboratorRepository = questionCollaboratorRepository;
    }

    @QueryMapping(name = "questions")
    public PageResult<QuestionDto> questions(
            @Argument UUID questionBankId,
            @Argument UUID questionTopicId,
            @Argument String topicName,
            @Argument QuestionStatus status,
            @Argument QuestionType type,
            @Argument QuestionSharing sharing,
            @Argument String scope,
            @Argument String keyword,
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
            @Argument UUID questionBankId,
            @Argument UUID questionTopicId,
            @Argument String topicName,
            @Argument QuestionStatus status,
            @Argument QuestionType type,
            @Argument QuestionSharing sharing,
            @Argument String scope,
            @Argument String keyword,
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
    public QuestionBankDto bank(QuestionDto source) {
        return viewQuestionBankDetailsUseCase.execute(new ViewQuestionBankDetailsQuery(source.questionBankId()));
    }

    @SchemaMapping(typeName = "Question", field = "topic")
    public QuestionTopicDto topic(QuestionDto source) {
        return viewQuestionTopicDetailsUseCase.execute(new ViewQuestionTopicDetailsQuery(source.questionTopicId()));
    }

    @SchemaMapping(typeName = "QuestionTopic", field = "bank")
    public QuestionBankDto topicBank(QuestionTopicDto source) {
        return viewQuestionBankDetailsUseCase.execute(new ViewQuestionBankDetailsQuery(source.questionBankId()));
    }

    @SchemaMapping(typeName = "Question", field = "assets")
    public List<QuestionAssetDto> assets(QuestionDto source) {
        return QuestionAssetDtoMapper.toDtoList(questionAssetRepository.findByQuestionId(source.id()));
    }

    @SchemaMapping(typeName = "Question", field = "evaluationGuide")
    public QuestionEvaluationGuideDto evaluationGuide(QuestionDto source) {
        return questionEvaluationGuideRepository.findByQuestionId(source.id())
            .map(QuestionEvaluationGuideDtoMapper::toDto)
            .orElse(null);
    }

    @SchemaMapping(typeName = "Question", field = "collaborators")
    public List<QuestionCollaboratorDto> collaborators(QuestionDto source) {
        return QuestionCollaboratorDtoMapper.toDtoList(questionCollaboratorRepository.findByQuestionId(source.id()));
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

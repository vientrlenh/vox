package com.sep.vox.interfaces.graphql.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewAdminQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewAdminReviewQueueQuery;
import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolReviewQueueQuery;
import com.sep.vox.application.port.input.query.ViewTeacherMyQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewTeacherQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewTeacherReviewQueueQuery;
import com.sep.vox.application.port.input.usecase.question.ViewAdminQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewAdminReviewQueueUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionEvaluationGuideUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionTopicByQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewSchoolQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewSchoolReviewQueueUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewTeacherMyQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewTeacherQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewTeacherReviewQueueUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Controller("graphqlQuestionController")
public class QuestionController {

    private final ViewQuestionDetailsUseCase viewQuestionDetailsUseCase;
    private final ViewTeacherMyQuestionsUseCase viewTeacherMyQuestionsUseCase;
    private final ViewTeacherQuestionsUseCase viewTeacherQuestionsUseCase;
    private final ViewTeacherReviewQueueUseCase viewTeacherReviewQueueUseCase;
    private final ViewSchoolQuestionsUseCase viewSchoolQuestionsUseCase;
    private final ViewSchoolReviewQueueUseCase viewSchoolReviewQueueUseCase;
    private final ViewAdminQuestionsUseCase viewAdminQuestionsUseCase;
    private final ViewAdminReviewQueueUseCase viewAdminReviewQueueUseCase;
    private final ViewQuestionTopicByQuestionUseCase viewQuestionTopicByQuestionUseCase;
    private final ViewQuestionAssetsUseCase viewQuestionAssetsUseCase;
    private final ViewQuestionEvaluationGuideUseCase viewQuestionEvaluationGuideUseCase;

    public QuestionController(
            ViewQuestionDetailsUseCase viewQuestionDetailsUseCase,
            ViewTeacherMyQuestionsUseCase viewTeacherMyQuestionsUseCase,
            ViewTeacherQuestionsUseCase viewTeacherQuestionsUseCase,
            ViewTeacherReviewQueueUseCase viewTeacherReviewQueueUseCase,
            ViewSchoolQuestionsUseCase viewSchoolQuestionsUseCase,
            ViewSchoolReviewQueueUseCase viewSchoolReviewQueueUseCase,
            ViewAdminQuestionsUseCase viewAdminQuestionsUseCase,
            ViewAdminReviewQueueUseCase viewAdminReviewQueueUseCase,
            ViewQuestionTopicByQuestionUseCase viewQuestionTopicByQuestionUseCase,
            ViewQuestionAssetsUseCase viewQuestionAssetsUseCase,
            ViewQuestionEvaluationGuideUseCase viewQuestionEvaluationGuideUseCase) {
        this.viewQuestionDetailsUseCase = viewQuestionDetailsUseCase;
        this.viewTeacherMyQuestionsUseCase = viewTeacherMyQuestionsUseCase;
        this.viewTeacherQuestionsUseCase = viewTeacherQuestionsUseCase;
        this.viewTeacherReviewQueueUseCase = viewTeacherReviewQueueUseCase;
        this.viewSchoolQuestionsUseCase = viewSchoolQuestionsUseCase;
        this.viewSchoolReviewQueueUseCase = viewSchoolReviewQueueUseCase;
        this.viewAdminQuestionsUseCase = viewAdminQuestionsUseCase;
        this.viewAdminReviewQueueUseCase = viewAdminReviewQueueUseCase;
        this.viewQuestionTopicByQuestionUseCase = viewQuestionTopicByQuestionUseCase;
        this.viewQuestionAssetsUseCase = viewQuestionAssetsUseCase;
        this.viewQuestionEvaluationGuideUseCase = viewQuestionEvaluationGuideUseCase;
    }

    // ==================== QUERIES ====================

    @QueryMapping(name = "question")
    @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')")
    public QuestionDto question(@Argument(name = "id") UUID id) {
        var query = new ViewQuestionDetailsQuery(id);
        return viewQuestionDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "teacherMyQuestions")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<QuestionDto> teacherMyQuestions(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        var query = new ViewTeacherMyQuestionsQuery(page, size);
        return viewTeacherMyQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "teacherQuestions")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<QuestionDto> teacherQuestions(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "scope") String scope,
            @Argument(name = "status") String status,
            @Argument(name = "type") String type,
            @Argument(name = "keyword") String keyword) {
        var query = new ViewTeacherQuestionsQuery(page, size, scope, status, type, keyword);
        return viewTeacherQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "teacherReviewQueue")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<QuestionDto> teacherReviewQueue(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        var query = new ViewTeacherReviewQueueQuery(page, size);
        return viewTeacherReviewQueueUseCase.execute(query);
    }

    @QueryMapping(name = "schoolReviewQueue")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<QuestionDto> schoolReviewQueue(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        var query = new ViewSchoolReviewQueueQuery(page, size);
        return viewSchoolReviewQueueUseCase.execute(query);
    }

    @QueryMapping(name = "schoolQuestions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<QuestionDto> schoolQuestions(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "scope") String scope,
            @Argument(name = "status") String status,
            @Argument(name = "type") String type,
            @Argument(name = "keyword") String keyword) {
        var query = new ViewSchoolQuestionsQuery(page, size, scope, status, type, keyword);
        return viewSchoolQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "adminQuestions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<QuestionDto> adminQuestions(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "includeArchived") Boolean includeArchived,
            @Argument(name = "status") String status,
            @Argument(name = "keyword") String keyword) {
        var query = new ViewAdminQuestionsQuery(page, size, includeArchived, status, keyword);
        return viewAdminQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "adminReviewQueue")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<QuestionDto> adminReviewQueue(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        var query = new ViewAdminReviewQueueQuery(page, size);
        return viewAdminReviewQueueUseCase.execute(query);
    }

    // ==================== SCHEMA MAPPINGS (nested fields on Question) ====================

    @SchemaMapping(typeName = "Question", field = "questionTopic")
    public QuestionTopicDto questionTopic(QuestionDto question) {
        return viewQuestionTopicByQuestionUseCase.execute(question.questionTopicId());
    }

    @SchemaMapping(typeName = "Question", field = "assets")
    public List<QuestionAssetDto> assets(QuestionDto question) {
        return viewQuestionAssetsUseCase.execute(question.id());
    }

    @SchemaMapping(typeName = "Question", field = "evaluationGuide")
    public QuestionEvaluationGuideDto evaluationGuide(QuestionDto question) {
        return viewQuestionEvaluationGuideUseCase.execute(question.id()).orElse(null);
    }

}

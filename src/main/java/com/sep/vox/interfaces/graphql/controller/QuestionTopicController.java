package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewAdminBankTopicsQuery;
import com.sep.vox.application.port.input.query.ViewAdminTopicQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolBankTopicsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolTopicQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewTeacherBankTopicsQuery;
import com.sep.vox.application.port.input.query.ViewTeacherTopicQuestionsQuery;
import com.sep.vox.application.port.input.usecase.question.ViewAdminBankTopicsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewAdminTopicQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewSchoolBankTopicsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewSchoolTopicQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewTeacherBankTopicsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewTeacherTopicQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewAdminQuestionTopicDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewSchoolQuestionTopicDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewTeacherQuestionTopicDetailsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Controller("graphqlQuestionTopicController")
public class QuestionTopicController {

    private final ViewTeacherBankTopicsUseCase viewTeacherBankTopicsUseCase;
    private final ViewSchoolBankTopicsUseCase viewSchoolBankTopicsUseCase;
    private final ViewAdminBankTopicsUseCase viewAdminBankTopicsUseCase;
    private final ViewTeacherTopicQuestionsUseCase viewTeacherTopicQuestionsUseCase;
    private final ViewSchoolTopicQuestionsUseCase viewSchoolTopicQuestionsUseCase;
    private final ViewTeacherQuestionTopicDetailsUseCase viewTeacherQuestionTopicDetailsUseCase;
    private final ViewSchoolQuestionTopicDetailsUseCase viewSchoolQuestionTopicDetailsUseCase;
    private final ViewAdminQuestionTopicDetailsUseCase viewAdminQuestionTopicDetailsUseCase;
    private final ViewAdminTopicQuestionsUseCase viewAdminTopicQuestionsUseCase;

    public QuestionTopicController(
            ViewTeacherBankTopicsUseCase viewTeacherBankTopicsUseCase,
            ViewSchoolBankTopicsUseCase viewSchoolBankTopicsUseCase,
            ViewAdminBankTopicsUseCase viewAdminBankTopicsUseCase,
            ViewTeacherTopicQuestionsUseCase viewTeacherTopicQuestionsUseCase,
            ViewSchoolTopicQuestionsUseCase viewSchoolTopicQuestionsUseCase,
            ViewTeacherQuestionTopicDetailsUseCase viewTeacherQuestionTopicDetailsUseCase,
            ViewSchoolQuestionTopicDetailsUseCase viewSchoolQuestionTopicDetailsUseCase,
            ViewAdminQuestionTopicDetailsUseCase viewAdminQuestionTopicDetailsUseCase,
            ViewAdminTopicQuestionsUseCase viewAdminTopicQuestionsUseCase) {
        this.viewTeacherBankTopicsUseCase = viewTeacherBankTopicsUseCase;
        this.viewSchoolBankTopicsUseCase = viewSchoolBankTopicsUseCase;
        this.viewAdminBankTopicsUseCase = viewAdminBankTopicsUseCase;
        this.viewTeacherTopicQuestionsUseCase = viewTeacherTopicQuestionsUseCase;
        this.viewSchoolTopicQuestionsUseCase = viewSchoolTopicQuestionsUseCase;
        this.viewTeacherQuestionTopicDetailsUseCase = viewTeacherQuestionTopicDetailsUseCase;
        this.viewSchoolQuestionTopicDetailsUseCase = viewSchoolQuestionTopicDetailsUseCase;
        this.viewAdminQuestionTopicDetailsUseCase = viewAdminQuestionTopicDetailsUseCase;
        this.viewAdminTopicQuestionsUseCase = viewAdminTopicQuestionsUseCase;
    }

    @QueryMapping(name = "teacherBankTopics")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<QuestionTopicDto> teacherBankTopics(
            @Argument(name = "bankId") UUID bankId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        var query = new ViewTeacherBankTopicsQuery(bankId, page, size);
        return viewTeacherBankTopicsUseCase.execute(query);
    }

    @QueryMapping(name = "teacherQuestionTopic")
    @PreAuthorize("hasRole('TEACHER')")
    public QuestionTopicDto teacherQuestionTopic(@Argument(name = "id") UUID id) {
        return viewTeacherQuestionTopicDetailsUseCase.execute(new ViewQuestionTopicDetailsQuery(id));
    }

    @QueryMapping(name = "schoolBankTopics")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<QuestionTopicDto> schoolBankTopics(
            @Argument(name = "bankId") UUID bankId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        var query = new ViewSchoolBankTopicsQuery(bankId, page, size);
        return viewSchoolBankTopicsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolQuestionTopic")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public QuestionTopicDto schoolQuestionTopic(@Argument(name = "id") UUID id) {
        return viewSchoolQuestionTopicDetailsUseCase.execute(new ViewQuestionTopicDetailsQuery(id));
    }

    @QueryMapping(name = "adminBankTopics")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<QuestionTopicDto> adminBankTopics(
            @Argument(name = "bankId") UUID bankId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "includeArchived") Boolean includeArchived) {
        var query = new ViewAdminBankTopicsQuery(bankId, page, size, includeArchived);
        return viewAdminBankTopicsUseCase.execute(query);
    }

    @QueryMapping(name = "adminQuestionTopic")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public QuestionTopicDto adminQuestionTopic(@Argument(name = "id") UUID id) {
        return viewAdminQuestionTopicDetailsUseCase.execute(new ViewQuestionTopicDetailsQuery(id));
    }

    @QueryMapping(name = "teacherTopicQuestions")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<QuestionDto> teacherTopicQuestions(
            @Argument(name = "bankId") UUID bankId,
            @Argument(name = "topicId") UUID topicId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "scope") String scope,
            @Argument(name = "status") String status,
            @Argument(name = "type") String type,
            @Argument(name = "keyword") String keyword) {
        var query = new ViewTeacherTopicQuestionsQuery(bankId, topicId, page, size, scope, status, type, keyword);
        return viewTeacherTopicQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "schoolTopicQuestions")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<QuestionDto> schoolTopicQuestions(
            @Argument(name = "bankId") UUID bankId,
            @Argument(name = "topicId") UUID topicId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "scope") String scope,
            @Argument(name = "status") String status,
            @Argument(name = "type") String type,
            @Argument(name = "keyword") String keyword) {
        var query = new ViewSchoolTopicQuestionsQuery(bankId, topicId, page, size, scope, status, type, keyword);
        return viewSchoolTopicQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "adminTopicQuestions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<QuestionDto> adminTopicQuestions(
            @Argument(name = "bankId") UUID bankId,
            @Argument(name = "topicId") UUID topicId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "includeArchived") Boolean includeArchived,
            @Argument(name = "scope") String scope,
            @Argument(name = "status") String status,
            @Argument(name = "type") String type,
            @Argument(name = "keyword") String keyword) {
        var query = new ViewAdminTopicQuestionsQuery(bankId, topicId, page, size, includeArchived, scope, status, type, keyword);
        return viewAdminTopicQuestionsUseCase.execute(query);
    }

}

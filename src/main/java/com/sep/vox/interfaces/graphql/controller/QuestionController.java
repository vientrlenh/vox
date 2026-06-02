package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;


import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionsByTopicQuery;
import com.sep.vox.application.port.input.query.ViewQuestionsQuery;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsByTopicUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicDetailsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Controller("graphqlQuestionController")
public class QuestionController {

    private final ViewQuestionsUseCase viewQuestionsUseCase;
    private final ViewQuestionDetailsUseCase viewQuestionDetailsUseCase;
    private final ViewQuestionsByTopicUseCase viewQuestionsByTopicUseCase;
    private final ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase;

    public QuestionController(
            ViewQuestionsUseCase viewQuestionsUseCase,
            ViewQuestionDetailsUseCase viewQuestionDetailsUseCase,
            ViewQuestionsByTopicUseCase viewQuestionsByTopicUseCase,
            ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase) {
        this.viewQuestionsUseCase = viewQuestionsUseCase;
        this.viewQuestionDetailsUseCase = viewQuestionDetailsUseCase;
        this.viewQuestionsByTopicUseCase = viewQuestionsByTopicUseCase;
        this.viewQuestionTopicDetailsUseCase = viewQuestionTopicDetailsUseCase;
    }

    @QueryMapping(name = "questions")
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public PageResult<QuestionDto> questions(@Argument(name = "page") int page, @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        var query = new ViewQuestionsQuery(page, size);
        return viewQuestionsUseCase.execute(query);
    }

    @QueryMapping(name = "questionsByTopic")
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public PageResult<QuestionDto> questionsByTopic(
            @Argument(name = "topicId") UUID topicId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        var query = new ViewQuestionsByTopicQuery(topicId, page, size);
        return viewQuestionsByTopicUseCase.execute(query);
    }

    @QueryMapping(name = "question")
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public QuestionDto question(@Argument(name = "id") UUID id) {
        var query = new ViewQuestionDetailsQuery(id);
        return viewQuestionDetailsUseCase.execute(query);
    }

    @SchemaMapping(typeName = "Question", field = "topic")
    public QuestionTopicDto topic(QuestionDto question) {
        var query = new ViewQuestionTopicDetailsQuery(question.topicId());
        return viewQuestionTopicDetailsUseCase.execute(query);
    }
}

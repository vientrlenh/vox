package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewQuestionTopicDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionTopicsQuery;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionTopicDto;

@Controller("graphqlQuestionTopicController")
public class QuestionTopicController {

    private final ViewQuestionTopicsUseCase viewQuestionTopicsUseCase;
    private final ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase;
    private final ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase;

    public QuestionTopicController(
            ViewQuestionTopicsUseCase viewQuestionTopicsUseCase,
            ViewQuestionTopicDetailsUseCase viewQuestionTopicDetailsUseCase,
            ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase) {
        this.viewQuestionTopicsUseCase = viewQuestionTopicsUseCase;
        this.viewQuestionTopicDetailsUseCase = viewQuestionTopicDetailsUseCase;
        this.viewQuestionBankDetailsUseCase = viewQuestionBankDetailsUseCase;
    }

}

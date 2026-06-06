package com.sep.vox.interfaces.graphql.controller;

import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicsUseCase;

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

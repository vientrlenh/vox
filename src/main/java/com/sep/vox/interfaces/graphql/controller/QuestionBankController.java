package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewAdminBankQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.question.ViewAdminBankQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBanksUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionDto;

@Controller("graphqlQuestionBankController")
public class QuestionBankController {

    private final ViewQuestionBanksUseCase viewQuestionBanksUseCase;
    private final ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase;
    private final ViewAdminBankQuestionsUseCase viewAdminBankQuestionsUseCase;

    public QuestionBankController(
            ViewQuestionBanksUseCase viewQuestionBanksUseCase,
            ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase,
            ViewAdminBankQuestionsUseCase viewAdminBankQuestionsUseCase) {
        this.viewQuestionBanksUseCase = viewQuestionBanksUseCase;
        this.viewQuestionBankDetailsUseCase = viewQuestionBankDetailsUseCase;
        this.viewAdminBankQuestionsUseCase = viewAdminBankQuestionsUseCase;
    }

    @QueryMapping(name = "questionBanks")
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public PageResult<QuestionBankDto> questionBanks(@Argument(name = "page") int page, @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        var query = new ViewQuestionBanksQuery(page, size);
        return viewQuestionBanksUseCase.execute(query);
    }

    @QueryMapping(name = "questionBank")
    // @PreAuthorize("hasAnyRole('TEACHER', 'SCHOOL_ADMIN')")
    public QuestionBankDto questionBank(@Argument(name = "id") UUID id) {
        var query = new ViewQuestionBankDetailsQuery(id);
        return viewQuestionBankDetailsUseCase.execute(query);
    }

    @QueryMapping(name = "adminBankQuestions")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<QuestionDto> adminBankQuestions(
            @Argument(name = "bankId") UUID bankId,
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "includeArchived") Boolean includeArchived,
            @Argument(name = "status") String status,
            @Argument(name = "keyword") String keyword) {
        var query = new ViewAdminBankQuestionsQuery(bankId, page, size, includeArchived, status, keyword);
        return viewAdminBankQuestionsUseCase.execute(query);
    }
}

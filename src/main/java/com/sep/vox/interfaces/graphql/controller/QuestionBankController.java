package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBanksUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;

@Controller("graphqlQuestionBankController")
public class QuestionBankController {

    private final ViewQuestionBanksUseCase viewQuestionBanksUseCase;
    private final ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase;

    public QuestionBankController(
            ViewQuestionBanksUseCase viewQuestionBanksUseCase,
            ViewQuestionBankDetailsUseCase viewQuestionBankDetailsUseCase) {
        this.viewQuestionBanksUseCase = viewQuestionBanksUseCase;
        this.viewQuestionBankDetailsUseCase = viewQuestionBankDetailsUseCase;
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
}

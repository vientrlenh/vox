package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewAdminBankQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolQuestionBanksQuery;
import com.sep.vox.application.port.input.query.ViewTeacherQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.question.ViewAdminBankQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewAdminQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewSchoolQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewSchoolQuestionBanksUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewTeacherQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewTeacherQuestionBanksUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionDto;

@Controller("graphqlQuestionBankController")
public class QuestionBankController {

    private final ViewTeacherQuestionBanksUseCase viewTeacherQuestionBanksUseCase;
    private final ViewTeacherQuestionBankDetailsUseCase viewTeacherQuestionBankDetailsUseCase;
    private final ViewSchoolQuestionBanksUseCase viewSchoolQuestionBanksUseCase;
    private final ViewSchoolQuestionBankDetailsUseCase viewSchoolQuestionBankDetailsUseCase;
    private final ViewAdminQuestionBankDetailsUseCase viewAdminQuestionBankDetailsUseCase;
    private final ViewAdminBankQuestionsUseCase viewAdminBankQuestionsUseCase;

    public QuestionBankController(
            ViewTeacherQuestionBanksUseCase viewTeacherQuestionBanksUseCase,
            ViewTeacherQuestionBankDetailsUseCase viewTeacherQuestionBankDetailsUseCase,
            ViewSchoolQuestionBanksUseCase viewSchoolQuestionBanksUseCase,
            ViewSchoolQuestionBankDetailsUseCase viewSchoolQuestionBankDetailsUseCase,
            ViewAdminQuestionBankDetailsUseCase viewAdminQuestionBankDetailsUseCase,
            ViewAdminBankQuestionsUseCase viewAdminBankQuestionsUseCase) {
        this.viewTeacherQuestionBanksUseCase = viewTeacherQuestionBanksUseCase;
        this.viewTeacherQuestionBankDetailsUseCase = viewTeacherQuestionBankDetailsUseCase;
        this.viewSchoolQuestionBanksUseCase = viewSchoolQuestionBanksUseCase;
        this.viewSchoolQuestionBankDetailsUseCase = viewSchoolQuestionBankDetailsUseCase;
        this.viewAdminQuestionBankDetailsUseCase = viewAdminQuestionBankDetailsUseCase;
        this.viewAdminBankQuestionsUseCase = viewAdminBankQuestionsUseCase;
    }

    @QueryMapping(name = "teacherQuestionBanks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<QuestionBankDto> teacherQuestionBanks(
            @Argument(name = "page") int page, @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        return viewTeacherQuestionBanksUseCase.execute(new ViewTeacherQuestionBanksQuery(page, size));
    }

    @QueryMapping(name = "teacherQuestionBank")
    @PreAuthorize("hasRole('TEACHER')")
    public QuestionBankDto teacherQuestionBank(@Argument(name = "id") UUID id) {
        return viewTeacherQuestionBankDetailsUseCase.execute(new ViewQuestionBankDetailsQuery(id));
    }

    @QueryMapping(name = "schoolQuestionBanks")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public PageResult<QuestionBankDto> schoolQuestionBanks(
            @Argument(name = "page") int page, @Argument(name = "size") int size) {
        if (page <= 0 || size <= 0) {
            throw new IllegalStateException("Số trang hoặc kích thước trang yêu cầu không hợp lệ");
        }
        return viewSchoolQuestionBanksUseCase.execute(new ViewSchoolQuestionBanksQuery(page, size));
    }

    @QueryMapping(name = "schoolQuestionBank")
    @PreAuthorize("hasRole('SCHOOL_ADMIN')")
    public QuestionBankDto schoolQuestionBank(@Argument(name = "id") UUID id) {
        return viewSchoolQuestionBankDetailsUseCase.execute(new ViewQuestionBankDetailsQuery(id));
    }

    @QueryMapping(name = "adminQuestionBank")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public QuestionBankDto adminQuestionBank(@Argument(name = "id") UUID id) {
        return viewAdminQuestionBankDetailsUseCase.execute(new ViewQuestionBankDetailsQuery(id));
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
        return viewAdminBankQuestionsUseCase.execute(
            new ViewAdminBankQuestionsQuery(bankId, page, size, includeArchived, status, keyword));
    }
}

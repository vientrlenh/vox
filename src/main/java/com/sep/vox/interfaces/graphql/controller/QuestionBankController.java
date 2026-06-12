package com.sep.vox.interfaces.graphql.controller;

import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewAdminQuestionBanksQuery;
import com.sep.vox.application.port.input.query.ViewAdminSchoolQuestionBanksQuery;
import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolQuestionBanksQuery;
import com.sep.vox.application.port.input.query.ViewTeacherQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.questionbank.ViewAdminQuestionBanksUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewAdminSchoolQuestionBanksUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewAdminQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewSchoolQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewSchoolQuestionBanksUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewTeacherQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewTeacherQuestionBanksUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;

@Controller("graphqlQuestionBankController")
public class QuestionBankController {

    private final ViewTeacherQuestionBanksUseCase viewTeacherQuestionBanksUseCase;
    private final ViewTeacherQuestionBankDetailsUseCase viewTeacherQuestionBankDetailsUseCase;
    private final ViewSchoolQuestionBanksUseCase viewSchoolQuestionBanksUseCase;
    private final ViewSchoolQuestionBankDetailsUseCase viewSchoolQuestionBankDetailsUseCase;
    private final ViewAdminQuestionBankDetailsUseCase viewAdminQuestionBankDetailsUseCase;
    private final ViewAdminQuestionBanksUseCase viewAdminQuestionBanksUseCase;
    private final ViewAdminSchoolQuestionBanksUseCase viewAdminSchoolQuestionBanksUseCase;

    public QuestionBankController(
            ViewTeacherQuestionBanksUseCase viewTeacherQuestionBanksUseCase,
            ViewTeacherQuestionBankDetailsUseCase viewTeacherQuestionBankDetailsUseCase,
            ViewSchoolQuestionBanksUseCase viewSchoolQuestionBanksUseCase,
            ViewSchoolQuestionBankDetailsUseCase viewSchoolQuestionBankDetailsUseCase,
            ViewAdminQuestionBankDetailsUseCase viewAdminQuestionBankDetailsUseCase,
            ViewAdminQuestionBanksUseCase viewAdminQuestionBanksUseCase,
            ViewAdminSchoolQuestionBanksUseCase viewAdminSchoolQuestionBanksUseCase) {
        this.viewTeacherQuestionBanksUseCase = viewTeacherQuestionBanksUseCase;
        this.viewTeacherQuestionBankDetailsUseCase = viewTeacherQuestionBankDetailsUseCase;
        this.viewSchoolQuestionBanksUseCase = viewSchoolQuestionBanksUseCase;
        this.viewSchoolQuestionBankDetailsUseCase = viewSchoolQuestionBankDetailsUseCase;
        this.viewAdminQuestionBankDetailsUseCase = viewAdminQuestionBankDetailsUseCase;
        this.viewAdminQuestionBanksUseCase = viewAdminQuestionBanksUseCase;
        this.viewAdminSchoolQuestionBanksUseCase = viewAdminSchoolQuestionBanksUseCase;
    }

    @QueryMapping(name = "teacherQuestionBanks")
    @PreAuthorize("hasRole('TEACHER')")
    public PageResult<QuestionBankDto> teacherQuestionBanks(
            @Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
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
            @Argument(name = "page") Integer page, @Argument(name = "size") Integer size) {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
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

    @QueryMapping(name = "adminQuestionBanks")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<QuestionBankDto> adminQuestionBanks(
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
        return viewAdminQuestionBanksUseCase.execute(new ViewAdminQuestionBanksQuery(page, size));
    }

    @QueryMapping(name = "adminSchoolQuestionBanks")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public PageResult<QuestionBankDto> adminSchoolQuestionBanks(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        page = page == null ? 1 : page;
        size = size == null ? 20 : size;
        return viewAdminSchoolQuestionBanksUseCase.execute(new ViewAdminSchoolQuestionBanksQuery(schoolId, page, size));
    }

    @QueryMapping(name = "adminQuestionBank")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public QuestionBankDto adminQuestionBank(@Argument(name = "id") UUID id) {
        return viewAdminQuestionBankDetailsUseCase.execute(new ViewQuestionBankDetailsQuery(id));
    }
}

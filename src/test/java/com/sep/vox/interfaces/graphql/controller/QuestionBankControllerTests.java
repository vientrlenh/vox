package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sep.vox.application.port.input.query.ViewAdminQuestionBanksQuery;
import com.sep.vox.application.port.input.query.ViewAdminSchoolQuestionBanksQuery;
import com.sep.vox.application.port.input.query.ViewQuestionBankDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolQuestionBanksQuery;
import com.sep.vox.application.port.input.query.ViewTeacherQuestionBanksQuery;
import com.sep.vox.application.port.input.usecase.questionbank.ViewAdminQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewAdminQuestionBanksUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewAdminSchoolQuestionBanksUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewSchoolQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewSchoolQuestionBanksUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewTeacherQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewTeacherQuestionBanksUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;

class QuestionBankControllerTests {

    @Test
    void teacher_question_banks_should_return_page_result() {
        var teacherBanksUseCase = mock(ViewTeacherQuestionBanksUseCase.class);
        var controller = controller(teacherBanksUseCase);
        var expected = new PageResult<QuestionBankDto>(List.of(), 1, 20, 0, 0);
        var query = new ViewTeacherQuestionBanksQuery(1, 20);
        when(teacherBanksUseCase.execute(query)).thenReturn(expected);

        var result = controller.teacherQuestionBanks(1, 20);

        assertThat(result).isEqualTo(expected);
        verify(teacherBanksUseCase).execute(query);
    }

    @Test
    void teacher_question_banks_should_throw_when_page_or_size_invalid() {
        var controller = controller(mock(ViewTeacherQuestionBanksUseCase.class));

        assertThrows(IllegalStateException.class, () -> controller.teacherQuestionBanks(0, 20));
        assertThrows(IllegalStateException.class, () -> controller.teacherQuestionBanks(1, 0));
    }

    @Test
    void teacher_question_bank_should_return_detail() {
        var detailsUseCase = mock(ViewTeacherQuestionBankDetailsUseCase.class);
        var controller = controller(mock(ViewTeacherQuestionBanksUseCase.class), detailsUseCase);
        var bankId = UUID.randomUUID();
        var expected = questionBankDto(bankId, "TEACHER_BANK");
        when(detailsUseCase.execute(new ViewQuestionBankDetailsQuery(bankId))).thenReturn(expected);

        var result = controller.teacherQuestionBank(bankId);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewQuestionBankDetailsQuery(bankId));
    }

    @Test
    void school_question_banks_should_return_page_result() {
        var schoolBanksUseCase = mock(ViewSchoolQuestionBanksUseCase.class);
        var controller = controller(
            mock(ViewTeacherQuestionBanksUseCase.class),
            mock(ViewTeacherQuestionBankDetailsUseCase.class),
            schoolBanksUseCase,
            mock(ViewSchoolQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBanksUseCase.class),
            mock(ViewAdminSchoolQuestionBanksUseCase.class)
        );
        var expected = new PageResult<QuestionBankDto>(List.of(), 2, 10, 0, 0);
        var query = new ViewSchoolQuestionBanksQuery(2, 10);
        when(schoolBanksUseCase.execute(query)).thenReturn(expected);

        var result = controller.schoolQuestionBanks(2, 10);

        assertThat(result).isEqualTo(expected);
        verify(schoolBanksUseCase).execute(query);
    }

    @Test
    void school_question_bank_should_return_detail() {
        var detailsUseCase = mock(ViewSchoolQuestionBankDetailsUseCase.class);
        var controller = controller(
            mock(ViewTeacherQuestionBanksUseCase.class),
            mock(ViewTeacherQuestionBankDetailsUseCase.class),
            mock(ViewSchoolQuestionBanksUseCase.class),
            detailsUseCase,
            mock(ViewAdminQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBanksUseCase.class),
            mock(ViewAdminSchoolQuestionBanksUseCase.class)
        );
        var bankId = UUID.randomUUID();
        var expected = questionBankDto(bankId, "SCHOOL_BANK");
        when(detailsUseCase.execute(new ViewQuestionBankDetailsQuery(bankId))).thenReturn(expected);

        var result = controller.schoolQuestionBank(bankId);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewQuestionBankDetailsQuery(bankId));
    }

    @Test
    void admin_question_banks_should_return_page_result() {
        var adminBanksUseCase = mock(ViewAdminQuestionBanksUseCase.class);
        var controller = controller(
            mock(ViewTeacherQuestionBanksUseCase.class),
            mock(ViewTeacherQuestionBankDetailsUseCase.class),
            mock(ViewSchoolQuestionBanksUseCase.class),
            mock(ViewSchoolQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBankDetailsUseCase.class),
            adminBanksUseCase,
            mock(ViewAdminSchoolQuestionBanksUseCase.class)
        );
        var expected = new PageResult<QuestionBankDto>(List.of(), 1, 50, 0, 0);
        var query = new ViewAdminQuestionBanksQuery(1, 50);
        when(adminBanksUseCase.execute(query)).thenReturn(expected);

        var result = controller.adminQuestionBanks(1, 50);

        assertThat(result).isEqualTo(expected);
        verify(adminBanksUseCase).execute(query);
    }

    @Test
    void admin_school_question_banks_should_return_page_result() {
        var adminSchoolBanksUseCase = mock(ViewAdminSchoolQuestionBanksUseCase.class);
        var controller = controller(
            mock(ViewTeacherQuestionBanksUseCase.class),
            mock(ViewTeacherQuestionBankDetailsUseCase.class),
            mock(ViewSchoolQuestionBanksUseCase.class),
            mock(ViewSchoolQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBanksUseCase.class),
            adminSchoolBanksUseCase
        );
        var schoolId = UUID.randomUUID();
        var expected = new PageResult<QuestionBankDto>(List.of(), 3, 15, 0, 0);
        var query = new ViewAdminSchoolQuestionBanksQuery(schoolId, 3, 15);
        when(adminSchoolBanksUseCase.execute(query)).thenReturn(expected);

        var result = controller.adminSchoolQuestionBanks(schoolId, 3, 15);

        assertThat(result).isEqualTo(expected);
        verify(adminSchoolBanksUseCase).execute(query);
    }

    @Test
    void admin_question_bank_should_return_detail() {
        var detailsUseCase = mock(ViewAdminQuestionBankDetailsUseCase.class);
        var controller = controller(
            mock(ViewTeacherQuestionBanksUseCase.class),
            mock(ViewTeacherQuestionBankDetailsUseCase.class),
            mock(ViewSchoolQuestionBanksUseCase.class),
            mock(ViewSchoolQuestionBankDetailsUseCase.class),
            detailsUseCase,
            mock(ViewAdminQuestionBanksUseCase.class),
            mock(ViewAdminSchoolQuestionBanksUseCase.class)
        );
        var bankId = UUID.randomUUID();
        var expected = questionBankDto(bankId, "ADMIN_BANK");
        when(detailsUseCase.execute(new ViewQuestionBankDetailsQuery(bankId))).thenReturn(expected);

        var result = controller.adminQuestionBank(bankId);

        assertThat(result).isEqualTo(expected);
        verify(detailsUseCase).execute(new ViewQuestionBankDetailsQuery(bankId));
    }

    @Test
    void get_queries_should_use_expected_roles() throws Exception {
        assertRole("teacherQuestionBanks", "hasRole('TEACHER')", Integer.class, Integer.class);
        assertRole("teacherQuestionBank", "hasRole('TEACHER')", UUID.class);
        assertRole("schoolQuestionBanks", "hasRole('SCHOOL_ADMIN')", Integer.class, Integer.class);
        assertRole("schoolQuestionBank", "hasRole('SCHOOL_ADMIN')", UUID.class);
        assertRole("adminQuestionBanks", "hasRole('SYSTEM_ADMIN')", Integer.class, Integer.class);
        assertRole("adminSchoolQuestionBanks", "hasRole('SYSTEM_ADMIN')", UUID.class, Integer.class, Integer.class);
        assertRole("adminQuestionBank", "hasRole('SYSTEM_ADMIN')", UUID.class);
    }

    private void assertRole(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = QuestionBankController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expected);
    }

    private QuestionBankController controller(ViewTeacherQuestionBanksUseCase teacherBanksUseCase) {
        return controller(
            teacherBanksUseCase,
            mock(ViewTeacherQuestionBankDetailsUseCase.class),
            mock(ViewSchoolQuestionBanksUseCase.class),
            mock(ViewSchoolQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBanksUseCase.class),
            mock(ViewAdminSchoolQuestionBanksUseCase.class)
        );
    }

    private QuestionBankController controller(
            ViewTeacherQuestionBanksUseCase teacherBanksUseCase,
            ViewTeacherQuestionBankDetailsUseCase teacherDetailsUseCase) {
        return controller(
            teacherBanksUseCase,
            teacherDetailsUseCase,
            mock(ViewSchoolQuestionBanksUseCase.class),
            mock(ViewSchoolQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBankDetailsUseCase.class),
            mock(ViewAdminQuestionBanksUseCase.class),
            mock(ViewAdminSchoolQuestionBanksUseCase.class)
        );
    }

    private QuestionBankController controller(
            ViewTeacherQuestionBanksUseCase teacherBanksUseCase,
            ViewTeacherQuestionBankDetailsUseCase teacherDetailsUseCase,
            ViewSchoolQuestionBanksUseCase schoolBanksUseCase,
            ViewSchoolQuestionBankDetailsUseCase schoolDetailsUseCase,
            ViewAdminQuestionBankDetailsUseCase adminDetailsUseCase,
            ViewAdminQuestionBanksUseCase adminBanksUseCase,
            ViewAdminSchoolQuestionBanksUseCase adminSchoolBanksUseCase) {
        return new QuestionBankController(
            teacherBanksUseCase,
            teacherDetailsUseCase,
            schoolBanksUseCase,
            schoolDetailsUseCase,
            adminDetailsUseCase,
            adminBanksUseCase,
            adminSchoolBanksUseCase
        );
    }

    private QuestionBankDto questionBankDto(UUID id, String code) {
        return new QuestionBankDto(
            id,
            UUID.randomUUID(),
            code,
            code,
            code,
            true,
            "2026-06-14T10:00:00Z",
            "2026-06-14T10:00:00Z"
        );
    }
}

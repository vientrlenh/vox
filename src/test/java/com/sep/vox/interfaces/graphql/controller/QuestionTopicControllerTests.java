package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

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

class QuestionTopicControllerTests {

    @Test
    void teacher_bank_topics_should_return_page_result() {
        var useCase = mock(ViewTeacherBankTopicsUseCase.class);
        var controller = controller(useCase);
        var bankId = UUID.randomUUID();
        var query = new ViewTeacherBankTopicsQuery(bankId, 1, 20);
        var expected = new PageResult<QuestionTopicDto>(List.of(), 1, 20, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.teacherBankTopics(bankId, 1, 20);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void teacher_question_topic_should_return_detail() {
        var useCase = mock(ViewTeacherQuestionTopicDetailsUseCase.class);
        var controller = controller(
            mock(ViewTeacherBankTopicsUseCase.class),
            mock(ViewSchoolBankTopicsUseCase.class),
            mock(ViewAdminBankTopicsUseCase.class),
            mock(ViewTeacherTopicQuestionsUseCase.class),
            mock(ViewSchoolTopicQuestionsUseCase.class),
            useCase,
            mock(ViewSchoolQuestionTopicDetailsUseCase.class),
            mock(ViewAdminQuestionTopicDetailsUseCase.class),
            mock(ViewAdminTopicQuestionsUseCase.class)
        );
        var topicId = UUID.randomUUID();
        var expected = questionTopicDto(topicId, "TEACHER_TOPIC");
        when(useCase.execute(new ViewQuestionTopicDetailsQuery(topicId))).thenReturn(expected);

        var result = controller.teacherQuestionTopic(topicId);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(new ViewQuestionTopicDetailsQuery(topicId));
    }

    @Test
    void school_bank_topics_should_return_page_result() {
        var useCase = mock(ViewSchoolBankTopicsUseCase.class);
        var controller = controller(
            mock(ViewTeacherBankTopicsUseCase.class),
            useCase,
            mock(ViewAdminBankTopicsUseCase.class),
            mock(ViewTeacherTopicQuestionsUseCase.class),
            mock(ViewSchoolTopicQuestionsUseCase.class),
            mock(ViewTeacherQuestionTopicDetailsUseCase.class),
            mock(ViewSchoolQuestionTopicDetailsUseCase.class),
            mock(ViewAdminQuestionTopicDetailsUseCase.class),
            mock(ViewAdminTopicQuestionsUseCase.class)
        );
        var bankId = UUID.randomUUID();
        var query = new ViewSchoolBankTopicsQuery(bankId, 2, 15);
        var expected = new PageResult<QuestionTopicDto>(List.of(), 2, 15, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.schoolBankTopics(bankId, 2, 15);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void school_question_topic_should_return_detail() {
        var useCase = mock(ViewSchoolQuestionTopicDetailsUseCase.class);
        var controller = controller(
            mock(ViewTeacherBankTopicsUseCase.class),
            mock(ViewSchoolBankTopicsUseCase.class),
            mock(ViewAdminBankTopicsUseCase.class),
            mock(ViewTeacherTopicQuestionsUseCase.class),
            mock(ViewSchoolTopicQuestionsUseCase.class),
            mock(ViewTeacherQuestionTopicDetailsUseCase.class),
            useCase,
            mock(ViewAdminQuestionTopicDetailsUseCase.class),
            mock(ViewAdminTopicQuestionsUseCase.class)
        );
        var topicId = UUID.randomUUID();
        var expected = questionTopicDto(topicId, "SCHOOL_TOPIC");
        when(useCase.execute(new ViewQuestionTopicDetailsQuery(topicId))).thenReturn(expected);

        var result = controller.schoolQuestionTopic(topicId);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(new ViewQuestionTopicDetailsQuery(topicId));
    }

    @Test
    void admin_bank_topics_should_return_page_result() {
        var useCase = mock(ViewAdminBankTopicsUseCase.class);
        var controller = controller(
            mock(ViewTeacherBankTopicsUseCase.class),
            mock(ViewSchoolBankTopicsUseCase.class),
            useCase,
            mock(ViewTeacherTopicQuestionsUseCase.class),
            mock(ViewSchoolTopicQuestionsUseCase.class),
            mock(ViewTeacherQuestionTopicDetailsUseCase.class),
            mock(ViewSchoolQuestionTopicDetailsUseCase.class),
            mock(ViewAdminQuestionTopicDetailsUseCase.class),
            mock(ViewAdminTopicQuestionsUseCase.class)
        );
        var bankId = UUID.randomUUID();
        var query = new ViewAdminBankTopicsQuery(bankId, 1, 25, true);
        var expected = new PageResult<QuestionTopicDto>(List.of(), 1, 25, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.adminBankTopics(bankId, 1, 25, true);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void admin_question_topic_should_return_detail() {
        var useCase = mock(ViewAdminQuestionTopicDetailsUseCase.class);
        var controller = controller(
            mock(ViewTeacherBankTopicsUseCase.class),
            mock(ViewSchoolBankTopicsUseCase.class),
            mock(ViewAdminBankTopicsUseCase.class),
            mock(ViewTeacherTopicQuestionsUseCase.class),
            mock(ViewSchoolTopicQuestionsUseCase.class),
            mock(ViewTeacherQuestionTopicDetailsUseCase.class),
            mock(ViewSchoolQuestionTopicDetailsUseCase.class),
            useCase,
            mock(ViewAdminTopicQuestionsUseCase.class)
        );
        var topicId = UUID.randomUUID();
        var expected = questionTopicDto(topicId, "ADMIN_TOPIC");
        when(useCase.execute(new ViewQuestionTopicDetailsQuery(topicId))).thenReturn(expected);

        var result = controller.adminQuestionTopic(topicId);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(new ViewQuestionTopicDetailsQuery(topicId));
    }

    @Test
    void teacher_topic_questions_should_return_page_result() {
        var useCase = mock(ViewTeacherTopicQuestionsUseCase.class);
        var controller = controller(
            mock(ViewTeacherBankTopicsUseCase.class),
            mock(ViewSchoolBankTopicsUseCase.class),
            mock(ViewAdminBankTopicsUseCase.class),
            useCase,
            mock(ViewSchoolTopicQuestionsUseCase.class),
            mock(ViewTeacherQuestionTopicDetailsUseCase.class),
            mock(ViewSchoolQuestionTopicDetailsUseCase.class),
            mock(ViewAdminQuestionTopicDetailsUseCase.class),
            mock(ViewAdminTopicQuestionsUseCase.class)
        );
        var bankId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var query = new ViewTeacherTopicQuestionsQuery(bankId, topicId, 1, 20, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "voice");
        var expected = new PageResult<QuestionDto>(List.of(), 1, 20, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.teacherTopicQuestions(bankId, topicId, 1, 20, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "voice");

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void school_topic_questions_should_return_page_result() {
        var useCase = mock(ViewSchoolTopicQuestionsUseCase.class);
        var controller = controller(
            mock(ViewTeacherBankTopicsUseCase.class),
            mock(ViewSchoolBankTopicsUseCase.class),
            mock(ViewAdminBankTopicsUseCase.class),
            mock(ViewTeacherTopicQuestionsUseCase.class),
            useCase,
            mock(ViewTeacherQuestionTopicDetailsUseCase.class),
            mock(ViewSchoolQuestionTopicDetailsUseCase.class),
            mock(ViewAdminQuestionTopicDetailsUseCase.class),
            mock(ViewAdminTopicQuestionsUseCase.class)
        );
        var bankId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var query = new ViewSchoolTopicQuestionsQuery(bankId, topicId, 1, 20, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "voice");
        var expected = new PageResult<QuestionDto>(List.of(), 1, 20, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.schoolTopicQuestions(bankId, topicId, 1, 20, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "voice");

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void admin_topic_questions_should_return_page_result() {
        var useCase = mock(ViewAdminTopicQuestionsUseCase.class);
        var controller = controller(
            mock(ViewTeacherBankTopicsUseCase.class),
            mock(ViewSchoolBankTopicsUseCase.class),
            mock(ViewAdminBankTopicsUseCase.class),
            mock(ViewTeacherTopicQuestionsUseCase.class),
            mock(ViewSchoolTopicQuestionsUseCase.class),
            mock(ViewTeacherQuestionTopicDetailsUseCase.class),
            mock(ViewSchoolQuestionTopicDetailsUseCase.class),
            mock(ViewAdminQuestionTopicDetailsUseCase.class),
            useCase
        );
        var bankId = UUID.randomUUID();
        var topicId = UUID.randomUUID();
        var query = new ViewAdminTopicQuestionsQuery(bankId, topicId, 1, 20, true, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "voice");
        var expected = new PageResult<QuestionDto>(List.of(), 1, 20, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.adminTopicQuestions(bankId, topicId, 1, 20, true, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "voice");

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void get_queries_should_use_expected_roles() throws Exception {
        assertRole("teacherBankTopics", "hasRole('TEACHER')", UUID.class, Integer.class, Integer.class);
        assertRole("teacherQuestionTopic", "hasRole('TEACHER')", UUID.class);
        assertRole("schoolBankTopics", "hasRole('SCHOOL_ADMIN')", UUID.class, Integer.class, Integer.class);
        assertRole("schoolQuestionTopic", "hasRole('SCHOOL_ADMIN')", UUID.class);
        assertRole("adminBankTopics", "hasRole('SYSTEM_ADMIN')", UUID.class, Integer.class, Integer.class, Boolean.class);
        assertRole("adminQuestionTopic", "hasRole('SYSTEM_ADMIN')", UUID.class);
        assertRole("teacherTopicQuestions", "hasRole('TEACHER')", UUID.class, UUID.class, Integer.class, Integer.class, String.class, String.class, String.class, String.class);
        assertRole("schoolTopicQuestions", "hasRole('SCHOOL_ADMIN')", UUID.class, UUID.class, Integer.class, Integer.class, String.class, String.class, String.class, String.class);
        assertRole("adminTopicQuestions", "hasRole('SYSTEM_ADMIN')", UUID.class, UUID.class, Integer.class, Integer.class, Boolean.class, String.class, String.class, String.class, String.class);
    }

    private void assertRole(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = QuestionTopicController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expected);
    }

    private QuestionTopicController controller(ViewTeacherBankTopicsUseCase teacherBankTopicsUseCase) {
        return controller(
            teacherBankTopicsUseCase,
            mock(ViewSchoolBankTopicsUseCase.class),
            mock(ViewAdminBankTopicsUseCase.class),
            mock(ViewTeacherTopicQuestionsUseCase.class),
            mock(ViewSchoolTopicQuestionsUseCase.class),
            mock(ViewTeacherQuestionTopicDetailsUseCase.class),
            mock(ViewSchoolQuestionTopicDetailsUseCase.class),
            mock(ViewAdminQuestionTopicDetailsUseCase.class),
            mock(ViewAdminTopicQuestionsUseCase.class)
        );
    }

    private QuestionTopicController controller(
            ViewTeacherBankTopicsUseCase teacherBankTopicsUseCase,
            ViewSchoolBankTopicsUseCase schoolBankTopicsUseCase,
            ViewAdminBankTopicsUseCase adminBankTopicsUseCase,
            ViewTeacherTopicQuestionsUseCase teacherTopicQuestionsUseCase,
            ViewSchoolTopicQuestionsUseCase schoolTopicQuestionsUseCase,
            ViewTeacherQuestionTopicDetailsUseCase teacherDetailsUseCase,
            ViewSchoolQuestionTopicDetailsUseCase schoolDetailsUseCase,
            ViewAdminQuestionTopicDetailsUseCase adminDetailsUseCase,
            ViewAdminTopicQuestionsUseCase adminTopicQuestionsUseCase) {
        return new QuestionTopicController(
            teacherBankTopicsUseCase,
            schoolBankTopicsUseCase,
            adminBankTopicsUseCase,
            teacherTopicQuestionsUseCase,
            schoolTopicQuestionsUseCase,
            teacherDetailsUseCase,
            schoolDetailsUseCase,
            adminDetailsUseCase,
            adminTopicQuestionsUseCase
        );
    }

    private QuestionTopicDto questionTopicDto(UUID id, String code) {
        return new QuestionTopicDto(
            id,
            UUID.randomUUID(),
            code,
            code,
            code,
            "PUBLISHED",
            "2026-06-14T10:00:00Z",
            "2026-06-14T10:00:00Z"
        );
    }
}

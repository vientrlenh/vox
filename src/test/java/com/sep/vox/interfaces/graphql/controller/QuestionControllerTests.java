package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.sep.vox.application.port.input.query.ViewAdminQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewAdminReviewQueueQuery;
import com.sep.vox.application.port.input.query.ViewQuestionDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolReviewQueueQuery;
import com.sep.vox.application.port.input.query.ViewTeacherMyQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewTeacherQuestionsQuery;
import com.sep.vox.application.port.input.query.ViewTeacherReviewQueueQuery;
import com.sep.vox.application.port.input.usecase.question.ViewAdminQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewAdminReviewQueueUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionAssetsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionEvaluationGuideUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionTopicByQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewSchoolQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewSchoolReviewQueueUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewTeacherMyQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewTeacherQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewTeacherReviewQueueUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionEvaluationGuideDto;
import com.sep.vox.domain.dto.QuestionTopicDto;

class QuestionControllerTests {

    @Test
    void question_should_return_detail() {
        var useCase = mock(ViewQuestionDetailsUseCase.class);
        var controller = controller(useCase);
        var questionId = UUID.randomUUID();
        var expected = questionDto(questionId, "QUESTION_DETAIL");
        when(useCase.execute(new ViewQuestionDetailsQuery(questionId))).thenReturn(expected);

        var result = controller.question(questionId);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(new ViewQuestionDetailsQuery(questionId));
    }

    @Test
    void teacher_my_questions_should_return_page_result() {
        var useCase = mock(ViewTeacherMyQuestionsUseCase.class);
        var controller = controller(
            mock(ViewQuestionDetailsUseCase.class),
            useCase,
            mock(ViewTeacherQuestionsUseCase.class),
            mock(ViewTeacherReviewQueueUseCase.class),
            mock(ViewSchoolQuestionsUseCase.class),
            mock(ViewSchoolReviewQueueUseCase.class),
            mock(ViewAdminQuestionsUseCase.class),
            mock(ViewAdminReviewQueueUseCase.class),
            mock(ViewQuestionTopicByQuestionUseCase.class),
            mock(ViewQuestionAssetsUseCase.class),
            mock(ViewQuestionEvaluationGuideUseCase.class)
        );
        var query = new ViewTeacherMyQuestionsQuery(1, 20);
        var expected = new PageResult<QuestionDto>(List.of(), 1, 20, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.teacherMyQuestions(1, 20);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void teacher_questions_should_return_page_result() {
        var useCase = mock(ViewTeacherQuestionsUseCase.class);
        var controller = controller(
            mock(ViewQuestionDetailsUseCase.class),
            mock(ViewTeacherMyQuestionsUseCase.class),
            useCase,
            mock(ViewTeacherReviewQueueUseCase.class),
            mock(ViewSchoolQuestionsUseCase.class),
            mock(ViewSchoolReviewQueueUseCase.class),
            mock(ViewAdminQuestionsUseCase.class),
            mock(ViewAdminReviewQueueUseCase.class),
            mock(ViewQuestionTopicByQuestionUseCase.class),
            mock(ViewQuestionAssetsUseCase.class),
            mock(ViewQuestionEvaluationGuideUseCase.class)
        );
        var query = new ViewTeacherQuestionsQuery(1, 20, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "fluency");
        var expected = new PageResult<QuestionDto>(List.of(), 1, 20, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.teacherQuestions(1, 20, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "fluency");

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void teacher_review_queue_should_return_page_result() {
        var useCase = mock(ViewTeacherReviewQueueUseCase.class);
        var controller = controller(
            mock(ViewQuestionDetailsUseCase.class),
            mock(ViewTeacherMyQuestionsUseCase.class),
            mock(ViewTeacherQuestionsUseCase.class),
            useCase,
            mock(ViewSchoolQuestionsUseCase.class),
            mock(ViewSchoolReviewQueueUseCase.class),
            mock(ViewAdminQuestionsUseCase.class),
            mock(ViewAdminReviewQueueUseCase.class),
            mock(ViewQuestionTopicByQuestionUseCase.class),
            mock(ViewQuestionAssetsUseCase.class),
            mock(ViewQuestionEvaluationGuideUseCase.class)
        );
        var query = new ViewTeacherReviewQueueQuery(2, 10);
        var expected = new PageResult<QuestionDto>(List.of(), 2, 10, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.teacherReviewQueue(2, 10);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void school_review_queue_should_return_page_result() {
        var useCase = mock(ViewSchoolReviewQueueUseCase.class);
        var controller = controller(
            mock(ViewQuestionDetailsUseCase.class),
            mock(ViewTeacherMyQuestionsUseCase.class),
            mock(ViewTeacherQuestionsUseCase.class),
            mock(ViewTeacherReviewQueueUseCase.class),
            mock(ViewSchoolQuestionsUseCase.class),
            useCase,
            mock(ViewAdminQuestionsUseCase.class),
            mock(ViewAdminReviewQueueUseCase.class),
            mock(ViewQuestionTopicByQuestionUseCase.class),
            mock(ViewQuestionAssetsUseCase.class),
            mock(ViewQuestionEvaluationGuideUseCase.class)
        );
        var query = new ViewSchoolReviewQueueQuery(1, 30);
        var expected = new PageResult<QuestionDto>(List.of(), 1, 30, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.schoolReviewQueue(1, 30);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void school_questions_should_return_page_result() {
        var useCase = mock(ViewSchoolQuestionsUseCase.class);
        var controller = controller(
            mock(ViewQuestionDetailsUseCase.class),
            mock(ViewTeacherMyQuestionsUseCase.class),
            mock(ViewTeacherQuestionsUseCase.class),
            mock(ViewTeacherReviewQueueUseCase.class),
            useCase,
            mock(ViewSchoolReviewQueueUseCase.class),
            mock(ViewAdminQuestionsUseCase.class),
            mock(ViewAdminReviewQueueUseCase.class),
            mock(ViewQuestionTopicByQuestionUseCase.class),
            mock(ViewQuestionAssetsUseCase.class),
            mock(ViewQuestionEvaluationGuideUseCase.class)
        );
        var query = new ViewSchoolQuestionsQuery(1, 20, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "rubric");
        var expected = new PageResult<QuestionDto>(List.of(), 1, 20, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.schoolQuestions(1, 20, "QUESTION_BANK", "PUBLISHED", "SHORT_ANSWER", "rubric");

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void admin_questions_should_return_page_result() {
        var useCase = mock(ViewAdminQuestionsUseCase.class);
        var controller = controller(
            mock(ViewQuestionDetailsUseCase.class),
            mock(ViewTeacherMyQuestionsUseCase.class),
            mock(ViewTeacherQuestionsUseCase.class),
            mock(ViewTeacherReviewQueueUseCase.class),
            mock(ViewSchoolQuestionsUseCase.class),
            mock(ViewSchoolReviewQueueUseCase.class),
            useCase,
            mock(ViewAdminReviewQueueUseCase.class),
            mock(ViewQuestionTopicByQuestionUseCase.class),
            mock(ViewQuestionAssetsUseCase.class),
            mock(ViewQuestionEvaluationGuideUseCase.class)
        );
        var query = new ViewAdminQuestionsQuery(1, 50, true, "PUBLISHED", "topic");
        var expected = new PageResult<QuestionDto>(List.of(), 1, 50, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.adminQuestions(1, 50, true, "PUBLISHED", "topic");

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void admin_review_queue_should_return_page_result() {
        var useCase = mock(ViewAdminReviewQueueUseCase.class);
        var controller = controller(
            mock(ViewQuestionDetailsUseCase.class),
            mock(ViewTeacherMyQuestionsUseCase.class),
            mock(ViewTeacherQuestionsUseCase.class),
            mock(ViewTeacherReviewQueueUseCase.class),
            mock(ViewSchoolQuestionsUseCase.class),
            mock(ViewSchoolReviewQueueUseCase.class),
            mock(ViewAdminQuestionsUseCase.class),
            useCase,
            mock(ViewQuestionTopicByQuestionUseCase.class),
            mock(ViewQuestionAssetsUseCase.class),
            mock(ViewQuestionEvaluationGuideUseCase.class)
        );
        var query = new ViewAdminReviewQueueQuery(1, 25);
        var expected = new PageResult<QuestionDto>(List.of(), 1, 25, 0, 0);
        when(useCase.execute(query)).thenReturn(expected);

        var result = controller.adminReviewQueue(1, 25);

        assertThat(result).isEqualTo(expected);
        verify(useCase).execute(query);
    }

    @Test
    void schema_mappings_should_delegate_related_getters() {
        var topicUseCase = mock(ViewQuestionTopicByQuestionUseCase.class);
        var assetsUseCase = mock(ViewQuestionAssetsUseCase.class);
        var guideUseCase = mock(ViewQuestionEvaluationGuideUseCase.class);
        var controller = controller(
            mock(ViewQuestionDetailsUseCase.class),
            mock(ViewTeacherMyQuestionsUseCase.class),
            mock(ViewTeacherQuestionsUseCase.class),
            mock(ViewTeacherReviewQueueUseCase.class),
            mock(ViewSchoolQuestionsUseCase.class),
            mock(ViewSchoolReviewQueueUseCase.class),
            mock(ViewAdminQuestionsUseCase.class),
            mock(ViewAdminReviewQueueUseCase.class),
            topicUseCase,
            assetsUseCase,
            guideUseCase
        );
        var question = questionDto(UUID.randomUUID(), "QUESTION_SCHEMA");
        var topic = new QuestionTopicDto(question.questionTopicId(), UUID.randomUUID(), "TOPIC", "Topic", null, "PUBLISHED", null, null);
        var assets = List.of(new QuestionAssetDto(UUID.randomUUID(), question.id(), "Image", 10, "Alt", "IMAGE", "https://vox.local/image.jpg", null, null, 1));
        var guide = new QuestionEvaluationGuideDto(UUID.randomUUID(), question.id(), "Expected", "Key points", "Acceptable", "Off topic", "Hints", "Mistakes");
        when(topicUseCase.execute(question.questionTopicId())).thenReturn(topic);
        when(assetsUseCase.execute(question.id())).thenReturn(assets);
        when(guideUseCase.execute(question.id())).thenReturn(Optional.of(guide));

        assertThat(controller.questionTopic(question)).isEqualTo(topic);
        assertThat(controller.assets(question)).isEqualTo(assets);
        assertThat(controller.evaluationGuide(question)).isEqualTo(guide);
        verify(topicUseCase).execute(question.questionTopicId());
        verify(assetsUseCase).execute(question.id());
        verify(guideUseCase).execute(question.id());
    }

    @Test
    void get_queries_should_use_expected_roles() throws Exception {
        assertRole("question", "hasAnyRole('TEACHER', 'SCHOOL_ADMIN', 'SYSTEM_ADMIN')", UUID.class);
        assertRole("teacherMyQuestions", "hasRole('TEACHER')", Integer.class, Integer.class);
        assertRole("teacherQuestions", "hasRole('TEACHER')", Integer.class, Integer.class, String.class, String.class, String.class, String.class);
        assertRole("teacherReviewQueue", "hasRole('TEACHER')", Integer.class, Integer.class);
        assertRole("schoolReviewQueue", "hasRole('SCHOOL_ADMIN')", Integer.class, Integer.class);
        assertRole("schoolQuestions", "hasRole('SCHOOL_ADMIN')", Integer.class, Integer.class, String.class, String.class, String.class, String.class);
        assertRole("adminQuestions", "hasRole('SYSTEM_ADMIN')", Integer.class, Integer.class, Boolean.class, String.class, String.class);
        assertRole("adminReviewQueue", "hasRole('SYSTEM_ADMIN')", Integer.class, Integer.class);
    }

    private void assertRole(String methodName, String expected, Class<?>... parameterTypes) throws Exception {
        Method method = QuestionController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(expected);
    }

    private QuestionController controller(ViewQuestionDetailsUseCase detailsUseCase) {
        return controller(
            detailsUseCase,
            mock(ViewTeacherMyQuestionsUseCase.class),
            mock(ViewTeacherQuestionsUseCase.class),
            mock(ViewTeacherReviewQueueUseCase.class),
            mock(ViewSchoolQuestionsUseCase.class),
            mock(ViewSchoolReviewQueueUseCase.class),
            mock(ViewAdminQuestionsUseCase.class),
            mock(ViewAdminReviewQueueUseCase.class),
            mock(ViewQuestionTopicByQuestionUseCase.class),
            mock(ViewQuestionAssetsUseCase.class),
            mock(ViewQuestionEvaluationGuideUseCase.class)
        );
    }

    private QuestionController controller(
            ViewQuestionDetailsUseCase detailsUseCase,
            ViewTeacherMyQuestionsUseCase teacherMyQuestionsUseCase,
            ViewTeacherQuestionsUseCase teacherQuestionsUseCase,
            ViewTeacherReviewQueueUseCase teacherReviewQueueUseCase,
            ViewSchoolQuestionsUseCase schoolQuestionsUseCase,
            ViewSchoolReviewQueueUseCase schoolReviewQueueUseCase,
            ViewAdminQuestionsUseCase adminQuestionsUseCase,
            ViewAdminReviewQueueUseCase adminReviewQueueUseCase,
            ViewQuestionTopicByQuestionUseCase topicByQuestionUseCase,
            ViewQuestionAssetsUseCase assetsUseCase,
            ViewQuestionEvaluationGuideUseCase guideUseCase) {
        return new QuestionController(
            detailsUseCase,
            teacherMyQuestionsUseCase,
            teacherQuestionsUseCase,
            teacherReviewQueueUseCase,
            schoolQuestionsUseCase,
            schoolReviewQueueUseCase,
            adminQuestionsUseCase,
            adminReviewQueueUseCase,
            topicByQuestionUseCase,
            assetsUseCase,
            guideUseCase
        );
    }

    private QuestionDto questionDto(UUID id, String code) {
        return new QuestionDto(
            id,
            UUID.randomUUID(),
            code,
            "Instruction",
            "Question",
            "Prompt",
            "Preparation",
            "SHORT_ANSWER",
            15,
            30,
            60,
            "QUESTION_BANK",
            "BANK_VISIBLE",
            null,
            false,
            "PUBLISHED",
            "2026-06-14T10:00:00Z",
            "2026-06-14T10:00:00Z"
        );
    }
}

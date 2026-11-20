package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.usecase.question.ViewQuestionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionStatusCountsUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsForAssessmentPaperUseCase;
import com.sep.vox.application.port.input.usecase.question.ViewQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBankDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.ViewQuestionBanksUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicDetailsUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.ViewQuestionTopicsUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends question operations through the real schema and asserts that the audit timestamps are returned
 * as {@code DateTime} values (ISO-8601 UTC).
 */
@GraphQlControllerTest({ QuestionController.class, QuestionBankController.class, QuestionTopicController.class })
@MockitoBean(types = {
    ViewQuestionsForAssessmentPaperUseCase.class, ViewQuestionDetailsUseCase.class,
    ViewQuestionStatusCountsUseCase.class, UserContextPort.class, ViewQuestionBankDetailsUseCase.class,
    ViewQuestionTopicDetailsUseCase.class })
class QuestionGraphQlTests {

    private static final UUID ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-28T02:30:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-29T03:45:10Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewQuestionsUseCase viewQuestionsUseCase;
    @MockitoBean
    private ViewQuestionBanksUseCase viewQuestionBanksUseCase;
    @MockitoBean
    private ViewQuestionTopicsUseCase viewQuestionTopicsUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void questions_should_return_created_and_updated_at_as_date_time() {
        QuestionDto question = new QuestionDto(ID, ID, ID, "Q-01", null, "Question", null, null, "READ_ALOUD", 10, 5, 60,
            "PRIVATE", null, false, "DRAFT", "OPEN", null, CREATED_AT, UPDATED_AT, null, null);
        when(viewQuestionsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(question), 1, 20, 1L, 1));

        graphQlTester.document("{ questions { content { createdAt updatedAt } } }").execute()
            .errors().verify()
            .path("questions.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("questions.content[0].updatedAt").entity(String.class).isEqualTo("2026-07-29T03:45:10Z");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void question_banks_should_return_created_and_updated_at_as_date_time() {
        QuestionBankDto bank = new QuestionBankDto(ID, ID, null, "QB-01", "Bank", null, "SYSTEM", "PUBLISHED",
            CREATED_AT, null, null, null);
        when(viewQuestionBanksUseCase.execute(any())).thenReturn(new PageResult<>(List.of(bank), 1, 20, 1L, 1));

        graphQlTester.document("{ questionBanks { content { createdAt updatedAt } } }").execute()
            .errors().verify()
            .path("questionBanks.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("questionBanks.content[0].updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void question_topics_should_return_created_and_updated_at_as_date_time() {
        QuestionTopicDto topic = new QuestionTopicDto(ID, ID, "T-01", "Topic", null, "PUBLISHED", CREATED_AT, UPDATED_AT, null, null);
        when(viewQuestionTopicsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(topic), 1, 20, 1L, 1));

        graphQlTester.document("{ questionTopics { content { createdAt updatedAt } } }").execute()
            .errors().verify()
            .path("questionTopics.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("questionTopics.content[0].updatedAt").entity(String.class).isEqualTo("2026-07-29T03:45:10Z");
    }
}

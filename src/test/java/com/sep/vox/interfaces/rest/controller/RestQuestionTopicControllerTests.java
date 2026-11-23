package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.usecase.questiontopic.AcceptQuestionTopicImportUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.BulkUpdateQuestionTopicStatusUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.CreateQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.DeleteQuestionTopicUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.PreviewQuestionTopicImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicStatusUseCase;
import com.sep.vox.application.port.input.usecase.questiontopic.UpdateQuestionTopicUseCase;
import com.sep.vox.domain.dto.QuestionTopicDto;
import com.sep.vox.support.RestControllerTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Pins the JSON text that REST clients receive for the temporal and status fields of the shared
 * {@code domain/dto} records returned by {@link RestQuestionTopicController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(RestQuestionTopicController.class)
class RestQuestionTopicControllerTests {

    private static final String CREATED_AT = "2026-07-28T02:30:00Z";
    private static final String UPDATED_AT = "2026-07-29T03:45:10Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateQuestionTopicUseCase createQuestionTopicUseCase;
    @MockitoBean
    private UpdateQuestionTopicUseCase updateQuestionTopicUseCase;
    @MockitoBean
    private UpdateQuestionTopicStatusUseCase updateQuestionTopicStatusUseCase;
    @MockitoBean
    private BulkUpdateQuestionTopicStatusUseCase bulkUpdateQuestionTopicStatusUseCase;
    @MockitoBean
    private DeleteQuestionTopicUseCase deleteQuestionTopicUseCase;
    @MockitoBean
    private PreviewQuestionTopicImportFromFileUseCase previewQuestionTopicImportFromFileUseCase;
    @MockitoBean
    private AcceptQuestionTopicImportUseCase acceptQuestionTopicImportUseCase;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void updateStatus_should_keep_topic_json_text() {
        when(updateQuestionTopicStatusUseCase.execute(any())).thenReturn(
            new QuestionTopicDto(UUID.randomUUID(), UUID.randomUUID(), "T-01", "Topic", "Description", "PUBLISHED", Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID()));

        MvcTestResult result = mvc.patch().uri("/api/v1/question-topics/{id}/status", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\": \"PUBLISH\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "status", "PUBLISHED",
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }
}

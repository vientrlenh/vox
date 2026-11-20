package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.service.QuestionSpreadsheetService;
import com.sep.vox.application.port.input.usecase.question.AcceptQuestionImportUseCase;
import com.sep.vox.application.port.input.usecase.question.BulkUpdateQuestionStatusUseCase;
import com.sep.vox.application.port.input.usecase.question.CloneQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.CreateSystemQuestionBankQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.DeleteQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.GetQuestionAssetUploadUrlUseCase;
import com.sep.vox.application.port.input.usecase.question.PreviewQuestionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.question.RegenerateQuestionAssetAnalysisUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionAssetUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionCollaboratorUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionStatusUseCase;
import com.sep.vox.application.port.input.usecase.question.UpdateQuestionUseCase;
import com.sep.vox.application.port.input.usecase.question.UpsertQuestionEvaluationGuideUseCase;
import com.sep.vox.domain.dto.QuestionAssetDto;
import com.sep.vox.domain.dto.QuestionCollaboratorDto;
import com.sep.vox.domain.dto.QuestionDto;
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
 * {@code domain/dto} records returned by {@link QuestionController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(QuestionController.class)
class QuestionControllerTests {

    private static final String CREATED_AT = "2026-07-28T02:30:00Z";
    private static final String UPDATED_AT = "2026-07-29T03:45:10Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateSystemQuestionBankQuestionUseCase createQuestionUseCase;
    @MockitoBean
    private UpdateQuestionUseCase updateQuestionUseCase;
    @MockitoBean
    private CloneQuestionUseCase cloneQuestionUseCase;
    @MockitoBean
    private UpdateQuestionStatusUseCase updateQuestionStatusUseCase;
    @MockitoBean
    private DeleteQuestionUseCase deleteQuestionUseCase;
    @MockitoBean
    private CreateQuestionCollaboratorUseCase createQuestionCollaboratorUseCase;
    @MockitoBean
    private UpdateQuestionCollaboratorUseCase updateQuestionCollaboratorUseCase;
    @MockitoBean
    private DeleteQuestionCollaboratorUseCase deleteQuestionCollaboratorUseCase;
    @MockitoBean
    private CreateQuestionAssetUseCase createQuestionAssetUseCase;
    @MockitoBean
    private UpdateQuestionAssetUseCase updateQuestionAssetUseCase;
    @MockitoBean
    private DeleteQuestionAssetUseCase deleteQuestionAssetUseCase;
    @MockitoBean
    private RegenerateQuestionAssetAnalysisUseCase regenerateQuestionAssetAnalysisUseCase;
    @MockitoBean
    private GetQuestionAssetUploadUrlUseCase getQuestionAssetUploadUrlUseCase;
    @MockitoBean
    private UpsertQuestionEvaluationGuideUseCase upsertQuestionEvaluationGuideUseCase;
    @MockitoBean
    private BulkUpdateQuestionStatusUseCase bulkUpdateQuestionStatusUseCase;
    @MockitoBean
    private PreviewQuestionImportFromFileUseCase previewQuestionImportFromFileUseCase;
    @MockitoBean
    private AcceptQuestionImportUseCase acceptQuestionImportUseCase;
    @MockitoBean
    private QuestionSpreadsheetService questionSpreadsheetService;

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateStatus_should_keep_question_json_text() {
        when(updateQuestionStatusUseCase.execute(any())).thenReturn(
            new QuestionDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Q-01", "Instruction", "Question", "Prompt", "Preparation", "READ_ALOUD", 10, 5, 60, "PRIVATE", null, false, "SUBMITTED_FOR_REVIEW", "NORMAL", null, Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID()));

        MvcTestResult result = mvc.patch().uri("/api/v1/questions/{id}/status", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\": \"SUBMIT\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "type", "READ_ALOUD",
                "status", "SUBMITTED_FOR_REVIEW",
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createCollaborator_should_keep_collaborator_json_text() {
        when(createQuestionCollaboratorUseCase.execute(any())).thenReturn(
            new QuestionCollaboratorDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "CAN_USE", Instant.parse(CREATED_AT)));

        MvcTestResult result = mvc.post().uri("/api/v1/questions/{id}/collaborators", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"00000000-0000-0000-0000-000000000001\", \"permission\": \"CAN_USE\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "permission", "CAN_USE",
                "assignedAt", CREATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void regenerateAssetAnalysis_should_keep_asset_json_text() {
        when(regenerateQuestionAssetAnalysisUseCase.execute(any())).thenReturn(
            new QuestionAssetDto(UUID.randomUUID(), UUID.randomUUID(), "Title", 5, "Alt", "AUDIO", "https://example.test/a.mp3", "Transcript", "Description", 1));

        MvcTestResult result = mvc.post().uri("/api/v1/questions/{id}/assets/{assetId}/regenerate-analysis", UUID.randomUUID(), UUID.randomUUID())
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "type", "AUDIO"
            ));
    }
}

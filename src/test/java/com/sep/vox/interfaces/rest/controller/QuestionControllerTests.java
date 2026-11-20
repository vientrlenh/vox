package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.command.CreateQuestionAssetMutationCommand;
import com.sep.vox.application.port.input.command.CreateQuestionCollaboratorCommand;
import com.sep.vox.application.port.input.command.CreateSystemQuestionBankQuestionCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionAssetCommand;
import com.sep.vox.application.port.input.command.UpdateQuestionCommand;
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
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.support.RestControllerTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

    @Test
    @WithMockUser(roles = "TEACHER")
    void create_should_bind_lower_case_type_and_sharing_to_enums() {
        MvcTestResult result = postCreate("\"short_answer\"", "\"school_shared\"");

        assertThat(result).hasStatus(201);
        ArgumentCaptor<CreateSystemQuestionBankQuestionCommand> captor =
            ArgumentCaptor.forClass(CreateSystemQuestionBankQuestionCommand.class);
        verify(createQuestionUseCase).execute(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(QuestionType.SHORT_ANSWER);
        assertThat(captor.getValue().sharing()).isEqualTo(QuestionSharing.SCHOOL_SHARED);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void create_should_answer_400_listing_question_types_for_unknown_type() {
        MvcTestResult result = postCreate("\"X\"", "\"PRIVATE\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.type").asString()
            .contains("READ_ALOUD", "SHORT_ANSWER", "LONG_ANSWER", "OPINION", "DESCRIPTION")
            .doesNotContain("QuestionType");
        verify(createQuestionUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void create_should_answer_400_listing_sharing_modes_for_unknown_sharing() {
        MvcTestResult result = postCreate("\"OPINION\"", "\"PUBLIC\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.sharing").asString()
            .contains("PRIVATE", "SCHOOL_SHARED");
        verify(createQuestionUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void create_should_answer_400_when_type_is_missing() {
        MvcTestResult result = postCreate("null", "\"PRIVATE\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.type").isNotNull();
        verify(createQuestionUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void update_should_bind_lower_case_type_and_sharing_to_enums() {
        MvcTestResult result = putUpdate("\"long_answer\"", "\"private\"");

        assertThat(result).hasStatus2xxSuccessful();
        ArgumentCaptor<UpdateQuestionCommand> captor = ArgumentCaptor.forClass(UpdateQuestionCommand.class);
        verify(updateQuestionUseCase).execute(captor.capture());
        assertThat(captor.getValue().questionType()).isEqualTo(QuestionType.LONG_ANSWER);
        assertThat(captor.getValue().sharing()).isEqualTo(QuestionSharing.PRIVATE);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void update_should_answer_400_for_unknown_type_and_sharing() {
        assertThat(putUpdate("\"X\"", "null")).hasStatus(400).bodyJson()
            .extractingPath("$.errors.type").asString().contains("READ_ALOUD", "DESCRIPTION");
        assertThat(putUpdate("null", "\"PUBLIC\"")).hasStatus(400).bodyJson()
            .extractingPath("$.errors.sharing").asString().contains("PRIVATE", "SCHOOL_SHARED");
        verify(updateQuestionUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void update_should_keep_type_and_sharing_optional() {
        assertThat(putUpdate("null", "null")).hasStatus2xxSuccessful();
    }

    private MvcTestResult postCreate(String typeJson, String sharingJson) {
        return mvc.post().uri("/api/v1/questions")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"questionBankId\": \"00000000-0000-0000-0000-000000000001\","
                + " \"questionTopicId\": \"00000000-0000-0000-0000-000000000002\","
                + " \"questionText\": \"Question\", \"type\": " + typeJson + ","
                + " \"preparationTimeSeconds\": 5, \"minResponseSeconds\": 5, \"maxResponseSeconds\": 30,"
                + " \"sharing\": " + sharingJson + "}")
            .exchange();
    }

    private MvcTestResult putUpdate(String typeJson, String sharingJson) {
        return mvc.put().uri("/api/v1/questions/{id}", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\": " + typeJson + ", \"sharing\": " + sharingJson + "}")
            .exchange();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createAsset_should_bind_lower_case_type_to_enum() {
        MvcTestResult result = postAsset("\"text_passage\"");

        assertThat(result).hasStatus(201);
        ArgumentCaptor<CreateQuestionAssetMutationCommand> captor =
            ArgumentCaptor.forClass(CreateQuestionAssetMutationCommand.class);
        verify(createQuestionAssetUseCase).execute(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(QuestionAssetType.TEXT_PASSAGE);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createAsset_should_answer_400_listing_asset_types_for_unknown_or_missing_type() {
        assertThat(postAsset("\"PDF\"")).hasStatus(400).bodyJson().extractingPath("$.errors.type").asString()
            .contains("AUDIO", "IMAGE", "VIDEO", "TEXT_PASSAGE").doesNotContain("QuestionAssetType");
        assertThat(postAsset("null")).hasStatus(400).bodyJson().extractingPath("$.errors.type").isNotNull();
        verify(createQuestionAssetUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateAsset_should_bind_type_and_reject_unknown_type() {
        assertThat(putAsset("\"audio\"")).hasStatus2xxSuccessful();
        ArgumentCaptor<UpdateQuestionAssetCommand> captor = ArgumentCaptor.forClass(UpdateQuestionAssetCommand.class);
        verify(updateQuestionAssetUseCase).execute(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(QuestionAssetType.AUDIO);

        assertThat(putAsset("\"PDF\"")).hasStatus(400).bodyJson().extractingPath("$.errors.type").asString()
            .contains("AUDIO", "TEXT_PASSAGE");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createCollaborator_should_bind_lower_case_permission_to_enum() {
        MvcTestResult result = postCollaborator("\"can_edit\"");

        assertThat(result).hasStatus(201);
        ArgumentCaptor<CreateQuestionCollaboratorCommand> captor =
            ArgumentCaptor.forClass(CreateQuestionCollaboratorCommand.class);
        verify(createQuestionCollaboratorUseCase).execute(captor.capture());
        assertThat(captor.getValue().permission()).isEqualTo(QuestionCollaboratorPermission.CAN_EDIT);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void collaborator_should_answer_400_listing_permissions_for_unknown_or_missing_permission() {
        assertThat(postCollaborator("\"OWNER\"")).hasStatus(400).bodyJson()
            .extractingPath("$.errors.permission").asString().contains("READ_ONLY", "CAN_USE", "CAN_EDIT");
        assertThat(postCollaborator("null")).hasStatus(400);
        assertThat(putCollaborator("\"OWNER\"")).hasStatus(400).bodyJson()
            .extractingPath("$.errors.permission").asString().contains("READ_ONLY", "CAN_USE", "CAN_EDIT");
        assertThat(putCollaborator("null")).hasStatus(400);
        verify(createQuestionCollaboratorUseCase, never()).execute(any());
        verify(updateQuestionCollaboratorUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateCollaborator_should_bind_permission_to_enum() {
        assertThat(putCollaborator("\"read_only\"")).hasStatus2xxSuccessful();
    }

    private MvcTestResult postAsset(String typeJson) {
        return mvc.post().uri("/api/v1/questions/{id}/assets", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\": " + typeJson + ", \"order\": 1}")
            .exchange();
    }

    private MvcTestResult putAsset(String typeJson) {
        return mvc.put().uri("/api/v1/questions/{id}/assets/{assetId}", UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\": " + typeJson + "}")
            .exchange();
    }

    private MvcTestResult postCollaborator(String permissionJson) {
        return mvc.post().uri("/api/v1/questions/{id}/collaborators", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"00000000-0000-0000-0000-000000000001\", \"permission\": " + permissionJson + "}")
            .exchange();
    }

    private MvcTestResult putCollaborator(String permissionJson) {
        return mvc.put().uri("/api/v1/questions/{id}/collaborators/{collaboratorId}", UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"permission\": " + permissionJson + "}")
            .exchange();
    }
    @Test
    @WithMockUser(roles = "TEACHER")
    void downloadTemplate_should_bind_type_and_answer_400_listing_types_for_unknown_type() {
        when(questionSpreadsheetService.downloadTemplate(any())).thenReturn(new byte[0]);
        when(questionSpreadsheetService.templateFileName()).thenReturn("template.xlsx");

        assertThat(mvc.get().uri("/api/v1/questions/import/template?type=opinion").exchange())
            .hasStatus2xxSuccessful();
        verify(questionSpreadsheetService).downloadTemplate(QuestionType.OPINION);
        assertThat(mvc.get().uri("/api/v1/questions/import/template").exchange()).hasStatus2xxSuccessful();
        verify(questionSpreadsheetService).downloadTemplate(null);
        assertThat(mvc.get().uri("/api/v1/questions/import/template?type=X").exchange())
            .hasStatus(400).bodyJson().extractingPath("$.errors.type").asString()
            .contains("READ_ALOUD", "SHORT_ANSWER", "LONG_ANSWER", "OPINION", "DESCRIPTION");
    }
}

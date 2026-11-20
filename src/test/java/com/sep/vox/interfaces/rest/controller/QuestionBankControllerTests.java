package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.usecase.questionbank.AcceptQuestionBankImportUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.BulkUpdateQuestionBankStatusUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateQuestionBankGradeUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSchoolQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.CreateSystemQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankGradeUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.DeleteQuestionBankUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.PreviewQuestionBankImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankStatusUseCase;
import com.sep.vox.application.port.input.usecase.questionbank.UpdateQuestionBankUseCase;
import com.sep.vox.domain.dto.QuestionBankDto;
import com.sep.vox.domain.dto.QuestionBankGradeDto;
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
 * {@code domain/dto} records returned by {@link QuestionBankController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(QuestionBankController.class)
class QuestionBankControllerTests {

    private static final String CREATED_AT = "2026-07-28T02:30:00Z";
    private static final String UPDATED_AT = "2026-07-29T03:45:10Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateSystemQuestionBankUseCase createSystemQuestionBankUseCase;
    @MockitoBean
    private CreateSchoolQuestionBankUseCase createSchoolQuestionBankUseCase;
    @MockitoBean
    private UpdateQuestionBankUseCase updateQuestionBankUseCase;
    @MockitoBean
    private UpdateQuestionBankStatusUseCase updateQuestionBankStatusUseCase;
    @MockitoBean
    private BulkUpdateQuestionBankStatusUseCase bulkUpdateQuestionBankStatusUseCase;
    @MockitoBean
    private DeleteQuestionBankUseCase deleteQuestionBankUseCase;
    @MockitoBean
    private CreateQuestionBankGradeUseCase createQuestionBankGradeUseCase;
    @MockitoBean
    private DeleteQuestionBankGradeUseCase deleteQuestionBankGradeUseCase;
    @MockitoBean
    private PreviewQuestionBankImportFromFileUseCase previewQuestionBankImportFromFileUseCase;
    @MockitoBean
    private AcceptQuestionBankImportUseCase acceptQuestionBankImportUseCase;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void updateStatus_should_keep_bank_json_text() {
        when(updateQuestionBankStatusUseCase.execute(any())).thenReturn(
            new QuestionBankDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "QB-01", "Bank", "Description", "SYSTEM", "PUBLISHED", Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID()));

        MvcTestResult result = mvc.patch().uri("/api/v1/question-banks/{id}/status", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\": \"PUBLISH\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "ownerType", "SYSTEM",
                "status", "PUBLISHED",
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void createGrade_should_keep_bankgrade_json_text() {
        when(createQuestionBankGradeUseCase.execute(any())).thenReturn(
            new QuestionBankGradeDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.parse(CREATED_AT), UUID.randomUUID()));

        MvcTestResult result = mvc.post().uri("/api/v1/question-banks/{id}/grades", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"schoolGradeId\": \"00000000-0000-0000-0000-000000000003\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "attachedAt", CREATED_AT
            ));
    }
}

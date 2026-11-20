package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.domain.model.assessment.AssessmentBlueprintSlotType;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.usecase.assessmentblueprint.CreateAssessmentBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.CreateAssessmentBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.CreateAssessmentBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.CreateAssessmentBlueprintVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DeleteAssessmentBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DeleteAssessmentBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DeleteAssessmentBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DeleteAssessmentBlueprintVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.DuplicateAssessmentBlueprintVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintActiveStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintSlotUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.UpdateAssessmentBlueprintVersionUseCase;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSectionDto;
import com.sep.vox.domain.dto.AssessmentBlueprintSlotDto;
import com.sep.vox.domain.dto.AssessmentBlueprintVersionDto;
import com.sep.vox.support.RestControllerTest;
import java.math.BigDecimal;
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
 * {@code domain/dto} records returned by {@link AssessmentBlueprintController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(AssessmentBlueprintController.class)
class AssessmentBlueprintControllerTests {

    private static final String CREATED_AT = "2026-07-28T02:30:00Z";
    private static final String UPDATED_AT = "2026-07-29T03:45:10Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateAssessmentBlueprintUseCase createAssessmentBlueprintUseCase;
    @MockitoBean
    private UpdateAssessmentBlueprintUseCase updateAssessmentBlueprintUseCase;
    @MockitoBean
    private UpdateAssessmentBlueprintActiveStatusUseCase updateAssessmentBlueprintActiveStatusUseCase;
    @MockitoBean
    private DeleteAssessmentBlueprintUseCase deleteAssessmentBlueprintUseCase;
    @MockitoBean
    private CreateAssessmentBlueprintVersionUseCase createAssessmentBlueprintVersionUseCase;
    @MockitoBean
    private DuplicateAssessmentBlueprintVersionUseCase duplicateAssessmentBlueprintVersionUseCase;
    @MockitoBean
    private UpdateAssessmentBlueprintVersionUseCase updateAssessmentBlueprintVersionUseCase;
    @MockitoBean
    private UpdateAssessmentBlueprintVersionStatusUseCase updateAssessmentBlueprintVersionStatusUseCase;
    @MockitoBean
    private DeleteAssessmentBlueprintVersionUseCase deleteAssessmentBlueprintVersionUseCase;
    @MockitoBean
    private CreateAssessmentBlueprintSectionUseCase createAssessmentBlueprintSectionUseCase;
    @MockitoBean
    private UpdateAssessmentBlueprintSectionUseCase updateAssessmentBlueprintSectionUseCase;
    @MockitoBean
    private DeleteAssessmentBlueprintSectionUseCase deleteAssessmentBlueprintSectionUseCase;
    @MockitoBean
    private CreateAssessmentBlueprintSlotUseCase createAssessmentBlueprintSlotUseCase;
    @MockitoBean
    private UpdateAssessmentBlueprintSlotUseCase updateAssessmentBlueprintSlotUseCase;
    @MockitoBean
    private DeleteAssessmentBlueprintSlotUseCase deleteAssessmentBlueprintSlotUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateActiveStatus_should_keep_blueprint_json_text() {
        when(updateAssessmentBlueprintActiveStatusUseCase.execute(any())).thenReturn(
            new AssessmentBlueprintDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "BP-01", "Blueprint", "Description", true, Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID()));

        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-blueprints/{id}/active", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"isActive\": true}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateVersionStatus_should_keep_version_json_text() {
        when(updateAssessmentBlueprintVersionStatusUseCase.execute(any())).thenReturn(
            new AssessmentBlueprintVersionDto(UUID.randomUUID(), UUID.randomUUID(), 1, "V1", "Description", AssessmentBlueprintVersionStatus.PUBLISHED, 1800, Instant.parse("2026-08-01T01:00:00Z"), Instant.parse("2026-08-01T03:00:00Z"), Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID()));

        MvcTestResult result = mvc.patch().uri("/api/v1/assessment-blueprint-versions/{id}/status", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\": \"PUBLISH\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "status", "PUBLISHED",
                "effectiveFrom", "2026-08-01T01:00:00Z",
                "effectiveTo", "2026-08-01T03:00:00Z",
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateSection_should_keep_section_json_text() {
        when(updateAssessmentBlueprintSectionUseCase.execute(any())).thenReturn(
            new AssessmentBlueprintSectionDto(UUID.randomUUID(), UUID.randomUUID(), 1, "Intro", "Instruction", 300, BigDecimal.ONE, Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID()));

        MvcTestResult result = mvc.put().uri("/api/v1/assessment-blueprint-sections/{sectionId}", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"order\": 1, \"title\": \"Intro\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateSlot_should_keep_slot_json_text() {
        when(updateAssessmentBlueprintSlotUseCase.execute(any())).thenReturn(
            new AssessmentBlueprintSlotDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, BigDecimal.ONE, 10, 60, AssessmentBlueprintSlotType.FIXED, UUID.randomUUID(), null, Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID()));

        MvcTestResult result = mvc.put().uri("/api/v1/assessment-blueprint-slots/{slotId}", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"order\": 1, \"slotType\": \"FIXED\", \"fixedQuestionId\": \"00000000-0000-0000-0000-000000000002\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "slotType", "FIXED",
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }
}

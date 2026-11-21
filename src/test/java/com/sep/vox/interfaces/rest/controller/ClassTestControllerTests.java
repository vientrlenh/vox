package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.usecase.assessment.ChangeClassTestBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateClassTestUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateClassTestQuestionsUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateClassTestSectionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentgrading.ClaimClassTestGradingUseCase;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.support.RestControllerTest;
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
 * {@code domain/dto} records returned by {@link ClassTestController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(ClassTestController.class)
class ClassTestControllerTests {

    private static final String CREATED_AT = "2026-07-28T02:30:00Z";
    private static final String UPDATED_AT = "2026-07-29T03:45:10Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateClassTestUseCase createClassTestUseCase;
    @MockitoBean
    private UpdateAssessmentUseCase updateAssessmentUseCase;
    @MockitoBean
    private UpdateAssessmentStatusUseCase updateAssessmentStatusUseCase;
    @MockitoBean
    private DeleteAssessmentUseCase deleteAssessmentUseCase;
    @MockitoBean
    private UpdateClassTestQuestionsUseCase updateClassTestQuestionsUseCase;
    @MockitoBean
    private CreateClassTestSectionUseCase createClassTestSectionUseCase;
    @MockitoBean
    private UpdateClassTestSectionUseCase updateClassTestSectionUseCase;
    @MockitoBean
    private DeleteClassTestSectionUseCase deleteClassTestSectionUseCase;
    @MockitoBean
    private ChangeClassTestBlueprintUseCase changeClassTestBlueprintUseCase;
    @MockitoBean
    private ClaimClassTestGradingUseCase claimClassTestGradingUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateStatus_should_keep_assessment_json_text() {
        when(updateAssessmentStatusUseCase.execute(any())).thenReturn(
            new AssessmentDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "AS-01", "Midterm", "Description", UUID.randomUUID(), UUID.randomUUID(), AssessmentKind.CENTRALIZED, AssessmentDeliveryMode.LAB, AssessmentStatus.SCHEDULED, 1, 900, ResultDecisionMethod.HIGHEST, Instant.parse("2026-08-01T01:00:00Z"), Instant.parse("2026-08-01T03:00:00Z"), UUID.randomUUID(), false, null, null, false, Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID(), null));

        MvcTestResult result = mvc.patch().uri("/api/v1/class-tests/{assessmentId}/status", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\": \"START\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "kind", "CENTRALIZED",
                "status", "SCHEDULED",
                "openAt", "2026-08-01T01:00:00Z",
                "closeAt", "2026-08-01T03:00:00Z",
                "createdAt", CREATED_AT,
                "updatedAt", UPDATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void create_should_answer_400_naming_open_at_for_unparsable_date() {
        MvcTestResult result = mvc.post().uri("/api/v1/class-tests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"schoolClassId\": \"00000000-0000-0000-0000-000000000001\", \"name\": \"Quiz\","
                + " \"openAt\": \"2026-13-45\", \"closeAt\": \"2026-08-01T03:00:00Z\","
                + " \"assessmentPolicyId\": \"00000000-0000-0000-0000-000000000002\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.openAt").isNotNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void create_should_answer_400_listing_modes_for_unknown_delivery_mode() {
        MvcTestResult result = mvc.post().uri("/api/v1/class-tests")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"schoolClassId\": \"00000000-0000-0000-0000-000000000001\", \"name\": \"Quiz\","
                + " \"openAt\": \"2026-08-01T01:00:00Z\", \"closeAt\": \"2026-08-01T03:00:00Z\","
                + " \"assessmentPolicyId\": \"00000000-0000-0000-0000-000000000002\", \"deliveryMode\": \"NOPE\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.deliveryMode").asString()
            .contains("STUDENT_DEVICE", "LAB");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void claimGrading_should_bind_round_type_in_any_letter_case() {
        MvcTestResult result = postClaim("\"spot_check\"");

        assertThat(result).hasStatus2xxSuccessful();
        verify(claimClassTestGradingUseCase).execute(
            argThat(command -> command.roundType() == GradingRoundType.SPOT_CHECK));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void claimGrading_should_answer_400_listing_rounds_for_unknown_round_type() {
        MvcTestResult result = postClaim("\"NOPE\"");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.roundType").asString()
            .contains("INITIAL", "SPOT_CHECK", "REMEDIATION", "APPEAL");
        verify(claimClassTestGradingUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void claimGrading_should_answer_400_when_round_type_is_missing() {
        MvcTestResult result = postClaim("null");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.roundType").isNotNull();
    }

    private MvcTestResult postClaim(String roundTypeJson) {
        return mvc.post().uri("/api/v1/class-tests/{assessmentId}/grading/claim", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"roundType\": " + roundTypeJson
                + ", \"candidateResultIds\": [\"00000000-0000-0000-0000-000000000002\"]}")
            .exchange();
    }
}

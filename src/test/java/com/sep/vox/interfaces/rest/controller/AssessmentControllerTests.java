package com.sep.vox.interfaces.rest.controller;

import com.sep.vox.domain.model.assessment.AssessmentDeliveryMode;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolReleaseMode;
import com.sep.vox.domain.model.assessment.AssessmentSecurePoolStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.assessment.ResultDecisionMethod;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.usecase.assessment.AttachAssessmentBlueprintUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateAssessmentMemberUseCase;
import com.sep.vox.application.port.input.usecase.assessment.CreateAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentMemberUseCase;
import com.sep.vox.application.port.input.usecase.assessment.DeleteAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.StartClassTestSessionUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentDeliveryModeUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentMemberUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentSecurePoolStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentStatusUseCase;
import com.sep.vox.application.port.input.usecase.assessment.UpdateAssessmentUseCase;
import com.sep.vox.application.port.input.usecase.assessment.VerifyAssessmentScheduleOtpUseCase;
import com.sep.vox.application.port.input.usecase.assessment.ViewMyAssessmentsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentschedule.GetAssessmentScheduleOtpUseCase;
import com.sep.vox.domain.dto.AssessmentDto;
import com.sep.vox.domain.dto.AssessmentMemberDto;
import com.sep.vox.domain.dto.AssessmentSecurePoolDto;
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
 * {@code domain/dto} records returned by {@link AssessmentController}. The GraphQL typed-scalars plan changes those
 * field types; the JSON written here must stay the same.
 */
@RestControllerTest(AssessmentController.class)
class AssessmentControllerTests {

    private static final String CREATED_AT = "2026-07-28T02:30:00Z";
    private static final String UPDATED_AT = "2026-07-29T03:45:10Z";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateAssessmentUseCase createAssessmentUseCase;
    @MockitoBean
    private UpdateAssessmentUseCase updateAssessmentUseCase;
    @MockitoBean
    private UpdateAssessmentStatusUseCase updateAssessmentStatusUseCase;
    @MockitoBean
    private DeleteAssessmentUseCase deleteAssessmentUseCase;
    @MockitoBean
    private CreateAssessmentMemberUseCase createAssessmentMemberUseCase;
    @MockitoBean
    private UpdateAssessmentMemberUseCase updateAssessmentMemberUseCase;
    @MockitoBean
    private DeleteAssessmentMemberUseCase deleteAssessmentMemberUseCase;
    @MockitoBean
    private UpdateAssessmentSecurePoolStatusUseCase updateAssessmentSecurePoolStatusUseCase;
    @MockitoBean
    private AttachAssessmentBlueprintUseCase attachAssessmentBlueprintUseCase;
    @MockitoBean
    private UpdateAssessmentDeliveryModeUseCase updateAssessmentDeliveryModeUseCase;
    @MockitoBean
    private GetAssessmentScheduleOtpUseCase getAssessmentScheduleOtpUseCase;
    @MockitoBean
    private ViewMyAssessmentsUseCase viewMyAssessmentsUseCase;
    @MockitoBean
    private VerifyAssessmentScheduleOtpUseCase verifyAssessmentScheduleOtpUseCase;
    @MockitoBean
    private StartClassTestSessionUseCase startClassTestSessionUseCase;

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateStatus_should_keep_assessment_json_text() {
        when(updateAssessmentStatusUseCase.execute(any())).thenReturn(
            new AssessmentDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "AS-01", "Midterm", "Description", UUID.randomUUID(), UUID.randomUUID(), AssessmentKind.CENTRALIZED, AssessmentDeliveryMode.LAB, AssessmentStatus.SCHEDULED, 1, 900, ResultDecisionMethod.HIGHEST, Instant.parse("2026-08-01T01:00:00Z"), Instant.parse("2026-08-01T03:00:00Z"), UUID.randomUUID(), false, null, null, false, Instant.parse(CREATED_AT), Instant.parse(UPDATED_AT), UUID.randomUUID(), UUID.randomUUID(), null));

        MvcTestResult result = mvc.patch().uri("/api/v1/assessments/{id}/status", UUID.randomUUID())
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
    void createMember_should_keep_member_json_text() {
        when(createAssessmentMemberUseCase.execute(any())).thenReturn(
            new AssessmentMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AssessmentMemberRole.CHAIR, Instant.parse(CREATED_AT), UUID.randomUUID()));

        MvcTestResult result = mvc.post().uri("/api/v1/assessments/{assessmentId}/members", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"00000000-0000-0000-0000-000000000001\", \"role\": \"CHAIR\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "role", "CHAIR",
                "grantedAt", CREATED_AT
            ));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateSecurePoolStatus_should_keep_pool_json_text() {
        when(updateAssessmentSecurePoolStatusUseCase.execute(any())).thenReturn(
            new AssessmentSecurePoolDto(UUID.randomUUID(), UUID.randomUUID(), AssessmentSecurePoolStatus.SEALED, AssessmentSecurePoolReleaseMode.MANUAL, Instant.parse("2026-08-01T01:00:00Z"), Instant.parse("2026-08-01T03:00:00Z"), UUID.randomUUID()));

        MvcTestResult result = mvc.patch().uri("/api/v1/assessments/{assessmentId}/secure-pool/status", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"action\": \"RELEASE\"}")
            .exchange();

        assertThat(result).hasStatus2xxSuccessful().bodyJson().extractingPath("$.data").asMap()
            .containsAllEntriesOf(Map.of(
                "status", "SEALED",
                "embargoUntil", "2026-08-01T01:00:00Z",
                "releasedAt", "2026-08-01T03:00:00Z"
            ));
    }
}

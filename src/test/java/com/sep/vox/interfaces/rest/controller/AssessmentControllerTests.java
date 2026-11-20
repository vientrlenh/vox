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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.command.CreateAssessmentCommand;
import com.sep.vox.application.port.input.command.UpdateAssessmentDeliveryModeCommand;
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
import com.sep.vox.application.port.input.query.ViewMyAssessmentsQuery;
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
import org.mockito.ArgumentCaptor;
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

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyAssessments_should_bind_lower_case_kind_query_value() {
        MvcTestResult result = mvc.get().uri("/api/v1/assessments?kind=class_test").exchange();

        assertThat(result).hasStatus2xxSuccessful();
        ArgumentCaptor<ViewMyAssessmentsQuery> query = ArgumentCaptor.forClass(ViewMyAssessmentsQuery.class);
        verify(viewMyAssessmentsUseCase).execute(query.capture());
        assertThat(query.getValue().kind()).isEqualTo(AssessmentKind.CLASS_TEST);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyAssessments_should_answer_400_listing_kinds_for_unknown_value() {
        MvcTestResult result = mvc.get().uri("/api/v1/assessments?kind=NOPE").exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.kind").asString()
            .contains("CENTRALIZED", "CLASS_TEST");
    }

    private static String createAssessmentBody(String openAt, String deliveryMode) {
        return "{\"code\": \"EX-1\", \"name\": \"Midterm\", \"languageId\": \"00000000-0000-0000-0000-000000000001\","
            + openAt + "\"closeAt\": \"2026-08-01T03:00:00Z\"," + deliveryMode + "}";
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void create_should_bind_instant_and_lower_case_delivery_mode() {
        MvcTestResult result = mvc.post().uri("/api/v1/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createAssessmentBody("\"openAt\": \"2026-08-01T08:00:00+07:00\",", "\"deliveryMode\": \"lab\""))
            .exchange();

        assertThat(result).hasStatus2xxSuccessful();
        ArgumentCaptor<CreateAssessmentCommand> command = ArgumentCaptor.forClass(CreateAssessmentCommand.class);
        verify(createAssessmentUseCase).execute(command.capture());
        assertThat(command.getValue().openAt()).isEqualTo(Instant.parse("2026-08-01T01:00:00Z"));
        assertThat(command.getValue().deliveryMode()).isEqualTo(AssessmentDeliveryMode.LAB);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void create_should_answer_400_naming_open_at_for_unparsable_date() {
        MvcTestResult result = mvc.post().uri("/api/v1/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createAssessmentBody("\"openAt\": \"2026-13-45\",", "\"deliveryMode\": \"LAB\""))
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.openAt").isNotNull();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void create_should_answer_400_listing_modes_for_unknown_delivery_mode() {
        MvcTestResult result = mvc.post().uri("/api/v1/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createAssessmentBody("\"openAt\": \"2026-08-01T01:00:00Z\",", "\"deliveryMode\": \"NOPE\""))
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.deliveryMode").asString()
            .contains("STUDENT_DEVICE", "LAB");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void create_should_answer_400_when_open_at_is_missing() {
        MvcTestResult result = mvc.post().uri("/api/v1/assessments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createAssessmentBody("", "\"deliveryMode\": \"LAB\""))
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.openAt").isNotNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateDeliveryMode_should_bind_enum_and_reject_unknown_value() {
        MvcTestResult valid = mvc.patch().uri("/api/v1/assessments/{id}/delivery-mode", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deliveryMode\": \"STUDENT_DEVICE\"}")
            .exchange();
        MvcTestResult unknown = mvc.patch().uri("/api/v1/assessments/{id}/delivery-mode", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"deliveryMode\": \"NOPE\"}")
            .exchange();

        assertThat(valid).hasStatus2xxSuccessful();
        ArgumentCaptor<UpdateAssessmentDeliveryModeCommand> command = ArgumentCaptor.forClass(UpdateAssessmentDeliveryModeCommand.class);
        verify(updateAssessmentDeliveryModeUseCase).execute(command.capture());
        assertThat(command.getValue().deliveryMode()).isEqualTo(AssessmentDeliveryMode.STUDENT_DEVICE);
        assertThat(unknown).hasStatus(400).bodyJson().extractingPath("$.errors.deliveryMode").isNotNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createMember_should_answer_400_listing_roles_for_unknown_role() {
        MvcTestResult result = mvc.post().uri("/api/v1/assessments/{assessmentId}/members", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"00000000-0000-0000-0000-000000000001\", \"role\": \"NOPE\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.role").asString()
            .contains("CHAIR", "AUTHOR", "REVIEWER");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createMember_should_answer_400_when_role_is_missing() {
        MvcTestResult result = mvc.post().uri("/api/v1/assessments/{assessmentId}/members", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": \"00000000-0000-0000-0000-000000000001\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.role").isNotNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void updateMember_should_answer_400_listing_roles_for_unknown_role() {
        MvcTestResult result = mvc.put().uri("/api/v1/assessments/{assessmentId}/members/{memberId}", UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\": \"NOPE\"}")
            .exchange();

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.role").asString()
            .contains("CHAIR", "AUTHOR", "REVIEWER");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyAssessments_should_answer_400_for_unknown_status_filter() {
        MvcTestResult result = mvc.get().uri("/api/v1/assessments?status=NOPE").exchange();

        assertThat(result).hasStatus(400);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void getMyAssessments_should_pass_known_status_filter_to_use_case() {
        MvcTestResult result = mvc.get().uri("/api/v1/assessments?status=upcoming").exchange();

        assertThat(result).hasStatus2xxSuccessful();
        ArgumentCaptor<ViewMyAssessmentsQuery> query = ArgumentCaptor.forClass(ViewMyAssessmentsQuery.class);
        verify(viewMyAssessmentsUseCase).execute(query.capture());
        assertThat(query.getValue().status()).isEqualTo("upcoming");
    }
}

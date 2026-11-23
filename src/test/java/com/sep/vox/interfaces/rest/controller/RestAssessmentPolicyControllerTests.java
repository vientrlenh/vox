package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.command.assessmentpolicysystem.CreateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.AcceptSchoolAssessmentPolicyImportUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.AcceptSystemAssessmentPolicyImportUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.ArchiveSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.ArchiveSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.CreateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.CreateSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.DeleteSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.DeleteSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.PreviewSchoolAssessmentPolicyImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.PreviewSystemAssessmentPolicyImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.PublishSchoolAssessmentPoliciesByRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.PublishSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.PublishSystemAssessmentPoliciesByRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.PublishSystemAssessmentPolicyUseCase;
import com.sep.vox.support.RestControllerTest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@RestControllerTest(RestAssessmentPolicyController.class)
class RestAssessmentPolicyControllerTests {

    private static final String SYSTEM_URI = "/api/v1/assessment-policies/system";
    private static final String SCHOOL_URI = "/api/v1/assessment-policies/schools/{schoolId}";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AcceptSchoolAssessmentPolicyImportUseCase acceptSchoolAssessmentPolicyImportUseCase;
    @MockitoBean
    private AcceptSystemAssessmentPolicyImportUseCase acceptSystemAssessmentPolicyImportUseCase;
    @MockitoBean
    private ArchiveSchoolAssessmentPolicyUseCase archiveSchoolAssessmentPolicyUseCase;
    @MockitoBean
    private ArchiveSystemAssessmentPolicyUseCase archiveSystemAssessmentPolicyUseCase;
    @MockitoBean
    private CreateSchoolAssessmentPolicyUseCase createSchoolAssessmentPolicyUseCase;
    @MockitoBean
    private CreateSystemAssessmentPolicyUseCase createSystemAssessmentPolicyUseCase;
    @MockitoBean
    private DeleteSchoolAssessmentPolicyUseCase deleteSchoolAssessmentPolicyUseCase;
    @MockitoBean
    private DeleteSystemAssessmentPolicyUseCase deleteSystemAssessmentPolicyUseCase;
    @MockitoBean
    private PreviewSchoolAssessmentPolicyImportFromFileUseCase previewSchoolAssessmentPolicyImportFromFileUseCase;
    @MockitoBean
    private PreviewSystemAssessmentPolicyImportFromFileUseCase previewSystemAssessmentPolicyImportFromFileUseCase;
    @MockitoBean
    private PublishSchoolAssessmentPoliciesByRubricVersionUseCase publishSchoolAssessmentPoliciesByRubricVersionUseCase;
    @MockitoBean
    private PublishSchoolAssessmentPolicyUseCase publishSchoolAssessmentPolicyUseCase;
    @MockitoBean
    private PublishSystemAssessmentPoliciesByRubricVersionUseCase publishSystemAssessmentPoliciesByRubricVersionUseCase;
    @MockitoBean
    private PublishSystemAssessmentPolicyUseCase publishSystemAssessmentPolicyUseCase;

    private static String policyBody(String effectiveFromJson) {
        return "[{\"frameworkVersionId\": \"" + UUID.randomUUID() + "\", \"rubricVersionId\": \"" + UUID.randomUUID()
            + "\", \"languageId\": \"" + UUID.randomUUID() + "\", \"targetFrameworkBandId\": \"" + UUID.randomUUID()
            + "\"" + effectiveFromJson + "}]";
    }

    private MvcTestResult postSystemPolicy(String body) {
        return mvc.post().uri(SYSTEM_URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult postSchoolPolicy(String body) {
        return mvc.post().uri(SCHOOL_URI, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createSystemPolicy_should_bind_effective_instants_with_offset() {
        when(createSystemAssessmentPolicyUseCase.execute(any())).thenReturn(List.of(UUID.randomUUID()));

        MvcTestResult result = postSystemPolicy(policyBody(
            ", \"effectiveFrom\": \"2026-07-28T07:00:00+07:00\", \"effectiveTo\": \"2027-07-28T00:00:00Z\""));

        assertThat(result).hasStatusOk();
        ArgumentCaptor<List<CreateAssessmentPolicyCommand>> commands = ArgumentCaptor.captor();
        verify(createSystemAssessmentPolicyUseCase).execute(commands.capture());
        CreateAssessmentPolicyCommand command = commands.getValue().getFirst();
        assertThat(command.effectiveFrom()).isEqualTo(Instant.parse("2026-07-28T00:00:00Z"));
        assertThat(command.effectiveTo()).isEqualTo(Instant.parse("2027-07-28T00:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createSystemPolicy_should_answer_400_naming_effective_from_for_date_only_value() {
        MvcTestResult result = postSystemPolicy(policyBody(", \"effectiveFrom\": \"2026-07-28\""));

        assertThat(result).hasStatus(400).bodyText().contains("effectiveFrom");
        verifyNoInteractions(createSystemAssessmentPolicyUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createSystemPolicy_should_answer_400_with_required_message_when_effective_from_is_missing() {
        MvcTestResult result = postSystemPolicy(policyBody(""));

        assertThat(result).hasStatus(400).bodyText().contains("Ngày bắt đầu hiệu lực không được để trống");
        verifyNoInteractions(createSystemAssessmentPolicyUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void createSchoolPolicy_should_bind_effective_instants_with_offset() {
        when(createSchoolAssessmentPolicyUseCase.execute(any())).thenReturn(List.of(UUID.randomUUID()));

        MvcTestResult result = postSchoolPolicy(policyBody(", \"effectiveFrom\": \"2026-07-28T00:00:00Z\""));

        assertThat(result).hasStatusOk();
        ArgumentCaptor<List<CreateAssessmentPolicyCommand>> commands = ArgumentCaptor.captor();
        verify(createSchoolAssessmentPolicyUseCase).execute(commands.capture());
        assertThat(commands.getValue().getFirst().effectiveFrom()).isEqualTo(Instant.parse("2026-07-28T00:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void createSchoolPolicy_should_answer_400_for_date_with_slashes() {
        MvcTestResult result = postSchoolPolicy(policyBody(", \"effectiveFrom\": \"28/07/2026\""));

        assertThat(result).hasStatus(400).bodyText().contains("effectiveFrom");
        verifyNoInteractions(createSchoolAssessmentPolicyUseCase);
    }

    @Test
    @WithAnonymousUser
    void createSystemPolicy_should_answer_401_for_anonymous() {
        assertThat(postSystemPolicy(policyBody(", \"effectiveFrom\": \"2026-07-28T00:00:00Z\""))).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createSystemPolicy_should_answer_403_for_wrong_role() {
        assertThat(postSystemPolicy(policyBody(", \"effectiveFrom\": \"2026-07-28T00:00:00Z\""))).hasStatus(403);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createSchoolPolicy_should_answer_403_for_system_admin() {
        assertThat(postSchoolPolicy(policyBody(", \"effectiveFrom\": \"2026-07-28T00:00:00Z\""))).hasStatus(403);
    }
}

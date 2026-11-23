package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sep.vox.application.port.input.command.framework.CreateFrameworkVersionCommand;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkCriterionBandsUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.CreateFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkCriterionBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkCriterionUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkResultBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.DeleteFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkStatusUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionStatusUseCase;
import com.sep.vox.support.RestControllerTest;
import java.time.Instant;
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

@RestControllerTest(RestFrameworkController.class)
class RestFrameworkControllerTests {

    private static final String VERSIONS_URI = "/api/v1/frameworks/{frameworkId}/versions";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CreateFrameworkCriteriaUseCase createFrameworkCriteriaUseCase;
    @MockitoBean
    private CreateFrameworkCriterionBandsUseCase createFrameworkCriterionBandsUseCase;
    @MockitoBean
    private CreateFrameworkResultBandsUseCase createFrameworkResultBandsUseCase;
    @MockitoBean
    private CreateFrameworkUseCase createFrameworkUseCase;
    @MockitoBean
    private CreateFrameworkVersionUseCase createFrameworkVersionUseCase;
    @MockitoBean
    private DeleteFrameworkCriterionBandUseCase deleteFrameworkCriterionBandUseCase;
    @MockitoBean
    private DeleteFrameworkCriterionUseCase deleteFrameworkCriterionUseCase;
    @MockitoBean
    private DeleteFrameworkResultBandUseCase deleteFrameworkResultBandUseCase;
    @MockitoBean
    private DeleteFrameworkUseCase deleteFrameworkUseCase;
    @MockitoBean
    private DeleteFrameworkVersionUseCase deleteFrameworkVersionUseCase;
    @MockitoBean
    private UpdateFrameworkStatusUseCase updateFrameworkStatusUseCase;
    @MockitoBean
    private UpdateFrameworkUseCase updateFrameworkUseCase;
    @MockitoBean
    private UpdateFrameworkVersionStatusUseCase updateFrameworkVersionStatusUseCase;

    private static String versionBody(String effectiveFromJson) {
        return "{\"code\": \"V1\", \"name\": \"Version 1\", \"version\": 1" + effectiveFromJson + "}";
    }

    private MvcTestResult postVersion(String body) {
        return mvc.post().uri(VERSIONS_URI, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createVersion_should_bind_effective_instants_with_offset() {
        MvcTestResult result = postVersion(versionBody(
            ", \"effectiveFrom\": \"2026-07-28T07:00:00+07:00\", \"effectiveTo\": \"2027-07-28T00:00:00Z\""));

        assertThat(result).hasStatus2xxSuccessful();
        ArgumentCaptor<CreateFrameworkVersionCommand> command = ArgumentCaptor.forClass(CreateFrameworkVersionCommand.class);
        verify(createFrameworkVersionUseCase).execute(command.capture());
        assertThat(command.getValue().effectiveFrom()).isEqualTo(Instant.parse("2026-07-28T00:00:00Z"));
        assertThat(command.getValue().effectiveTo()).isEqualTo(Instant.parse("2027-07-28T00:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createVersion_should_answer_400_naming_effective_from_for_date_only_value() {
        MvcTestResult result = postVersion(versionBody(", \"effectiveFrom\": \"2026-07-28\""));

        assertThat(result).hasStatus(400).bodyText().contains("effectiveFrom");
        verifyNoInteractions(createFrameworkVersionUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void createVersion_should_answer_400_naming_effective_from_when_missing() {
        MvcTestResult result = postVersion(versionBody(""));

        assertThat(result).hasStatus(400).bodyText().contains("effectiveFrom");
        verifyNoInteractions(createFrameworkVersionUseCase);
    }

    @Test
    @WithAnonymousUser
    void createVersion_should_answer_401_for_anonymous() {
        assertThat(postVersion(versionBody(", \"effectiveFrom\": \"2026-07-28T00:00:00Z\""))).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createVersion_should_answer_403_for_wrong_role() {
        assertThat(postVersion(versionBody(", \"effectiveFrom\": \"2026-07-28T00:00:00Z\""))).hasStatus(403);
    }
}

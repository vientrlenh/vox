package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sep.vox.application.port.input.command.rubricschool.AddSchoolRubricVersionsCommand;
import com.sep.vox.application.port.input.command.rubricsystem.AddSystemRubricVersionsCommand;
import com.sep.vox.application.port.input.command.rubricschool.CloneSystemRubricToSchoolCommand;
import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricCriterionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricResultBandImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AcceptSchoolRubricVersionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricCriterionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricResultBandImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AcceptSystemRubricVersionImportUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.AddSchoolRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.AddSystemRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ArchiveSchoolRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ArchiveSystemRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ChangeSchoolRubricVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ChangeSystemRubricVersionStatusUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CloneSystemRubricToSchoolUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.CreateSchoolRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.CreateSystemRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricResultBandUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.DeleteSchoolRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricResultBandUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.DeleteSystemRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricCriterionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricResultBandImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.PreviewSchoolRubricVersionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricCriterionImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricResultBandImportFromFileUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.PreviewSystemRubricVersionImportFromFileUseCase;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
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

@RestControllerTest(RestRubricController.class)
class RestRubricControllerTests {

    private static final String SYSTEM_VERSIONS_URI = "/api/v1/rubrics/system/rubrics/{rubricId}/versions";
    private static final String SCHOOL_VERSIONS_URI = "/api/v1/rubrics/schools/{schoolId}/rubrics/{rubricId}/versions";
    private static final String CLONE_URI = "/api/v1/rubrics/schools/{schoolId}/rubrics/clone-from-system";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private AcceptSchoolRubricCriterionImportUseCase acceptSchoolRubricCriterionImportUseCase;
    @MockitoBean
    private AcceptSchoolRubricResultBandImportUseCase acceptSchoolRubricResultBandImportUseCase;
    @MockitoBean
    private AcceptSchoolRubricVersionImportUseCase acceptSchoolRubricVersionImportUseCase;
    @MockitoBean
    private AcceptSystemRubricCriterionImportUseCase acceptSystemRubricCriterionImportUseCase;
    @MockitoBean
    private AcceptSystemRubricResultBandImportUseCase acceptSystemRubricResultBandImportUseCase;
    @MockitoBean
    private AcceptSystemRubricVersionImportUseCase acceptSystemRubricVersionImportUseCase;
    @MockitoBean
    private AddSchoolRubricVersionsUseCase addSchoolRubricVersionsUseCase;
    @MockitoBean
    private AddSystemRubricVersionsUseCase addSystemRubricVersionsUseCase;
    @MockitoBean
    private ArchiveSchoolRubricVersionUseCase archiveSchoolRubricVersionUseCase;
    @MockitoBean
    private ArchiveSystemRubricVersionUseCase archiveSystemRubricVersionUseCase;
    @MockitoBean
    private ChangeSchoolRubricVersionStatusUseCase changeSchoolRubricVersionStatusUseCase;
    @MockitoBean
    private ChangeSystemRubricVersionStatusUseCase changeSystemRubricVersionStatusUseCase;
    @MockitoBean
    private CloneSystemRubricToSchoolUseCase cloneSystemRubricToSchoolUseCase;
    @MockitoBean
    private CreateSchoolRubricCriterionUseCase createSchoolRubricCriterionUseCase;
    @MockitoBean
    private CreateSchoolRubricResultBandsUseCase createSchoolRubricResultBandsUseCase;
    @MockitoBean
    private CreateSchoolRubricUseCase createSchoolRubricUseCase;
    @MockitoBean
    private CreateSystemRubricCriteriaUseCase createSystemRubricCriteriaUseCase;
    @MockitoBean
    private CreateSystemRubricResultBandsUseCase createSystemRubricResultBandsUseCase;
    @MockitoBean
    private CreateSystemRubricUseCase createSystemRubricUseCase;
    @MockitoBean
    private DeleteSchoolRubricCriterionUseCase deleteSchoolRubricCriterionUseCase;
    @MockitoBean
    private DeleteSchoolRubricResultBandUseCase deleteSchoolRubricResultBandUseCase;
    @MockitoBean
    private DeleteSchoolRubricUseCase deleteSchoolRubricUseCase;
    @MockitoBean
    private DeleteSchoolRubricVersionUseCase deleteSchoolRubricVersionUseCase;
    @MockitoBean
    private DeleteSystemRubricCriterionUseCase deleteSystemRubricCriterionUseCase;
    @MockitoBean
    private DeleteSystemRubricResultBandUseCase deleteSystemRubricResultBandUseCase;
    @MockitoBean
    private DeleteSystemRubricUseCase deleteSystemRubricUseCase;
    @MockitoBean
    private DeleteSystemRubricVersionUseCase deleteSystemRubricVersionUseCase;
    @MockitoBean
    private PreviewSchoolRubricCriterionImportFromFileUseCase previewSchoolRubricCriterionImportFromFileUseCase;
    @MockitoBean
    private PreviewSchoolRubricResultBandImportFromFileUseCase previewSchoolRubricResultBandImportFromFileUseCase;
    @MockitoBean
    private PreviewSchoolRubricVersionImportFromFileUseCase previewSchoolRubricVersionImportFromFileUseCase;
    @MockitoBean
    private PreviewSystemRubricCriterionImportFromFileUseCase previewSystemRubricCriterionImportFromFileUseCase;
    @MockitoBean
    private PreviewSystemRubricResultBandImportFromFileUseCase previewSystemRubricResultBandImportFromFileUseCase;
    @MockitoBean
    private PreviewSystemRubricVersionImportFromFileUseCase previewSystemRubricVersionImportFromFileUseCase;

    private static String versionBody(String totalScoreMethodJson, String effectiveFromJson) {
        return "{\"versions\": [{\"version\": 2, \"name\": \"V2\", \"scoringScaleMin\": 0, \"scoringScaleMax\": 10,"
            + totalScoreMethodJson + effectiveFromJson + "}]}";
    }

    private MvcTestResult postSystemVersions(String body) {
        return mvc.post().uri(SYSTEM_VERSIONS_URI, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void addSystemVersions_should_bind_lower_case_method_and_instant_dates() {
        when(addSystemRubricVersionsUseCase.execute(any())).thenReturn(List.of(UUID.randomUUID()));

        MvcTestResult result = postSystemVersions(versionBody(
            "\"totalScoreMethod\": \"sum\",", "\"effectiveFrom\": \"2026-08-01T08:00:00+07:00\", \"effectiveTo\": \"2027-08-01T00:00:00Z\""));

        assertThat(result).hasStatusOk();
        ArgumentCaptor<AddSystemRubricVersionsCommand> command = ArgumentCaptor.forClass(AddSystemRubricVersionsCommand.class);
        verify(addSystemRubricVersionsUseCase).execute(command.capture());
        AddSystemRubricVersionsCommand.RubricVersionItemCommand item = command.getValue().versions().getFirst();
        assertThat(item.totalScoreMethod()).isEqualTo(RubricTotalScoreMethod.SUM);
        assertThat(item.effectiveFrom()).isEqualTo(Instant.parse("2026-08-01T01:00:00Z"));
        assertThat(item.effectiveTo()).isEqualTo(Instant.parse("2027-08-01T00:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void addSystemVersions_should_answer_400_listing_methods_for_unknown_method() {
        MvcTestResult result = postSystemVersions(versionBody("\"totalScoreMethod\": \"MEDIAN\"", ""));

        assertThat(result).hasStatus(400).bodyText().contains("totalScoreMethod", "WEIGHTED_AVERAGE", "SUM");
        verifyNoInteractions(addSystemRubricVersionsUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void addSystemVersions_should_answer_400_naming_method_when_missing() {
        MvcTestResult result = postSystemVersions(versionBody("\"totalScoreMethod\": null", ""));

        assertThat(result).hasStatus(400).bodyText().contains("totalScoreMethod");
        verifyNoInteractions(addSystemRubricVersionsUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void addSystemVersions_should_answer_400_for_date_without_offset() {
        MvcTestResult result = postSystemVersions(versionBody(
            "\"totalScoreMethod\": \"SUM\",", "\"effectiveFrom\": \"2026-08-01\""));

        assertThat(result).hasStatus(400).bodyText().contains("effectiveFrom");
        verifyNoInteractions(addSystemRubricVersionsUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void addSchoolVersions_should_bind_method_and_dates() {
        when(addSchoolRubricVersionsUseCase.execute(any())).thenReturn(List.of(UUID.randomUUID()));

        MvcTestResult result = mvc.post().uri(SCHOOL_VERSIONS_URI, UUID.randomUUID(), UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON)
            .content(versionBody("\"totalScoreMethod\": \"weighted_average\",", "\"effectiveFrom\": \"2026-08-01T01:00:00Z\""))
            .exchange();

        assertThat(result).hasStatusOk();
        ArgumentCaptor<AddSchoolRubricVersionsCommand> command = ArgumentCaptor.forClass(AddSchoolRubricVersionsCommand.class);
        verify(addSchoolRubricVersionsUseCase).execute(command.capture());
        AddSchoolRubricVersionsCommand.RubricVersionItemCommand item = command.getValue().versions().getFirst();
        assertThat(item.totalScoreMethod()).isEqualTo(RubricTotalScoreMethod.WEIGHTED_AVERAGE);
        assertThat(item.effectiveFrom()).isEqualTo(Instant.parse("2026-08-01T01:00:00Z"));
    }

    @Test
    @WithAnonymousUser
    void addSystemVersions_should_answer_401_for_anonymous() {
        assertThat(postSystemVersions(versionBody("\"totalScoreMethod\": \"SUM\"", ""))).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void addSystemVersions_should_answer_403_for_wrong_role() {
        assertThat(postSystemVersions(versionBody("\"totalScoreMethod\": \"SUM\"", ""))).hasStatus(403);
    }

    private static String cloneBody(String totalScoreMethodJson, String effectiveFrom) {
        return "{\"sourceRubricVersionId\": \"" + UUID.randomUUID() + "\", \"code\": \"ENG-K10\", \"name\": \"English\","
            + totalScoreMethodJson + "\"policies\": [{\"sourcePolicyId\": \"" + UUID.randomUUID()
            + "\", \"effectiveFrom\": \"" + effectiveFrom + "\"}]}";
    }

    private MvcTestResult postClone(String body) {
        return mvc.post().uri(CLONE_URI, UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void clone_should_bind_method_and_policy_effective_instant() {
        when(cloneSystemRubricToSchoolUseCase.execute(any())).thenReturn(UUID.randomUUID());

        MvcTestResult result = postClone(cloneBody("\"totalScoreMethod\": \"weighted_average\",", "2026-08-01T08:00:00+07:00"));

        assertThat(result).hasStatusOk();
        ArgumentCaptor<CloneSystemRubricToSchoolCommand> command = ArgumentCaptor.forClass(CloneSystemRubricToSchoolCommand.class);
        verify(cloneSystemRubricToSchoolUseCase).execute(command.capture());
        assertThat(command.getValue().totalScoreMethod()).isEqualTo(RubricTotalScoreMethod.WEIGHTED_AVERAGE);
        assertThat(command.getValue().policies().getFirst().effectiveFrom()).isEqualTo(Instant.parse("2026-08-01T01:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void clone_should_keep_method_null_when_omitted() {
        when(cloneSystemRubricToSchoolUseCase.execute(any())).thenReturn(UUID.randomUUID());

        MvcTestResult result = postClone(cloneBody("", "2026-08-01T01:00:00Z"));

        assertThat(result).hasStatusOk();
        ArgumentCaptor<CloneSystemRubricToSchoolCommand> command = ArgumentCaptor.forClass(CloneSystemRubricToSchoolCommand.class);
        verify(cloneSystemRubricToSchoolUseCase).execute(command.capture());
        assertThat(command.getValue().totalScoreMethod()).isNull();
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void clone_should_answer_400_for_unknown_method() {
        MvcTestResult result = postClone(cloneBody("\"totalScoreMethod\": \"MEDIAN\",", "2026-08-01T01:00:00Z"));

        assertThat(result).hasStatus(400).bodyText().contains("totalScoreMethod", "SUM");
        verifyNoInteractions(cloneSystemRubricToSchoolUseCase);
    }

    @Test
    @WithAnonymousUser
    void clone_should_answer_401_for_anonymous() {
        assertThat(postClone(cloneBody("", "2026-08-01T01:00:00Z"))).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void clone_should_answer_403_for_wrong_role() {
        assertThat(postClone(cloneBody("", "2026-08-01T01:00:00Z"))).hasStatus(403);
    }
}

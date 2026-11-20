package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.command.UpdateSystemRubricVersionCommand;
import com.sep.vox.application.port.input.query.ViewSystemRubricVersionsQuery;
import com.sep.vox.application.port.input.usecase.rubricschool.SearchSchoolRubricCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.SearchSchoolRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.SearchSchoolRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.SearchSchoolRubricsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.UpdateSchoolRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.UpdateSchoolRubricResultBandUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.UpdateSchoolRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.UpdateSchoolRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricCriterionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricDetailsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricResultBandDetailsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricVersionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricschool.ViewSchoolRubricsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.SearchSystemRubricCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.SearchSystemRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.SearchSystemRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.SearchSystemRubricsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.UpdateSystemRubricCriterionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.UpdateSystemRubricResultBandUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.UpdateSystemRubricUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.UpdateSystemRubricVersionUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricCriterionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricDetailsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricResultBandDetailsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricResultBandsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricVersionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricVersionsUseCase;
import com.sep.vox.application.port.input.usecase.rubricsystem.ViewSystemRubricsUseCase;
import com.sep.vox.application.port.input.usecase.rubricteacher.SearchTeacherRubricsUseCase;
import com.sep.vox.application.port.input.usecase.rubricteacher.ViewTeacherRubricVersionsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RubricCriterionDto;
import com.sep.vox.domain.dto.RubricResultBandDto;
import com.sep.vox.domain.dto.RubricVersionDto;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.support.GraphQlControllerTest;

/**
 * Sends rubric operations through the real schema and asserts the typed values: the {@code DateTime}
 * scalar and the {@code RubricStatus} / {@code RubricTotalScoreMethod} enums, on output and on input.
 */
@GraphQlControllerTest(RubricController.class)
@MockitoBean(types = {
    SearchSchoolRubricCriteriaUseCase.class, SearchSchoolRubricResultBandsUseCase.class, SearchSchoolRubricVersionsUseCase.class,
    SearchSchoolRubricsUseCase.class, SearchSystemRubricCriteriaUseCase.class, SearchSystemRubricResultBandsUseCase.class,
    SearchSystemRubricVersionsUseCase.class, SearchSystemRubricsUseCase.class, SearchTeacherRubricsUseCase.class,
    UpdateSchoolRubricCriterionUseCase.class, UpdateSchoolRubricResultBandUseCase.class, UpdateSchoolRubricUseCase.class,
    UpdateSchoolRubricVersionUseCase.class, UpdateSystemRubricCriterionUseCase.class, UpdateSystemRubricResultBandUseCase.class,
    UpdateSystemRubricUseCase.class, ViewSchoolRubricCriteriaUseCase.class, ViewSchoolRubricCriterionDetailsUseCase.class,
    ViewSchoolRubricDetailsUseCase.class, ViewSchoolRubricResultBandDetailsUseCase.class, ViewSchoolRubricResultBandsUseCase.class,
    ViewSchoolRubricVersionDetailsUseCase.class, ViewSchoolRubricVersionsUseCase.class, ViewSchoolRubricsUseCase.class,
    ViewSystemRubricCriterionDetailsUseCase.class, ViewSystemRubricDetailsUseCase.class, ViewSystemRubricResultBandDetailsUseCase.class,
    ViewSystemRubricVersionDetailsUseCase.class, ViewSystemRubricsUseCase.class, ViewTeacherRubricVersionsUseCase.class })
class RubricGraphQlTests {

    private static final UUID RUBRIC_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-28T02:30:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewSystemRubricVersionsUseCase viewSystemRubricVersionsUseCase;
    @MockitoBean
    private UpdateSystemRubricVersionUseCase updateSystemRubricVersionUseCase;
    @MockitoBean
    private ViewSystemRubricCriteriaUseCase viewSystemRubricCriteriaUseCase;
    @MockitoBean
    private ViewSystemRubricResultBandsUseCase viewSystemRubricResultBandsUseCase;

    private static RubricVersionDto version() {
        return new RubricVersionDto(VERSION_ID, RUBRIC_ID, 1, "V1", "Version 1", null, RubricStatus.PUBLISHED,
            Instant.parse("2026-09-01T00:00:00Z"), null, BigDecimal.ZERO, BigDecimal.TEN,
            RubricTotalScoreMethod.WEIGHTED_AVERAGE, CREATED_AT, null);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void rubric_versions_should_return_enums_and_date_time_values() {
        when(viewSystemRubricVersionsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(version()), 1, 10, 1L, 1));

        graphQlTester.document("{ viewSystemRubricVersions(rubricId: \"%s\") { content { status totalScoreMethod effectiveFrom effectiveTo createdAt } } }".formatted(RUBRIC_ID))
            .execute()
            .errors().verify()
            .path("viewSystemRubricVersions.content[0].status").entity(String.class).isEqualTo("PUBLISHED")
            .path("viewSystemRubricVersions.content[0].totalScoreMethod").entity(String.class).isEqualTo("WEIGHTED_AVERAGE")
            .path("viewSystemRubricVersions.content[0].effectiveFrom").entity(String.class).isEqualTo("2026-09-01T00:00:00Z")
            .path("viewSystemRubricVersions.content[0].effectiveTo").valueIsNull()
            .path("viewSystemRubricVersions.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void rubric_versions_should_bind_the_status_variable_to_the_enum() {
        when(viewSystemRubricVersionsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 1, 10, 0L, 0));

        graphQlTester.document("query($status: RubricStatus) { viewSystemRubricVersions(rubricId: \"%s\", status: $status) { totalElements } }".formatted(RUBRIC_ID))
            .variable("status", "ARCHIVED").execute()
            .errors().verify();

        verify(viewSystemRubricVersionsUseCase).execute(new ViewSystemRubricVersionsQuery(RUBRIC_ID, RubricStatus.ARCHIVED, 1, 10));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void rubric_versions_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ viewSystemRubricVersions(rubricId: \"%s\", status: NOT_A_STATUS) { totalElements } }".formatted(RUBRIC_ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewSystemRubricVersionsUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void rubric_criteria_and_result_bands_should_return_created_and_updated_at_as_date_time() {
        RubricCriterionDto criterion = new RubricCriterionDto(UUID.randomUUID(), VERSION_ID, UUID.randomUUID(), "C1", "Fluency",
            null, null, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, 1, true, CREATED_AT, null, null, null);
        RubricResultBandDto band = new RubricResultBandDto(UUID.randomUUID(), VERSION_ID, "B1", "Band 1", null,
            BigDecimal.ZERO, BigDecimal.TEN, 1, CREATED_AT, null, null, null);
        when(viewSystemRubricCriteriaUseCase.execute(any())).thenReturn(new PageResult<>(List.of(criterion), 1, 10, 1L, 1));
        when(viewSystemRubricResultBandsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(band), 1, 10, 1L, 1));

        graphQlTester.document("{ viewSystemRubricCriteria(versionId: \"%s\") { content { createdAt updatedAt } } viewSystemRubricResultBands(versionId: \"%s\") { content { createdAt updatedAt } } }".formatted(VERSION_ID, VERSION_ID))
            .execute()
            .errors().verify()
            .path("viewSystemRubricCriteria.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("viewSystemRubricCriteria.content[0].updatedAt").valueIsNull()
            .path("viewSystemRubricResultBands.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("viewSystemRubricResultBands.content[0].updatedAt").valueIsNull();
    }

    private static final String UPDATE_VERSION_MUTATION = """
        mutation($effectiveFrom: DateTime, $method: RubricTotalScoreMethod) {
            updateSystemRubricVersion(versionId: "%s",
                input: { effectiveFrom: $effectiveFrom, totalScoreMethod: $method })
        }""".formatted(VERSION_ID);

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_version_should_deliver_the_date_time_and_enum_variables_typed() {
        when(updateSystemRubricVersionUseCase.execute(any())).thenReturn(VERSION_ID);

        graphQlTester.document(UPDATE_VERSION_MUTATION)
            .variable("effectiveFrom", "2026-09-01T00:00:00+07:00").variable("method", "SUM").execute()
            .errors().verify();

        verify(updateSystemRubricVersionUseCase).execute(new UpdateSystemRubricVersionCommand(VERSION_ID, null, null,
            Instant.parse("2026-08-31T17:00:00Z"), null, null, null, RubricTotalScoreMethod.SUM));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_version_should_reject_a_date_without_an_offset() {
        graphQlTester.document(UPDATE_VERSION_MUTATION)
            .variable("effectiveFrom", "2026-09-01").variable("method", "SUM").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("effectiveFrom"))
            .verify();

        verifyNoInteractions(updateSystemRubricVersionUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_version_should_reject_an_unknown_total_score_method() {
        graphQlTester.document(UPDATE_VERSION_MUTATION)
            .variable("effectiveFrom", null).variable("method", "AVERAGE").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("method"))
            .verify();

        verifyNoInteractions(updateSystemRubricVersionUseCase);
    }
}

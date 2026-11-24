package com.sep.vox.interfaces.graphql.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.command.framework.UpdateFrameworkVersionCommand;
import com.sep.vox.application.port.input.query.framework.ViewFrameworkVersionsQuery;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkCriterionBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkCriterionUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkResultBandUseCase;
import com.sep.vox.application.port.input.usecase.framework.UpdateFrameworkVersionUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewActiveFrameworksUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkCriterionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworkVersionsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewFrameworksUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewSchoolFrameworkCriteriaUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewSchoolFrameworkVersionDetailsUseCase;
import com.sep.vox.application.port.input.usecase.framework.ViewSchoolFrameworkVersionsUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.FrameworkDto;
import com.sep.vox.domain.dto.FrameworkVersionDto;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends framework operations through the real schema and asserts the typed values: the {@code DateTime}
 * scalar and the {@code FrameworkVersionStatus} enum, on output and on input.
 */
@GraphQLControllerTest(GraphQLFrameworkController.class)
@MockitoBean(types = {
    ViewFrameworkDetailsUseCase.class, ViewFrameworkVersionDetailsUseCase.class,
    UpdateFrameworkCriterionUseCase.class, UpdateFrameworkCriterionBandUseCase.class,
    UpdateFrameworkResultBandUseCase.class, ViewFrameworkCriteriaUseCase.class,
    ViewFrameworkCriterionDetailsUseCase.class, ViewSchoolFrameworkCriteriaUseCase.class,
    ViewActiveFrameworksUseCase.class, ViewSchoolFrameworkVersionsUseCase.class,
    ViewSchoolFrameworkVersionDetailsUseCase.class })
class FrameworkGraphQLTests {

    private static final UUID FRAMEWORK_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-28T02:30:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewFrameworksUseCase viewFrameworksUseCase;
    @MockitoBean
    private ViewFrameworkVersionsUseCase viewFrameworkVersionsUseCase;
    @MockitoBean
    private UpdateFrameworkVersionUseCase updateFrameworkVersionUseCase;

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void frameworks_should_return_created_and_updated_at_as_date_time() {
        FrameworkDto framework = new FrameworkDto(FRAMEWORK_ID, "CEFR", "CEFR", null, true, CREATED_AT, null);
        when(viewFrameworksUseCase.execute(any())).thenReturn(new PageResult<>(List.of(framework), 1, 20, 1L, 1));

        graphQlTester.document("{ frameworks { content { createdAt updatedAt } } }").execute()
            .errors().verify()
            .path("frameworks.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("frameworks.content[0].updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void framework_versions_should_return_dates_and_status_and_bind_the_status_variable() {
        FrameworkVersionDto version = new FrameworkVersionDto(VERSION_ID, FRAMEWORK_ID, "V1", "Version 1", null, 1,
            Instant.parse("2026-09-01T00:00:00Z"), null, FrameworkVersionStatus.PUBLISHED, CREATED_AT, null,
            List.of(), List.of());
        when(viewFrameworkVersionsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(version), 1, 20, 1L, 1));

        graphQlTester.document("query($status: FrameworkVersionStatus) { frameworkVersions(frameworkId: \"%s\", status: $status) { content { effectiveFrom effectiveTo status createdAt } } }".formatted(FRAMEWORK_ID))
            .variable("status", "PUBLISHED").execute()
            .errors().verify()
            .path("frameworkVersions.content[0].effectiveFrom").entity(String.class).isEqualTo("2026-09-01T00:00:00Z")
            .path("frameworkVersions.content[0].effectiveTo").valueIsNull()
            .path("frameworkVersions.content[0].status").entity(String.class).isEqualTo("PUBLISHED")
            .path("frameworkVersions.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");

        verify(viewFrameworkVersionsUseCase).execute(
            new ViewFrameworkVersionsQuery(FRAMEWORK_ID, FrameworkVersionStatus.PUBLISHED, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void framework_versions_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ frameworkVersions(frameworkId: \"%s\", status: NOT_A_STATUS) { totalElements } }".formatted(FRAMEWORK_ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewFrameworkVersionsUseCase);
    }

    private static final String UPDATE_VERSION_MUTATION = """
        mutation($effectiveFrom: DateTime!, $effectiveTo: DateTime) {
            updateFrameworkVersion(frameworkId: "%s", versionId: "%s",
                input: { code: "V1", name: "Version 1", effectiveFrom: $effectiveFrom, effectiveTo: $effectiveTo })
        }""".formatted(FRAMEWORK_ID, VERSION_ID);

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_framework_version_should_deliver_date_time_variables_as_instants() {
        when(updateFrameworkVersionUseCase.execute(any())).thenReturn(VERSION_ID);

        graphQlTester.document(UPDATE_VERSION_MUTATION)
            .variable("effectiveFrom", "2026-09-01T00:00:00+07:00").variable("effectiveTo", "2027-06-01T00:00:00Z")
            .execute().errors().verify();

        verify(updateFrameworkVersionUseCase).execute(new UpdateFrameworkVersionCommand(FRAMEWORK_ID, VERSION_ID,
            "V1", "Version 1", null, Instant.parse("2026-08-31T17:00:00Z"), Instant.parse("2027-06-01T00:00:00Z")));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_framework_version_should_reject_a_date_without_an_offset() {
        graphQlTester.document(UPDATE_VERSION_MUTATION)
            .variable("effectiveFrom", "2026-09-01").variable("effectiveTo", null)
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("effectiveFrom"))
            .verify();

        verifyNoInteractions(updateFrameworkVersionUseCase);
    }
}

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

import com.sep.vox.application.port.input.query.assessmentblueprint.ViewAssessmentBlueprintsQuery;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.ViewAssessmentBlueprintDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentblueprint.ViewAssessmentBlueprintsUseCase;
import com.sep.vox.domain.dto.AssessmentBlueprintDto;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersion;
import com.sep.vox.domain.model.assessment.AssessmentBlueprintVersionStatus;
import com.sep.vox.domain.repository.AssessmentBlueprintSectionRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintSlotRepository;
import com.sep.vox.domain.repository.AssessmentBlueprintVersionRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends blueprint queries through the real schema and asserts the typed values: the {@code DateTime}
 * scalar and the {@code AssessmentBlueprintVersionStatus} enum, on the blueprint and its versions.
 */
@GraphQLControllerTest(GraphQLAssessmentBlueprintController.class)
@MockitoBean(types = { AssessmentBlueprintSectionRepository.class,
    AssessmentBlueprintSlotRepository.class, QuestionRepository.class })
class AssessmentBlueprintGraphQLTests {

    private static final UUID BLUEPRINT_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-07-28T02:30:00Z");
    private static final Instant EFFECTIVE_FROM = Instant.parse("2026-08-01T01:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewAssessmentBlueprintsUseCase viewAssessmentBlueprintsUseCase;
    @MockitoBean
    private ViewAssessmentBlueprintDetailsUseCase viewAssessmentBlueprintDetailsUseCase;
    @MockitoBean
    private AssessmentBlueprintVersionRepository assessmentBlueprintVersionRepository;

    private AssessmentBlueprintDto blueprint() {
        return new AssessmentBlueprintDto(BLUEPRINT_ID, UUID.randomUUID(), UUID.randomUUID(), null, "BP-01", "Blueprint",
            null, true, CREATED_AT, null, null, null);
    }

    private AssessmentBlueprintVersion version(int number, AssessmentBlueprintVersionStatus status) {
        return AssessmentBlueprintVersion.builder().id(UUID.randomUUID()).blueprintId(BLUEPRINT_ID).version(number)
            .code("V" + number).status(status).totalTimeLimitSeconds(1800).effectiveFrom(EFFECTIVE_FROM)
            .createdAt(CREATED_AT).build();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void blueprint_should_return_date_time_values() {
        when(viewAssessmentBlueprintDetailsUseCase.execute(any())).thenReturn(blueprint());

        graphQlTester.document("{ assessmentBlueprint(id: \"%s\") { createdAt updatedAt } }".formatted(BLUEPRINT_ID)).execute()
            .errors().verify()
            .path("assessmentBlueprint.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("assessmentBlueprint.updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void blueprint_versions_should_keep_only_the_requested_status_enum() {
        when(viewAssessmentBlueprintDetailsUseCase.execute(any())).thenReturn(blueprint());
        when(assessmentBlueprintVersionRepository.findByBlueprintId(BLUEPRINT_ID)).thenReturn(List.of(
            version(1, AssessmentBlueprintVersionStatus.ARCHIVED), version(2, AssessmentBlueprintVersionStatus.PUBLISHED)));

        graphQlTester.document("""
            query($status: AssessmentBlueprintVersionStatus) {
                assessmentBlueprint(id: "%s") { versions(status: $status) { code status effectiveFrom effectiveTo createdAt } }
            }""".formatted(BLUEPRINT_ID)).variable("status", "PUBLISHED").execute()
            .errors().verify()
            .path("assessmentBlueprint.versions[*].code").entityList(String.class).containsExactly("V2")
            .path("assessmentBlueprint.versions[0].status").entity(String.class).isEqualTo("PUBLISHED")
            .path("assessmentBlueprint.versions[0].effectiveFrom").entity(String.class).isEqualTo("2026-08-01T01:00:00Z")
            .path("assessmentBlueprint.versions[0].effectiveTo").valueIsNull()
            .path("assessmentBlueprint.versions[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void blueprint_versions_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ assessmentBlueprint(id: \"%s\") { versions(status: NOT_A_STATUS) { code } } }".formatted(BLUEPRINT_ID))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewAssessmentBlueprintDetailsUseCase, assessmentBlueprintVersionRepository);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void blueprints_should_bind_the_assessment_kind_variable_as_an_enum() {
        when(viewAssessmentBlueprintsUseCase.execute(any())).thenReturn(new PageResult<>(List.of(blueprint()), 1, 20, 1L, 1));

        graphQlTester.document("""
            query($kind: AssessmentKind) { assessmentBlueprints(assessmentKind: $kind) { content { createdAt } } }""")
            .variable("kind", "CLASS_TEST").execute()
            .errors().verify()
            .path("assessmentBlueprints.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");

        verify(viewAssessmentBlueprintsUseCase).execute(
            new ViewAssessmentBlueprintsQuery(null, null, null, AssessmentKind.CLASS_TEST, null, 1, 20));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void blueprints_should_reject_an_unknown_assessment_kind_as_a_validation_error() {
        graphQlTester.document("{ assessmentBlueprints(assessmentKind: NOT_A_KIND) { totalElements } }").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_KIND"))
            .verify();

        verifyNoInteractions(viewAssessmentBlueprintsUseCase);
    }
}

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

import com.sep.vox.application.port.input.command.assessmentpolicysystem.UpdateAssessmentPolicyCommand;
import com.sep.vox.application.port.input.query.assessmentpolicysystem.ViewSystemAssessmentPoliciesQuery;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.UpdateSchoolAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.ViewSchoolAssessmentPoliciesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyschool.ViewSchoolAssessmentPolicyDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.UpdateSystemAssessmentPolicyUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.ViewSystemAssessmentPoliciesUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicysystem.ViewSystemAssessmentPolicyDetailsUseCase;
import com.sep.vox.application.port.input.usecase.assessmentpolicyteacher.ViewTeacherAssessmentPoliciesUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.AssessmentPolicyDto;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStatus;
import com.sep.vox.domain.model.assessmentpolicy.AssessmentPolicyStrictness;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends assessment policy operations through the real schema and asserts the typed values: the
 * {@code DateTime} scalar (strict ISO-8601 with an offset) and the {@code AssessmentPolicyStatus} enum.
 */
@GraphQLControllerTest(GraphQLAssessmentPolicyController.class)
@MockitoBean(types = {
    UpdateSchoolAssessmentPolicyUseCase.class, ViewSchoolAssessmentPoliciesUseCase.class,
    ViewTeacherAssessmentPoliciesUseCase.class, ViewSystemAssessmentPolicyDetailsUseCase.class,
    ViewSchoolAssessmentPolicyDetailsUseCase.class })
class AssessmentPolicyGraphQLTests {

    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID BAND_ID = UUID.randomUUID();
    private static final Instant EFFECTIVE_FROM = Instant.parse("2026-07-27T17:00:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewSystemAssessmentPoliciesUseCase viewSystemAssessmentPoliciesUseCase;
    @MockitoBean
    private UpdateSystemAssessmentPolicyUseCase updateSystemAssessmentPolicyUseCase;

    private static final String VIEW_POLICIES_QUERY = """
        query($status: AssessmentPolicyStatus, $effectiveFrom: DateTime) {
            viewSystemAssessmentPolicies(status: $status, effectiveFrom: $effectiveFrom) {
                content { status strictness effectiveFrom effectiveTo createdAt updatedAt }
            }
        }""";

    private static final String UPDATE_POLICY_MUTATION = """
        mutation($effectiveFrom: DateTime!, $effectiveTo: DateTime) {
            updateSystemAssessmentPolicy(policyId: "%s",
                input: { targetFrameworkBandId: "%s", effectiveFrom: $effectiveFrom, effectiveTo: $effectiveTo })
        }""".formatted(POLICY_ID, BAND_ID);

    private static final String UPDATE_POLICY_REQUIRED_FIELDS_ONLY_MUTATION = """
        mutation($effectiveFrom: DateTime!) {
            updateSystemAssessmentPolicy(policyId: "%s",
                input: { targetFrameworkBandId: "%s", effectiveFrom: $effectiveFrom })
        }""".formatted(POLICY_ID, BAND_ID);

    private static final String UPDATE_POLICY_ALL_FIELDS_MUTATION = """
        mutation($passingScore: Float, $strictness: AssessmentPolicyStrictness, $effectiveFrom: DateTime!, $effectiveTo: DateTime) {
            updateSystemAssessmentPolicy(policyId: "%s",
                input: { targetFrameworkBandId: "%s", passingScore: $passingScore, strictness: $strictness,
                         effectiveFrom: $effectiveFrom, effectiveTo: $effectiveTo })
        }""".formatted(POLICY_ID, BAND_ID);

    private AssessmentPolicyDto policy() {
        return new AssessmentPolicyDto(POLICY_ID, null, null, null, null, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), BAND_ID, new BigDecimal("6.5"), "STANDARD", 1, AssessmentPolicyStatus.PUBLISHED,
            EFFECTIVE_FROM, null, Instant.parse("2026-07-28T02:30:00Z"), null);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void view_system_policies_should_return_status_enum_and_date_time_values() {
        when(viewSystemAssessmentPoliciesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(policy()), 1, 10, 1L, 1));

        graphQlTester.document(VIEW_POLICIES_QUERY).execute()
            .errors().verify()
            .path("viewSystemAssessmentPolicies.content[0].status").entity(String.class).isEqualTo("PUBLISHED")
            .path("viewSystemAssessmentPolicies.content[0].strictness").entity(String.class).isEqualTo("STANDARD")
            .path("viewSystemAssessmentPolicies.content[0].effectiveFrom").entity(String.class).isEqualTo("2026-07-27T17:00:00Z")
            .path("viewSystemAssessmentPolicies.content[0].effectiveTo").valueIsNull()
            .path("viewSystemAssessmentPolicies.content[0].createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void view_system_policies_should_bind_status_and_offset_date_time_variables() {
        when(viewSystemAssessmentPoliciesUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 1, 10, 0L, 0));

        graphQlTester.document(VIEW_POLICIES_QUERY)
            .variable("status", "ARCHIVED").variable("effectiveFrom", "2026-07-28T00:00:00+07:00").execute()
            .errors().verify();

        verify(viewSystemAssessmentPoliciesUseCase).execute(
            new ViewSystemAssessmentPoliciesQuery(AssessmentPolicyStatus.ARCHIVED, null, null, EFFECTIVE_FROM, null, 1, 10));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void view_system_policies_should_reject_a_date_without_an_offset() {
        graphQlTester.document(VIEW_POLICIES_QUERY).variable("effectiveFrom", "2026-07-28").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("effectiveFrom"))
            .verify();

        verifyNoInteractions(viewSystemAssessmentPoliciesUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void view_system_policies_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ viewSystemAssessmentPolicies(status: NOT_A_STATUS) { totalElements } }").execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_STATUS"))
            .verify();

        verifyNoInteractions(viewSystemAssessmentPoliciesUseCase);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_system_policy_should_accept_an_offset_date_time_and_deliver_an_instant() {
        when(updateSystemAssessmentPolicyUseCase.execute(any())).thenReturn(POLICY_ID);

        graphQlTester.document(UPDATE_POLICY_MUTATION)
            .variable("effectiveFrom", "2026-07-28T00:00:00+07:00").variable("effectiveTo", null)
            .execute().errors().verify();

        verify(updateSystemAssessmentPolicyUseCase).execute(
            new UpdateAssessmentPolicyCommand(null, POLICY_ID, BAND_ID, null, false, null, false, EFFECTIVE_FROM, null, true));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_system_policy_should_keep_passing_score_strictness_and_effective_to_when_omitted() {
        when(updateSystemAssessmentPolicyUseCase.execute(any())).thenReturn(POLICY_ID);

        graphQlTester.document(UPDATE_POLICY_REQUIRED_FIELDS_ONLY_MUTATION)
            .variable("effectiveFrom", "2026-07-28T00:00:00+07:00")
            .execute().errors().verify();

        verify(updateSystemAssessmentPolicyUseCase).execute(
            new UpdateAssessmentPolicyCommand(null, POLICY_ID, BAND_ID, null, false, null, false, EFFECTIVE_FROM, null, false));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_system_policy_should_mark_explicit_null_and_given_values_as_provided() {
        when(updateSystemAssessmentPolicyUseCase.execute(any())).thenReturn(POLICY_ID);

        graphQlTester.document(UPDATE_POLICY_ALL_FIELDS_MUTATION)
            .variable("passingScore", null).variable("strictness", "STRICT")
            .variable("effectiveFrom", "2026-07-28T00:00:00+07:00").variable("effectiveTo", null)
            .execute().errors().verify();

        verify(updateSystemAssessmentPolicyUseCase).execute(new UpdateAssessmentPolicyCommand(
            null, POLICY_ID, BAND_ID, null, true, AssessmentPolicyStrictness.STRICT, true, EFFECTIVE_FROM, null, true));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_system_policy_should_reject_a_date_without_an_offset() {
        graphQlTester.document(UPDATE_POLICY_MUTATION).variable("effectiveFrom", "2026-07-28").variable("effectiveTo", null)
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("effectiveFrom"))
            .verify();

        verifyNoInteractions(updateSystemAssessmentPolicyUseCase);
    }
}

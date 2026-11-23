package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.mapper.scoringrule.ScoringRuleParamsMapper;
import com.sep.vox.application.port.input.command.scoringrule.UpdateScoringRuleCommand;
import com.sep.vox.application.port.input.usecase.scoringrule.SearchSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.SearchSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.UpdateSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.UpdateSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewAllSchoolScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewAllSystemScoringRuleUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewSchoolScoringRuleDetailUseCase;
import com.sep.vox.application.port.input.usecase.scoringrule.ViewSystemScoringRuleDetailUseCase;
import com.sep.vox.application.support.FakeJsonSerializationPort;
import com.sep.vox.domain.dto.ScoringRuleDto;
import com.sep.vox.domain.model.scoringrule.ScoringRuleActionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleConditionType;
import com.sep.vox.domain.model.scoringrule.ScoringRuleSeverity;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends scoring rule operations through the real schema and asserts the typed values: the
 * {@code ScoringRuleConditionType}, {@code ScoringRuleActionType} and {@code ScoringRuleSeverity} enums and the
 * {@code DateTime} scalar, on output and on input.
 */
@GraphQLControllerTest(GraphQLScoringRuleController.class)
class ScoringRuleGraphQLTests {

    private static final UUID POLICY_ID = UUID.randomUUID();
    private static final UUID RULE_ID = UUID.randomUUID();

    @TestConfiguration
    static class ScoringRuleParamsMapperConfiguration {

        @Bean
        ScoringRuleParamsMapper scoringRuleParamsMapper() {
            return new ScoringRuleParamsMapper(new FakeJsonSerializationPort());
        }
    }

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private SearchSystemScoringRuleUseCase searchSystemScoringRuleUseCase;
    @MockitoBean
    private SearchSchoolScoringRuleUseCase searchSchoolScoringRuleUseCase;
    @MockitoBean
    private ViewAllSystemScoringRuleUseCase viewAllSystemScoringRuleUseCase;
    @MockitoBean
    private ViewAllSchoolScoringRuleUseCase viewAllSchoolScoringRuleUseCase;
    @MockitoBean
    private ViewSystemScoringRuleDetailUseCase viewSystemScoringRuleDetailUseCase;
    @MockitoBean
    private ViewSchoolScoringRuleDetailUseCase viewSchoolScoringRuleDetailUseCase;
    @MockitoBean
    private UpdateSystemScoringRuleUseCase updateSystemScoringRuleUseCase;
    @MockitoBean
    private UpdateSchoolScoringRuleUseCase updateSchoolScoringRuleUseCase;

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void rule_details_should_return_enums_and_date_time_values() {
        when(viewSystemScoringRuleDetailUseCase.execute(any())).thenReturn(rule());

        graphQlTester.document("""
                { viewSystemScoringRuleDetails(policyId: "%s", ruleId: "%s") {
                    conditionType actionType severity conditionParamsJson createdAt updatedAt } }
                """.formatted(POLICY_ID, RULE_ID))
            .execute()
            .errors().verify()
            .path("viewSystemScoringRuleDetails.conditionType").entity(String.class).isEqualTo("DURATION_LESS_THAN")
            .path("viewSystemScoringRuleDetails.actionType").entity(String.class).isEqualTo("CAP_FINAL_SCORE")
            .path("viewSystemScoringRuleDetails.severity").entity(String.class).isEqualTo("WARNING")
            .path("viewSystemScoringRuleDetails.conditionParamsJson").entity(String.class).satisfies(
                json -> assertThat(json).contains("seconds"))
            .path("viewSystemScoringRuleDetails.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("viewSystemScoringRuleDetails.updatedAt").valueIsNull();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_should_bind_the_enum_input_fields_into_the_command() {
        when(updateSystemScoringRuleUseCase.execute(any())).thenReturn(RULE_ID);

        graphQlTester.document("""
                mutation($input: UpdateScoringRuleInput!) {
                    updateSystemScoringRule(policyId: "%s", ruleId: "%s", input: $input)
                }""".formatted(POLICY_ID, RULE_ID))
            .variable("input", updateInput("DURATION_LESS_THAN", "CAP_FINAL_SCORE", "BLOCKING"))
            .execute()
            .errors().verify()
            .path("updateSystemScoringRule").entity(String.class).isEqualTo(RULE_ID.toString());

        ArgumentCaptor<UpdateScoringRuleCommand> command = ArgumentCaptor.forClass(UpdateScoringRuleCommand.class);
        verify(updateSystemScoringRuleUseCase).execute(command.capture());
        assertThat(command.getValue().conditionType()).isEqualTo(ScoringRuleConditionType.DURATION_LESS_THAN);
        assertThat(command.getValue().actionType()).isEqualTo(ScoringRuleActionType.CAP_FINAL_SCORE);
        assertThat(command.getValue().severity()).isEqualTo(ScoringRuleSeverity.BLOCKING);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_should_reject_an_unknown_severity_as_a_validation_error() {
        graphQlTester.document("""
                mutation($input: UpdateScoringRuleInput!) {
                    updateSystemScoringRule(policyId: "%s", ruleId: "%s", input: $input)
                }""".formatted(POLICY_ID, RULE_ID))
            .variable("input", updateInput("DURATION_LESS_THAN", "CAP_FINAL_SCORE", "NOT_A_SEVERITY"))
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("NOT_A_SEVERITY"))
            .verify();

        verifyNoInteractions(updateSystemScoringRuleUseCase);
    }

    private static Map<String, Object> updateInput(String conditionType, String actionType, String severity) {
        return Map.of(
            "name", "Short answer",
            "conditionType", conditionType,
            "conditionParamsJson", "{\"seconds\": 3}",
            "actionType", actionType,
            "actionParamsJson", "{\"maxScore\": 0}",
            "priority", 1,
            "severity", severity,
            "stopProcessing", false,
            "isActive", true);
    }

    private static ScoringRuleDto rule() {
        return new ScoringRuleDto(RULE_ID, POLICY_ID, "R1", "Short answer", null,
            ScoringRuleConditionType.DURATION_LESS_THAN, Map.of("seconds", 3), ScoringRuleActionType.CAP_FINAL_SCORE,
            Map.of("maxScore", 0), 1, ScoringRuleSeverity.WARNING, false, true,
            Instant.parse("2026-07-28T02:30:00Z"), null);
    }
}

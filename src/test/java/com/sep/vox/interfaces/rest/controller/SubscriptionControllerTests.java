package com.sep.vox.interfaces.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.sep.vox.application.port.input.command.AllocateAssessmentQuotaCommand;
import com.sep.vox.application.port.input.command.AllocatePracticeQuotaCommand;
import com.sep.vox.application.port.input.command.CreateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.command.CreateSubscriptionPlanQuotaCommand;
import com.sep.vox.application.port.input.command.FundQuotaFromBalanceCommand;
import com.sep.vox.application.port.input.command.SetQuotaDistributionPolicyCommand;
import com.sep.vox.application.port.input.usecase.subscription.AllocateAssessmentQuotaToTeachersUseCase;
import com.sep.vox.application.port.input.usecase.subscription.AllocatePracticeQuotaToStudentsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ArchiveSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CancelSchoolSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreateSubscriptionPlanReplacementUseCase;
import com.sep.vox.application.port.input.usecase.subscription.CreateSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.DeleteDraftSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ForceSuspendSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.FundQuotaFromBalanceUseCase;
import com.sep.vox.application.port.input.usecase.subscription.PublishSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.SetQuotaDistributionPolicyUseCase;
import com.sep.vox.application.port.input.usecase.subscription.UnsuspendSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.UpdateSubscriptionPlanReplacementUseCase;
import com.sep.vox.domain.common.DistributionMode;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.support.RestControllerTest;

@RestControllerTest(SubscriptionController.class)
@WithMockUser(roles = "SYSTEM_ADMIN")
@MockitoBean(types = {
    CreateSubscriptionPlanReplacementUseCase.class, ArchiveSubscriptionPlanUseCase.class,
    UpdateSubscriptionPlanReplacementUseCase.class, PublishSubscriptionPlanUseCase.class,
    DeleteDraftSubscriptionPlanUseCase.class, CancelSchoolSubscriptionUseCase.class,
    ForceSuspendSubscriptionUseCase.class, UnsuspendSubscriptionUseCase.class
})
class SubscriptionControllerTests {

    private static final String SUBSCRIPTIONS_URI = "/api/v1/subscriptions";
    private static final String FUNDING_URI = SUBSCRIPTIONS_URI + "/schools/{schoolId}/quota-funding/{quotaType}";
    private static final String POLICY_URI = SUBSCRIPTIONS_URI + "/schools/{schoolId}/quota-policies/{quotaType}";
    private static final String TEACHER_QUOTA_URI = SUBSCRIPTIONS_URI + "/schools/{schoolId}/teachers/assessment-quota";
    private static final String STUDENT_QUOTA_URI = SUBSCRIPTIONS_URI + "/schools/{schoolId}/students/practice-quota";
    private static final String FUNDING_BODY = "{\"amountVnd\": 500000}";
    private static final String POLICY_BODY = "{\"distributableRatio\": 0.8}";

    private final UUID schoolId = UUID.randomUUID();

    @Autowired
    private MockMvcTester mvc;
    @MockitoBean
    private CreateSubscriptionPlanUseCase createSubscriptionPlanUseCase;
    @MockitoBean
    private FundQuotaFromBalanceUseCase fundQuotaFromBalanceUseCase;
    @MockitoBean
    private SetQuotaDistributionPolicyUseCase setQuotaDistributionPolicyUseCase;
    @MockitoBean
    private AllocateAssessmentQuotaToTeachersUseCase allocateAssessmentQuotaToTeachersUseCase;
    @MockitoBean
    private AllocatePracticeQuotaToStudentsUseCase allocatePracticeQuotaToStudentsUseCase;

    private String planBody(String periodType, String quotaType) {
        String period = periodType == null ? "" : "\"periodType\": \"%s\",".formatted(periodType);
        String quota = quotaType == null ? "" : "\"quotaType\": \"%s\",".formatted(quotaType);
        return """
            {"name": "Basic", "tagline": "Basic plan", "priceVnd": 100000, %s
             "periodCount": 1, "maxTimePerAttemptMin": 30,
             "quotas": [{%s "includedAmountVnd": 50000}]}""".formatted(period, quota);
    }

    private MvcTestResult createPlan(String body) {
        return mvc.post().uri(SUBSCRIPTIONS_URI).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    @Test
    void create_plan_should_pass_typed_period_and_quota_type_to_the_command() {
        UUID planId = UUID.randomUUID();
        CreateSubscriptionPlanCommand expectedCommand = new CreateSubscriptionPlanCommand(
            "Basic", "Basic plan", BigDecimal.valueOf(100000), SubscriptionPlanPeriod.MONTH, 1, 30,
            List.of(new CreateSubscriptionPlanQuotaCommand(QuotaType.ASSESSMENT, BigDecimal.valueOf(50000))));
        when(createSubscriptionPlanUseCase.execute(expectedCommand)).thenReturn(planId);

        MvcTestResult result = createPlan(planBody("MONTH", "ASSESSMENT"));

        assertThat(result).hasStatus(201).bodyJson().extractingPath("$.data").isEqualTo(planId.toString());
    }

    @Test
    void create_plan_should_accept_lower_case_enum_values() {
        MvcTestResult result = createPlan(planBody("month", "practice"));

        assertThat(result).hasStatus(201);
        verify(createSubscriptionPlanUseCase).execute(new CreateSubscriptionPlanCommand(
            "Basic", "Basic plan", BigDecimal.valueOf(100000), SubscriptionPlanPeriod.MONTH, 1, 30,
            List.of(new CreateSubscriptionPlanQuotaCommand(QuotaType.PRACTICE, BigDecimal.valueOf(50000)))));
    }

    @Test
    void create_plan_should_answer_400_for_unknown_period_type_listing_accepted_values() {
        MvcTestResult result = createPlan(planBody("WEEK", "ASSESSMENT"));

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.periodType").asString()
            .contains("DAY", "MONTH", "YEAR").doesNotContain("com.sep");
        verifyNoInteractions(createSubscriptionPlanUseCase);
    }

    @Test
    void create_plan_should_answer_400_for_unknown_quota_type_listing_accepted_values() {
        MvcTestResult result = createPlan(planBody("MONTH", "SPEAKING"));

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors['quotas[0].quotaType']").asString()
            .contains("ASSESSMENT", "PRACTICE").doesNotContain("com.sep");
        verifyNoInteractions(createSubscriptionPlanUseCase);
    }

    @Test
    void create_plan_should_answer_400_when_period_type_or_quota_type_is_missing() {
        assertThat(createPlan(planBody(null, "ASSESSMENT"))).hasStatus(400).bodyJson()
            .extractingPath("$.errors.periodType").isNotNull();
        assertThat(createPlan(planBody("MONTH", null))).hasStatus(400);
        verifyNoInteractions(createSubscriptionPlanUseCase);
    }

    @Test
    @WithAnonymousUser
    void create_plan_should_answer_401_for_anonymous() {
        assertThat(createPlan(planBody("MONTH", "ASSESSMENT"))).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void create_plan_should_answer_403_for_a_teacher() {
        assertThat(createPlan(planBody("MONTH", "ASSESSMENT"))).hasStatus(403);
        verifyNoInteractions(createSubscriptionPlanUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void fund_quota_should_pass_typed_quota_type_from_the_path() {
        FundQuotaFromBalanceCommand expectedCommand =
            new FundQuotaFromBalanceCommand(schoolId, QuotaType.PRACTICE, BigDecimal.valueOf(500000), null);

        MvcTestResult result = postJson(FUNDING_URI, FUNDING_BODY, "practice");

        assertThat(result).hasStatus(200);
        verify(fundQuotaFromBalanceUseCase).execute(expectedCommand);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void fund_quota_should_answer_400_for_an_unknown_quota_type_in_the_path() {
        MvcTestResult result = postJson(FUNDING_URI, FUNDING_BODY, "NOPE");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.quotaType").asString()
            .contains("ASSESSMENT", "PRACTICE").doesNotContain("com.sep");
        verifyNoInteractions(fundQuotaFromBalanceUseCase);
    }

    @Test
    @WithAnonymousUser
    void fund_quota_should_answer_401_for_anonymous() {
        assertThat(postJson(FUNDING_URI, FUNDING_BODY, "PRACTICE")).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void fund_quota_should_answer_403_for_a_teacher() {
        assertThat(postJson(FUNDING_URI, FUNDING_BODY, "PRACTICE")).hasStatus(403);
        verifyNoInteractions(fundQuotaFromBalanceUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void set_policy_should_pass_typed_quota_type_from_the_path() {
        SetQuotaDistributionPolicyCommand expectedCommand =
            new SetQuotaDistributionPolicyCommand(schoolId, QuotaType.ASSESSMENT, new BigDecimal("0.8"));
        when(setQuotaDistributionPolicyUseCase.execute(expectedCommand)).thenReturn(new BigDecimal("0.8"));

        MvcTestResult result = putJson(POLICY_URI, POLICY_BODY, "ASSESSMENT");

        assertThat(result).hasStatus(200);
        verify(setQuotaDistributionPolicyUseCase).execute(expectedCommand);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void set_policy_should_answer_400_for_an_unknown_quota_type_in_the_path() {
        MvcTestResult result = putJson(POLICY_URI, POLICY_BODY, "NOPE");

        assertThat(result).hasStatus(400).bodyJson().extractingPath("$.errors.quotaType").asString()
            .contains("ASSESSMENT", "PRACTICE");
        verifyNoInteractions(setQuotaDistributionPolicyUseCase);
    }

    @Test
    @WithAnonymousUser
    void set_policy_should_answer_401_for_anonymous() {
        assertThat(putJson(POLICY_URI, POLICY_BODY, "PRACTICE")).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void set_policy_should_answer_403_for_a_student() {
        assertThat(putJson(POLICY_URI, POLICY_BODY, "PRACTICE")).hasStatus(403);
        verifyNoInteractions(setQuotaDistributionPolicyUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void allocate_assessment_quota_should_pass_typed_mode_to_the_command() {
        MvcTestResult result = putAllocation(TEACHER_QUOTA_URI, "{\"mode\": \"auto\"}");

        assertThat(result).hasStatus(200);
        verify(allocateAssessmentQuotaToTeachersUseCase)
            .execute(new AllocateAssessmentQuotaCommand(schoolId, DistributionMode.AUTO, List.of(), false));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void allocate_practice_quota_should_pass_typed_mode_to_the_command() {
        MvcTestResult result = putAllocation(STUDENT_QUOTA_URI, "{\"mode\": \"MANUAL\", \"confirmWalletDraw\": true}");

        assertThat(result).hasStatus(200);
        verify(allocatePracticeQuotaToStudentsUseCase)
            .execute(new AllocatePracticeQuotaCommand(schoolId, DistributionMode.MANUAL, List.of(), true));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void allocate_quota_should_answer_400_listing_modes_for_an_unknown_mode() {
        for (String uri : new String[] {TEACHER_QUOTA_URI, STUDENT_QUOTA_URI}) {
            assertThat(putAllocation(uri, "{\"mode\": \"AUTOMATIC\"}")).hasStatus(400).bodyJson()
                .extractingPath("$.errors.mode").asString().contains("AUTO", "MANUAL").doesNotContain("com.sep");
            assertThat(putAllocation(uri, "{}")).hasStatus(400).bodyJson().extractingPath("$.errors.mode").isNotNull();
        }
        verifyNoInteractions(allocateAssessmentQuotaToTeachersUseCase, allocatePracticeQuotaToStudentsUseCase);
    }

    @Test
    @WithAnonymousUser
    void allocate_quota_should_answer_401_for_anonymous() {
        assertThat(putAllocation(TEACHER_QUOTA_URI, "{\"mode\": \"AUTO\"}")).hasStatus(401);
        assertThat(putAllocation(STUDENT_QUOTA_URI, "{\"mode\": \"AUTO\"}")).hasStatus(401);
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void allocate_quota_should_answer_403_for_a_teacher() {
        assertThat(putAllocation(TEACHER_QUOTA_URI, "{\"mode\": \"AUTO\"}")).hasStatus(403);
        assertThat(putAllocation(STUDENT_QUOTA_URI, "{\"mode\": \"AUTO\"}")).hasStatus(403);
        verifyNoInteractions(allocateAssessmentQuotaToTeachersUseCase, allocatePracticeQuotaToStudentsUseCase);
    }

    private MvcTestResult putAllocation(String uri, String body) {
        return mvc.put().uri(uri, schoolId).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult postJson(String uri, String body, String quotaType) {
        return mvc.post().uri(uri, schoolId, quotaType).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult putJson(String uri, String body, String quotaType) {
        return mvc.put().uri(uri, schoolId, quotaType).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }
}

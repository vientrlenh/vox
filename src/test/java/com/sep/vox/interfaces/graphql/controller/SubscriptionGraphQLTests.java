package com.sep.vox.interfaces.graphql.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sep.vox.application.port.input.command.subscription.UpdateSubscriptionPlanCommand;
import com.sep.vox.application.port.input.query.subscription.ViewSchoolSubscriptionsQuery;
import com.sep.vox.application.port.input.usecase.subscription.PreviewSchoolSubscriptionRenewalUseCase;
import com.sep.vox.application.port.input.usecase.subscription.UpdateSubscriptionPlanUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewAssessmentQuotaUserAllocationsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyAssessmentQuotaAllocationUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewMyPracticeQuotaAllocationUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewPracticeQuotaUserAllocationsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionHistoryUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionQuotaRecordsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSchoolSubscriptionsUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionPlanDetailUseCase;
import com.sep.vox.application.port.input.usecase.subscription.ViewSubscriptionPlansUseCase;
import com.sep.vox.application.port.output.QuotaPricingPort;
import com.sep.vox.application.response.input.subscription.SchoolSubscriptionRenewalPreviewResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.dto.SubscriptionPlanDto;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.domain.model.metering.QuotaType;
import com.sep.vox.domain.model.subscription.SubscriptionPlanPeriod;
import com.sep.vox.domain.model.subscription.SubscriptionPlanStatus;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends subscription queries through the real schema and asserts the typed values: the
 * {@code SubscriptionPlanStatus}, {@code SubscriptionPlanPeriod} and {@code SchoolSubscriptionStatus}
 * enums and the {@code DateTime} scalar. Money stays {@code Float}.
 */
@GraphQLControllerTest(GraphQLSubscriptionController.class)
class SubscriptionGraphQLTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-07-28T02:30:00Z");
    private static final Instant END = Instant.parse("2027-07-28T02:30:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewSubscriptionPlansUseCase viewSubscriptionPlansUseCase;
    @MockitoBean
    private ViewSubscriptionPlanDetailUseCase viewSubscriptionPlanDetailUseCase;
    @MockitoBean
    private UpdateSubscriptionPlanUseCase updateSubscriptionPlanUseCase;
    @MockitoBean
    private ViewSchoolSubscriptionsUseCase viewSchoolSubscriptionsUseCase;
    @MockitoBean
    private ViewSchoolSubscriptionQuotaRecordsUseCase viewSchoolSubscriptionQuotaRecordsUseCase;
    @MockitoBean
    private ViewMyAssessmentQuotaAllocationUseCase viewMyAssessmentQuotaAllocationUseCase;
    @MockitoBean
    private PreviewSchoolSubscriptionRenewalUseCase previewSchoolSubscriptionRenewalUseCase;
    @MockitoBean
    private ViewMyPracticeQuotaAllocationUseCase viewMyPracticeQuotaAllocationUseCase;
    @MockitoBean
    private ViewSchoolSubscriptionUseCase viewSchoolSubscriptionUseCase;
    @MockitoBean
    private ViewSchoolSubscriptionHistoryUseCase viewSchoolSubscriptionHistoryUseCase;
    @MockitoBean
    private ViewAssessmentQuotaUserAllocationsUseCase viewAssessmentQuotaUserAllocationsUseCase;
    @MockitoBean
    private ViewPracticeQuotaUserAllocationsUseCase viewPracticeQuotaUserAllocationsUseCase;
    @MockitoBean
    private QuotaPricingPort quotaPricingPort;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_subscription_should_return_status_enum_and_date_time_values() {
        when(viewSchoolSubscriptionUseCase.execute(any())).thenReturn(subscription());

        graphQlTester.document("""
                { schoolSubscription(schoolId: "%s") {
                    status startDate endDate cancelledAt createdAt suspendedAt pricePaidSnapshot
                } }""".formatted(SCHOOL_ID))
            .execute()
            .errors().verify()
            .path("schoolSubscription.status").entity(String.class).isEqualTo("ACTIVE")
            .path("schoolSubscription.startDate").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("schoolSubscription.endDate").entity(String.class).isEqualTo("2027-07-28T02:30:00Z")
            .path("schoolSubscription.cancelledAt").valueIsNull()
            .path("schoolSubscription.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("schoolSubscription.suspendedAt").valueIsNull()
            .path("schoolSubscription.pricePaidSnapshot").entity(Double.class).isEqualTo(1000000.0);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void subscription_plan_should_return_enums_and_date_time_values() {
        when(viewSubscriptionPlanDetailUseCase.execute(any())).thenReturn(plan());

        graphQlTester.document("""
                { subscriptionPlan(id: "%s") { periodType status createdAt updatedAt priceVnd } }""".formatted(PLAN_ID))
            .execute()
            .errors().verify()
            .path("subscriptionPlan.periodType").entity(String.class).isEqualTo("MONTH")
            .path("subscriptionPlan.status").entity(String.class).isEqualTo("ACTIVE")
            .path("subscriptionPlan.createdAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z")
            .path("subscriptionPlan.updatedAt").valueIsNull()
            .path("subscriptionPlan.priceVnd").entity(Double.class).isEqualTo(1000000.0);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void renewal_preview_should_return_starts_at_as_date_time() {
        when(previewSchoolSubscriptionRenewalUseCase.execute(any())).thenReturn(
            new SchoolSubscriptionRenewalPreviewResponse(false, plan(), plan(), END, new BigDecimal("1000000")));

        graphQlTester.document("""
                { schoolSubscriptionRenewalPreview { planChanged startsAt amountDue currentPlan { status } } }""")
            .execute()
            .errors().verify()
            .path("schoolSubscriptionRenewalPreview.startsAt").entity(String.class).isEqualTo("2027-07-28T02:30:00Z")
            .path("schoolSubscriptionRenewalPreview.currentPlan.status").entity(String.class).isEqualTo("ACTIVE")
            .path("schoolSubscriptionRenewalPreview.amountDue").entity(Double.class).isEqualTo(1000000.0);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void school_subscriptions_should_bind_the_status_enum_variable() {
        when(viewSchoolSubscriptionsUseCase.execute(any())).thenReturn(
            new PageResult<>(List.of(subscription()), 1, 20, 1L, 1));

        graphQlTester.document("""
                query($status: SchoolSubscriptionStatus) {
                    schoolSubscriptions(status: $status) { content { status } }
                }""")
            .variable("status", "ACTIVE")
            .execute()
            .errors().verify()
            .path("schoolSubscriptions.content[0].status").entity(String.class).isEqualTo("ACTIVE");

        verify(viewSchoolSubscriptionsUseCase).execute(
            new ViewSchoolSubscriptionsQuery(null, null, SchoolSubscriptionStatus.ACTIVE, 1, 20));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void school_subscriptions_should_reject_an_unknown_status_as_a_validation_error() {
        graphQlTester.document("{ schoolSubscriptions(status: DRAFT) { totalElements } }")
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("DRAFT"))
            .verify();

        verifyNoInteractions(viewSchoolSubscriptionsUseCase);
    }

    private static SchoolSubscriptionDto subscription() {
        return new SchoolSubscriptionDto(UUID.randomUUID(), SCHOOL_ID, PLAN_ID, START, END,
            SchoolSubscriptionStatus.ACTIVE, new BigDecimal("1000000"), null, START, null, null, null);
    }

    private UpdateSubscriptionPlanCommand sendUpdateAndCaptureCommand(String inputFields) {
        when(updateSubscriptionPlanUseCase.execute(any())).thenReturn(PLAN_ID);
        graphQlTester.document("mutation { updateSubscriptionPlan(id: \"%s\", input: { %s }) }".formatted(PLAN_ID, inputFields))
            .execute().errors().verify();
        ArgumentCaptor<UpdateSubscriptionPlanCommand> command = ArgumentCaptor.forClass(UpdateSubscriptionPlanCommand.class);
        verify(updateSubscriptionPlanUseCase).execute(command.capture());
        return command.getValue();
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_plan_should_leave_everything_alone_for_an_empty_input() {
        UpdateSubscriptionPlanCommand command = sendUpdateAndCaptureCommand("");

        assertThat(command).isEqualTo(new UpdateSubscriptionPlanCommand(PLAN_ID, null, null, null, null, null, null));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_plan_should_treat_explicit_nulls_as_not_given() {
        UpdateSubscriptionPlanCommand command = sendUpdateAndCaptureCommand(
            "name: null, tagline: null, priceVnd: null, periodCount: null, maxTimePerAttemptMin: null, quotas: null");

        assertThat(command).isEqualTo(new UpdateSubscriptionPlanCommand(PLAN_ID, null, null, null, null, null, null));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_plan_should_deliver_values_and_flatten_the_quotas() {
        UpdateSubscriptionPlanCommand command = sendUpdateAndCaptureCommand(
            "name: \"Pro\", periodCount: 3, quotas: [{ quotaType: PRACTICE, includedAmountVnd: 5000 }]");

        assertThat(command.name()).isEqualTo("Pro");
        assertThat(command.periodCount()).isEqualTo(3);
        assertThat(command.quotas()).extracting(c -> c.quotaType())
            .containsExactly(QuotaType.PRACTICE);
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void update_plan_should_pass_an_empty_quota_list_unchanged() {
        UpdateSubscriptionPlanCommand command = sendUpdateAndCaptureCommand("quotas: []");

        assertThat(command.quotas()).isNotNull().isEmpty();
    }

    private static SubscriptionPlanDto plan() {
        return new SubscriptionPlanDto(PLAN_ID, "Standard", null, new BigDecimal("1000000"),
            SubscriptionPlanPeriod.MONTH, 12, 30, SubscriptionPlanStatus.ACTIVE, 1L, START, null, null, null, null);
    }
}

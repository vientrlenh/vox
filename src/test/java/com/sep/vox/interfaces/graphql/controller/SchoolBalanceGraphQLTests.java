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

import com.sep.vox.application.port.input.query.balance.ViewSchoolBalanceSummaryQuery;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceEntriesUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceSummaryUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolDebtEventsUseCase;
import com.sep.vox.application.response.input.balance.SchoolBalanceSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolBalanceDto;
import com.sep.vox.domain.dto.SchoolBalanceEntryDto;
import com.sep.vox.domain.dto.SchoolDebtEventDto;
import com.sep.vox.support.GraphQLControllerTest;

/**
 * Sends school balance queries through the real schema and asserts the {@code DateTime} scalar on
 * output and on the {@code from}/{@code to} variables. Money stays {@code String}.
 */
@GraphQLControllerTest(GraphQLSchoolBalanceController.class)
class SchoolBalanceGraphQLTests {

    private static final UUID SCHOOL_ID = UUID.randomUUID();
    private static final Instant OCCURRED_AT = Instant.parse("2026-07-28T02:30:00Z");

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private ViewSchoolBalanceUseCase viewSchoolBalanceUseCase;
    @MockitoBean
    private ViewSchoolBalanceEntriesUseCase viewSchoolBalanceEntriesUseCase;
    @MockitoBean
    private ViewSchoolBalanceSummaryUseCase viewSchoolBalanceSummaryUseCase;
    @MockitoBean
    private ViewSchoolDebtEventsUseCase viewSchoolDebtEventsUseCase;

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_balance_should_return_date_time_and_keep_money_as_text() {
        when(viewSchoolBalanceUseCase.execute(any())).thenReturn(
            new SchoolBalanceDto(SCHOOL_ID, "-128440.950000", true, OCCURRED_AT));

        graphQlTester.document("{ schoolBalance(schoolId: \"%s\") { balanceVnd locked updatedAt } }".formatted(SCHOOL_ID))
            .execute()
            .errors().verify()
            .path("schoolBalance.balanceVnd").entity(String.class).isEqualTo("-128440.950000")
            .path("schoolBalance.locked").entity(Boolean.class).isEqualTo(true)
            .path("schoolBalance.updatedAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_balance_entries_should_return_entry_type_and_date_time() {
        when(viewSchoolBalanceEntriesUseCase.execute(any())).thenReturn(
            new PageResult<>(List.of(entry()), 1, 20, 1L, 1));

        graphQlTester.document("""
                { schoolBalanceEntries(schoolId: "%s", entryType: TOP_UP) {
                    content { entryType amountVnd occurredAt }
                } }""".formatted(SCHOOL_ID))
            .execute()
            .errors().verify()
            .path("schoolBalanceEntries.content[0].entryType").entity(String.class).isEqualTo("TOP_UP")
            .path("schoolBalanceEntries.content[0].amountVnd").entity(String.class).isEqualTo("100000.000000")
            .path("schoolBalanceEntries.content[0].occurredAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_balance_summary_should_bind_date_time_variables() {
        when(viewSchoolBalanceSummaryUseCase.execute(any())).thenReturn(
            new SchoolBalanceSummaryResponse("1.000000", "0", "0", "0"));

        graphQlTester.document("""
                query($from: DateTime, $to: DateTime) {
                    schoolBalanceSummary(schoolId: "%s", from: $from, to: $to) { creditedVnd }
                }""".formatted(SCHOOL_ID))
            .variable("from", "2026-07-01T00:00:00Z")
            .variable("to", "2026-08-01T07:00:00+07:00")
            .execute()
            .errors().verify()
            .path("schoolBalanceSummary.creditedVnd").entity(String.class).isEqualTo("1.000000");

        verify(viewSchoolBalanceSummaryUseCase).execute(new ViewSchoolBalanceSummaryQuery(
            SCHOOL_ID, Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z")));
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_balance_summary_should_reject_a_date_without_an_offset_as_a_coercion_error() {
        graphQlTester.document("""
                query($from: DateTime) {
                    schoolBalanceSummary(schoolId: "%s", from: $from) { creditedVnd }
                }""".formatted(SCHOOL_ID))
            .variable("from", "2026-07-01")
            .execute()
            .errors().expect(error -> error.getMessage() != null && error.getMessage().contains("from"))
            .verify();

        verifyNoInteractions(viewSchoolBalanceSummaryUseCase);
    }

    @Test
    @WithMockUser(roles = "SCHOOL_ADMIN")
    void school_debt_events_should_return_date_time() {
        when(viewSchoolDebtEventsUseCase.execute(any())).thenReturn(new PageResult<>(
            List.of(new SchoolDebtEventDto(UUID.randomUUID(), SCHOOL_ID, UUID.randomUUID(), "LOCKED", "ASSESSMENT",
                null, null, "1.000000", "2.000000", "3.000000", "0.500000", OCCURRED_AT)), 1, 20, 1L, 1));

        graphQlTester.document("{ schoolDebtEvents(schoolId: \"%s\") { content { eventType overageVnd occurredAt } } }"
                .formatted(SCHOOL_ID))
            .execute()
            .errors().verify()
            .path("schoolDebtEvents.content[0].eventType").entity(String.class).isEqualTo("LOCKED")
            .path("schoolDebtEvents.content[0].overageVnd").entity(String.class).isEqualTo("0.500000")
            .path("schoolDebtEvents.content[0].occurredAt").entity(String.class).isEqualTo("2026-07-28T02:30:00Z");
    }

    private static SchoolBalanceEntryDto entry() {
        return new SchoolBalanceEntryDto(UUID.randomUUID(), SCHOOL_ID, "TOP_UP", "100000.000000", "100000.000000",
            OCCURRED_AT, null, UUID.randomUUID(), null, null, null, null, null, null, null);
    }
}

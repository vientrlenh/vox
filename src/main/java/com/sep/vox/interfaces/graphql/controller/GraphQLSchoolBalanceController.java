package com.sep.vox.interfaces.graphql.controller;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.balance.ViewSchoolBalanceEntriesQuery;
import com.sep.vox.application.port.input.query.balance.ViewSchoolBalanceSummaryQuery;
import com.sep.vox.application.port.input.query.balance.ViewSchoolDebtEventsQuery;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceEntriesUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceSummaryUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolDebtEventsUseCase;
import com.sep.vox.application.response.input.balance.SchoolBalanceSummaryResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolBalanceDto;
import com.sep.vox.domain.dto.SchoolBalanceEntryDto;
import com.sep.vox.domain.dto.SchoolDebtEventDto;
import com.sep.vox.interfaces.shared.PageArguments;

/**
 * The school's self-top-up wallet: the balance, the statement, the summary, and the debt log.
 *
 * <p>The debt log is here and not in GraphQLSchoolController because debt is the flip side of the wallet itself
 * -- a negative balance IS the debt, and {@code SchoolSubscriptionDebtGuardService} derives the lock
 * state straight from the sign of the balance. The two ledgers answer two questions about the same
 * wallet: the statement says "where did the money go", the debt log says "why is the school locked".
 *
 * <p>There is NO {@code @PreAuthorize} on the methods: the permission here is not a role but the
 * relationship between the caller and the {@code schoolId} they ask for. {@code hasRole('SYSTEM_ADMIN')}
 * would block the school's own admin, while leaving it out would let one school read another's wallet.
 * The check lives in the use case, through {@code SchoolScopedReadGuard} -- the place that knows both
 * sides.
 */
@Controller
@RequiredArgsConstructor
public class GraphQLSchoolBalanceController {

    private final ViewSchoolBalanceUseCase viewSchoolBalanceUseCase;
    private final ViewSchoolBalanceEntriesUseCase viewSchoolBalanceEntriesUseCase;
    private final ViewSchoolBalanceSummaryUseCase viewSchoolBalanceSummaryUseCase;
    private final ViewSchoolDebtEventsUseCase viewSchoolDebtEventsUseCase;

    @QueryMapping(name = "schoolBalance")
    public SchoolBalanceDto schoolBalance(@Argument(name = "schoolId") UUID schoolId) {
        return viewSchoolBalanceUseCase.execute(schoolId);
    }

    @QueryMapping(name = "schoolBalanceEntries")
    public PageResult<SchoolBalanceEntryDto> schoolBalanceEntries(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "entryType") String entryType,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSchoolBalanceEntriesUseCase.execute(
            new ViewSchoolBalanceEntriesQuery(schoolId, entryType, null, null, page, size));
    }

    @QueryMapping(name = "schoolBalanceSummary")
    public SchoolBalanceSummaryResponse schoolBalanceSummary(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "from") Instant from,
            @Argument(name = "to") Instant to) {
        return viewSchoolBalanceSummaryUseCase.execute(new ViewSchoolBalanceSummaryQuery(schoolId, from, to));
    }

    @QueryMapping(name = "schoolDebtEvents")
    public PageResult<SchoolDebtEventDto> schoolDebtEvents(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        PageArguments.validate(page, size);
        return viewSchoolDebtEventsUseCase.execute(new ViewSchoolDebtEventsQuery(schoolId, page, size));
    }
}

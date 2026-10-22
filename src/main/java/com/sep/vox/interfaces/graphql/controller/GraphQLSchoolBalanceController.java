package com.sep.vox.interfaces.graphql.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.sep.vox.application.port.input.query.ViewSchoolBalanceEntriesQuery;
import com.sep.vox.application.port.input.query.ViewSchoolBalanceSummaryQuery;
import com.sep.vox.application.port.input.query.ViewSchoolDebtEventsQuery;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceEntriesUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceSummaryUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolBalanceUseCase;
import com.sep.vox.application.port.input.usecase.balance.ViewSchoolDebtEventsUseCase;
import com.sep.vox.application.response.input.balance.SchoolBalanceSummaryResponse;
import com.sep.vox.application.shared.AppPageRequest;
import com.sep.vox.domain.dto.SchoolBalanceDto;
import com.sep.vox.domain.dto.SchoolBalanceEntryDto;
import com.sep.vox.domain.dto.SchoolDebtEventDto;
import com.sep.vox.domain.shared.PageResult;

import lombok.RequiredArgsConstructor;

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
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewSchoolBalanceEntriesUseCase.execute(
            new ViewSchoolBalanceEntriesQuery(schoolId, entryType, null, null, pageRequest));
    }

    @QueryMapping(name = "schoolBalanceSummary")
    public SchoolBalanceSummaryResponse schoolBalanceSummary(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "from") String from,
            @Argument(name = "to") String to) {
        return viewSchoolBalanceSummaryUseCase.execute(
            new ViewSchoolBalanceSummaryQuery(schoolId, parseInstant(from, "from"), parseInstant(to, "to")));
    }

    @QueryMapping(name = "schoolDebtEvents")
    public PageResult<SchoolDebtEventDto> schoolDebtEvents(
            @Argument(name = "schoolId") UUID schoolId,
            @Argument(name = "page") Integer page,
            @Argument(name = "size") Integer size) {
        AppPageRequest pageRequest = AppPageRequest.pageRequest(page, size);
        return viewSchoolDebtEventsUseCase.execute(new ViewSchoolDebtEventsQuery(schoolId, pageRequest));
    }

    /**
     * Mốc thời gian đi vào dưới dạng String vì schema chưa có scalar ngày giờ nào -- cùng lý do như
     * mọi field {@code occurredAt} đang trả ra. Bắt lỗi ở đây để một chuỗi sai định dạng ra thành câu
     * tiếng Việt đọc được, thay vì một DateTimeParseException lọt lên tận tầng GraphQL.
     */
    private static Instant parseInstant(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Mốc thời gian '" + field + "' phải theo định dạng ISO-8601, ví dụ 2026-08-01T00:00:00Z");
        }
    }
}

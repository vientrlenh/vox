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
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolBalanceDto;
import com.sep.vox.domain.dto.SchoolBalanceEntryDto;
import com.sep.vox.domain.dto.SchoolDebtEventDto;

/**
 * Ví tiền tự nạp của trường: số dư, sao kê, tổng hợp, và nhật ký nợ.
 *
 * <p>Nhật ký nợ ở đây chứ không ở SchoolController vì nợ là mặt trái của chính cái ví -- số dư âm
 * CHÍNH LÀ khoản nợ, và {@code SchoolSubscriptionDebtGuardService} suy trạng thái khoá thẳng từ dấu
 * của số dư. Hai sổ trả lời hai câu hỏi về cùng một ví: sao kê nói "tiền đi đâu", nhật ký nợ nói
 * "vì sao trường bị khoá".
 *
 * <p>KHÔNG có {@code @PreAuthorize} trên các method: quyền ở đây không phải một vai trò mà là quan hệ
 * giữa người gọi và {@code schoolId} họ hỏi. {@code hasRole('SYSTEM_ADMIN')} sẽ chặn mất chính quản
 * trị trường, còn bỏ trống thì trường này đọc được ví của trường kia. Phép kiểm nằm trong use case,
 * qua {@code SchoolScopedReadGuard} -- nơi biết cả hai vế.
 */
@Controller("graphqlSchoolBalanceController")
public class SchoolBalanceController {

    private final ViewSchoolBalanceUseCase viewSchoolBalanceUseCase;
    private final ViewSchoolBalanceEntriesUseCase viewSchoolBalanceEntriesUseCase;
    private final ViewSchoolBalanceSummaryUseCase viewSchoolBalanceSummaryUseCase;
    private final ViewSchoolDebtEventsUseCase viewSchoolDebtEventsUseCase;

    public SchoolBalanceController(
            ViewSchoolBalanceUseCase viewSchoolBalanceUseCase,
            ViewSchoolBalanceEntriesUseCase viewSchoolBalanceEntriesUseCase,
            ViewSchoolBalanceSummaryUseCase viewSchoolBalanceSummaryUseCase,
            ViewSchoolDebtEventsUseCase viewSchoolDebtEventsUseCase) {
        this.viewSchoolBalanceUseCase = viewSchoolBalanceUseCase;
        this.viewSchoolBalanceEntriesUseCase = viewSchoolBalanceEntriesUseCase;
        this.viewSchoolBalanceSummaryUseCase = viewSchoolBalanceSummaryUseCase;
        this.viewSchoolDebtEventsUseCase = viewSchoolDebtEventsUseCase;
    }

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
        validatePaging(page, size);
        return viewSchoolBalanceEntriesUseCase.execute(
            new ViewSchoolBalanceEntriesQuery(schoolId, entryType, null, null, page, size));
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
        validatePaging(page, size);
        return viewSchoolDebtEventsUseCase.execute(new ViewSchoolDebtEventsQuery(schoolId, page, size));
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

    // page ĐẾM TỪ 1 theo quy ước chung của dự án -- adapter trừ 1 trước khi xuống PageRequest.
    private static void validatePaging(Integer page, Integer size) {
        if (page == null || page < 1) {
            throw new IllegalArgumentException("Số trang phải lớn hơn hoặc bằng 1");
        }
        if (size == null || size <= 0) {
            throw new IllegalArgumentException("Kích thước trang phải lớn hơn 0");
        }
    }
}

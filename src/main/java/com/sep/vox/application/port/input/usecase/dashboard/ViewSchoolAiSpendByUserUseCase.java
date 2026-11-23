package com.sep.vox.application.port.input.usecase.dashboard;

import com.sep.vox.application.query.dto.UserAiSpendDto;
import com.sep.vox.domain.common.PageResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.dashboard.SearchSchoolAiSpendByUserQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SchoolAiCostQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolAiSpendByUserPageResponse;
import com.sep.vox.domain.common.DecimalText;

/** Bảng "ai đang tiêu hạn mức", đứng ngay dưới biểu đồ chi phí AI và đọc cùng một cửa sổ. */
@Service
@RequiredArgsConstructor
public class ViewSchoolAiSpendByUserUseCase
        implements IUseCase<SearchSchoolAiSpendByUserQuery, SchoolAiSpendByUserPageResponse> {

    private static final int DEFAULT_WINDOW_DAYS = 30;

    private final UserContextPort userContextPort;
    private final SchoolAiCostQueryRepository schoolAiCostQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolAiSpendByUserPageResponse execute(SearchSchoolAiSpendByUserQuery input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();
        Instant to = input.to() == null ? Instant.now() : input.to();
        Instant from = input.from() == null ? to.minus(DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS) : input.from();
        String quotaType = input.quotaType() == null ? null : input.quotaType().name();

        PageResult<UserAiSpendDto> page = schoolAiCostQueryRepository.findSpendByUser(
            schoolId, from, to, quotaType, input.page(), input.size());
        BigDecimal schoolWide = schoolAiCostQueryRepository.sumSchoolWideCost(schoolId, from, to, quotaType);

        return new SchoolAiSpendByUserPageResponse(
            page.content().stream()
                .map(row -> new SchoolAiSpendByUserPageResponse.UserAiSpendResponse(
                    row.userId(),
                    row.fullName(),
                    row.quotaType(),
                    DecimalText.of(row.spentVnd()),
                    DecimalText.of(row.allocatedAmountVnd())))
                .toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            DecimalText.orZero(schoolWide)
        );
    }
}

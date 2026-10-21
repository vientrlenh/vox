package com.sep.vox.application.port.input.usecase.dashboard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.SearchSchoolAiSpendByUserQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.SchoolAiCostQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolAiSpendByUserPageResponse;
import com.sep.vox.domain.shared.DecimalText;


@Service
public class ViewSchoolAiSpendByUserUseCase
        implements IUseCase<SearchSchoolAiSpendByUserQuery, SchoolAiSpendByUserPageResponse> {

    private static final int DEFAULT_WINDOW_DAYS = 30;

    private final UserContextPort userContextPort;
    private final SchoolAiCostQueryRepository schoolAiCostQueryRepository;

    public ViewSchoolAiSpendByUserUseCase(UserContextPort userContextPort,
            SchoolAiCostQueryRepository schoolAiCostQueryRepository) {
        this.userContextPort = userContextPort;
        this.schoolAiCostQueryRepository = schoolAiCostQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolAiSpendByUserPageResponse execute(SearchSchoolAiSpendByUserQuery input) {
        UUID schoolId = userContextPort.getCurrentSchoolId()
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        var to = input.to() == null ? Instant.now() : input.to();
        var from = input.from() == null ? to.minus(DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS) : input.from();
        var quotaType = input.quotaType() == null ? null : input.quotaType().name();

        var page = schoolAiCostQueryRepository.findSpendByUser(
            schoolId, from, to, quotaType, input.page(), input.size());
        var schoolWide = schoolAiCostQueryRepository.sumSchoolWideCost(schoolId, from, to, quotaType);

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

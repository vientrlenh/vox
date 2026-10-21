package com.sep.vox.application.port.input.usecase.dashboard;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.SearchSchoolGradingFailuresQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailurePageResponse;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailureResponse;


@Service
public class ViewSchoolGradingFailuresUseCase
        implements IUseCase<SearchSchoolGradingFailuresQuery, SchoolGradingFailurePageResponse> {

    private final UserContextPort userContextPort;
    private final SchoolWorkloadQueryRepository schoolWorkloadQueryRepository;

    public ViewSchoolGradingFailuresUseCase(UserContextPort userContextPort,
            SchoolWorkloadQueryRepository schoolWorkloadQueryRepository) {
        this.userContextPort = userContextPort;
        this.schoolWorkloadQueryRepository = schoolWorkloadQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolGradingFailurePageResponse execute(SearchSchoolGradingFailuresQuery input) {
        UUID schoolId = userContextPort.getCurrentSchoolId()
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));

        var page = schoolWorkloadQueryRepository.findUnhandledAiFailures(
            schoolId, input.assessmentId(), input.retryLeft(), input.page(), input.size());
        var counts = schoolWorkloadQueryRepository.countAiFailuresByAllowance(schoolId, input.assessmentId());

        return new SchoolGradingFailurePageResponse(
            page.content().stream().map(SchoolGradingFailureResponse::of).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            counts[0],
            counts[1]
        );
    }
}

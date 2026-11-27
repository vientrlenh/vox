package com.sep.vox.application.port.input.usecase.dashboard;

import com.sep.vox.application.query.dto.SchoolGradingFailureDto;
import com.sep.vox.domain.common.PageResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.dashboard.SearchSchoolGradingFailuresQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.SchoolWorkloadQueryRepository;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailurePageResponse;
import com.sep.vox.application.response.input.dashboard.SchoolGradingFailureResponse;

/**
 * The landing place of the "AI grading failed, nobody has handled it" row on the overview page.
 *
 * <p>The scope comes from the LOGGED-IN USER; schoolId is not accepted from the client -- the same way as
 * {@code ViewSchoolAdminDashboardUseCase}. Accepting it from the client would open the way for one school's admin
 * to read another school's submissions.
 *
 * <p>There is NO bulk action here. Re-grading with AI and reassigning the grader are both decisions made PER
 * SUBMISSION -- they depend on the grading schedule, who is free, and the per-turn allowance of that very
 * session. The two individual mutations already exist and this screen only leads to them.
 */
@Service
@RequiredArgsConstructor
public class ViewSchoolGradingFailuresUseCase
        implements IUseCase<SearchSchoolGradingFailuresQuery, SchoolGradingFailurePageResponse> {

    private final UserContextPort userContextPort;
    private final SchoolWorkloadQueryRepository schoolWorkloadQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolGradingFailurePageResponse execute(SearchSchoolGradingFailuresQuery input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();

        PageResult<SchoolGradingFailureDto> page = schoolWorkloadQueryRepository.findUnhandledAiFailures(
            schoolId, input.assessmentId(), input.retryLeft(), input.page(), input.size());
        int[] counts = schoolWorkloadQueryRepository.countAiFailuresByAllowance(schoolId, input.assessmentId());

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

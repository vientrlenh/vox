package com.sep.vox.application.port.input.usecase.assessmentgrading;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.SearchGradingAssignmentsQuery;
import com.sep.vox.application.port.input.service.AssessmentGradingAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.GradingAssignmentFilter;
import com.sep.vox.application.query.dto.GradingAssignmentRowInfo;
import com.sep.vox.application.query.repository.AssessmentGradingQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.service.assessment.GradingScopeKind;

/**
 * The assignment table of the school admin. The scope is always locked to the caller's school, and
 * defaults to CENTRALIZED assessments.
 *
 * <p>A default rather than a hard lock: the same table also serves the screen where the school MONITORS the
 * grading progress of a class test (read-only, with an assessmentId already given). Hard-locking here
 * would break that screen; leaving it empty would mix the two kinds together again like the bug being fixed.
 */
@Service
public class ViewGradingAssignmentsUseCase
        implements IUseCase<SearchGradingAssignmentsQuery, PageResult<GradingAssignmentRowInfo>> {

    private final AssessmentGradingQueryRepository assessmentGradingQueryRepository;
    private final AssessmentGradingAccessService assessmentGradingAccessService;

    public ViewGradingAssignmentsUseCase(
            AssessmentGradingQueryRepository assessmentGradingQueryRepository,
            AssessmentGradingAccessService assessmentGradingAccessService) {
        this.assessmentGradingQueryRepository = assessmentGradingQueryRepository;
        this.assessmentGradingAccessService = assessmentGradingAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<GradingAssignmentRowInfo> execute(SearchGradingAssignmentsQuery input) {
        UUID currentUserId = assessmentGradingAccessService.requireActiveUserId();
        UUID schoolId = assessmentGradingAccessService.requireCurrentSchoolId(currentUserId);
        return assessmentGradingQueryRepository.searchAssignments(new GradingAssignmentFilter(
            schoolId,
            input.assessmentId(),
            input.scheduleId(),
            input.teacherId(),
            input.resultStatus(),
            input.roundType(),
            input.status(),
            input.unassignedOnly(),
            input.overdueOnly(),
            input.hasOpenAppeal(),
            input.search(),
            GradingScopeKind.orCentralized(input.kind())
        ), input.page(), input.size());
    }
}

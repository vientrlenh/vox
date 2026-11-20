package com.sep.vox.application.port.input.usecase.assessmentdirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.application.query.repository.AssessmentDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

/**
 * The students that can be added as candidates of an assessment. The scope follows exactly
 * {@link AssessmentDirectoryAccessService}: the whole school, or only the students in the caller's own class when it is a
 * class test.
 */
@Service
public class ViewAssessmentDirectoryStudentsUseCase
        implements IUseCase<ViewAssessmentDirectoryQuery, PageResult<AssessmentDirectoryUserInfo>> {

    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final AssessmentDirectoryQueryRepository assessmentDirectoryQueryRepository;

    public ViewAssessmentDirectoryStudentsUseCase(
            AssessmentDirectoryAccessService assessmentDirectoryAccessService,
            AssessmentDirectoryQueryRepository assessmentDirectoryQueryRepository) {
        this.assessmentDirectoryAccessService = assessmentDirectoryAccessService;
        this.assessmentDirectoryQueryRepository = assessmentDirectoryQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentDirectoryUserInfo> execute(ViewAssessmentDirectoryQuery input) {
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolveByAssessmentId(input.assessmentId());
        String search = StringNormalization.trimAndCollapseSpaces(input.search());

        if (scope.schoolWide()) {
            return assessmentDirectoryQueryRepository.findUsersBySchoolId(
                scope.schoolId(), SchoolRoleCodes.STUDENT, search, input.excludeUserIds(),
                input.page(), input.size());
        }
        return assessmentDirectoryQueryRepository.findUsersByClassIds(
            assessmentDirectoryAccessService.callerClassIds(scope),
            SchoolRoleCodes.STUDENT,
            search,
            input.excludeUserIds(),
            input.page(),
            input.size()
        );
    }
}

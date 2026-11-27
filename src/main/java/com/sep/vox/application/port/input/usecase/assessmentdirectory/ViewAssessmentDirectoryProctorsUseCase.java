package com.sep.vox.application.port.input.usecase.assessmentdirectory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.assessmentdirectory.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.application.query.repository.AssessmentDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.user.SchoolRoleCodes;

/**
 * The teachers who can be assigned as a proctor of an assessment schedule.
 *
 * <p>It does not branch on {@code schoolWide}: a proctor is always a teacher of the whole school.
 * This does not widen any permission -- the {@code schoolUsersForRequester} query already lets every
 * teacher list the teachers of the same school; this only saves the frontend from looking up {@code roleId} itself.
 */
@Service
@RequiredArgsConstructor
public class ViewAssessmentDirectoryProctorsUseCase
        implements IUseCase<ViewAssessmentDirectoryQuery, PageResult<AssessmentDirectoryUserInfo>> {

    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final AssessmentDirectoryQueryRepository assessmentDirectoryQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentDirectoryUserInfo> execute(ViewAssessmentDirectoryQuery input) {
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolveByAssessmentId(input.assessmentId());

        return assessmentDirectoryQueryRepository.findUsersBySchoolId(
            scope.schoolId(),
            SchoolRoleCodes.TEACHER,
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.excludeUserIds(),
            input.page(),
            input.size()
        );
    }
}

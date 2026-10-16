package com.sep.vox.application.port.input.usecase.assessmentdirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.AssessmentDirectoryUserInfo;
import com.sep.vox.application.projection.repository.AssessmentDirectoryQueryRepository;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.shared.PageResult;

/**
 * Học sinh có thể thêm làm thí sinh của một kỳ thi. Phạm vi bám đúng
 * {@link AssessmentDirectoryAccessService}: toàn trường, hoặc chỉ học sinh trong lớp của
 * người gọi khi đó là bài trên lớp.
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
        var scope = assessmentDirectoryAccessService.resolveByAssessmentId(input.assessmentId());
        var search = StringNormalization.trimAndCollapseSpaces(input.search());

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

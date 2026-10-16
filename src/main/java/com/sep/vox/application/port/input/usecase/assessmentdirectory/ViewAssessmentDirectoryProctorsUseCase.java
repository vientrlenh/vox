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
 * Giáo viên có thể phân công làm giám thị cho một ca thi.
 *
 * <p>Không phân nhánh theo {@code schoolWide}: giám thị luôn là giáo viên toàn trường.
 * Điều này không nới quyền — query {@code schoolUsersForRequester} vốn đã cho mọi giáo
 * viên liệt kê giáo viên cùng trường; ở đây chỉ để FE khỏi phải tự tra {@code roleId}.
 */
@Service
public class ViewAssessmentDirectoryProctorsUseCase
        implements IUseCase<ViewAssessmentDirectoryQuery, PageResult<AssessmentDirectoryUserInfo>> {

    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final AssessmentDirectoryQueryRepository assessmentDirectoryQueryRepository;

    public ViewAssessmentDirectoryProctorsUseCase(
            AssessmentDirectoryAccessService assessmentDirectoryAccessService,
            AssessmentDirectoryQueryRepository assessmentDirectoryQueryRepository) {
        this.assessmentDirectoryAccessService = assessmentDirectoryAccessService;
        this.assessmentDirectoryQueryRepository = assessmentDirectoryQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentDirectoryUserInfo> execute(ViewAssessmentDirectoryQuery input) {
        var scope = assessmentDirectoryAccessService.resolveByAssessmentId(input.assessmentId());

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

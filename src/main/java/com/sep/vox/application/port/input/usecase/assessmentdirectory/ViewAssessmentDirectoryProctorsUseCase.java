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
 * Giáo viên có thể phân công làm giám thị cho một ca thi.
 *
 * <p>Không phân nhánh theo {@code schoolWide}: giám thị luôn là giáo viên toàn trường.
 * Điều này không nới quyền — query {@code schoolUsersForRequester} vốn đã cho mọi giáo
 * viên liệt kê giáo viên cùng trường; ở đây chỉ để FE khỏi phải tự tra {@code roleId}.
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

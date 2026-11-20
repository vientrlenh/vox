package com.sep.vox.application.port.input.usecase.examdirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.ViewExamDirectoryQuery;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ExamDirectoryUserInfo;
import com.sep.vox.application.query.repository.ExamDirectoryQueryRepository;
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
public class ViewExamDirectoryProctorsUseCase
        implements IUseCase<ViewExamDirectoryQuery, PageResult<ExamDirectoryUserInfo>> {

    private final ExamDirectoryAccessService examDirectoryAccessService;
    private final ExamDirectoryQueryRepository examDirectoryQueryRepository;

    public ViewExamDirectoryProctorsUseCase(
            ExamDirectoryAccessService examDirectoryAccessService,
            ExamDirectoryQueryRepository examDirectoryQueryRepository) {
        this.examDirectoryAccessService = examDirectoryAccessService;
        this.examDirectoryQueryRepository = examDirectoryQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamDirectoryUserInfo> execute(ViewExamDirectoryQuery input) {
        var scope = examDirectoryAccessService.resolveByExamId(input.examId());

        return examDirectoryQueryRepository.findUsersBySchoolId(
            scope.schoolId(),
            SchoolRoleCodes.TEACHER,
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.excludeUserIds(),
            input.page(),
            input.size()
        );
    }
}

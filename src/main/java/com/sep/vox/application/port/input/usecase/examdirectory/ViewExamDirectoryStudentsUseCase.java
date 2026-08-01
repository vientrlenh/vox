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
 * Học sinh có thể thêm làm thí sinh của một kỳ thi. Phạm vi bám đúng
 * {@link ExamDirectoryAccessService}: toàn trường, hoặc chỉ học sinh trong lớp của
 * người gọi khi đó là bài trên lớp.
 */
@Service
public class ViewExamDirectoryStudentsUseCase
        implements IUseCase<ViewExamDirectoryQuery, PageResult<ExamDirectoryUserInfo>> {

    private final ExamDirectoryAccessService examDirectoryAccessService;
    private final ExamDirectoryQueryRepository examDirectoryQueryRepository;

    public ViewExamDirectoryStudentsUseCase(
            ExamDirectoryAccessService examDirectoryAccessService,
            ExamDirectoryQueryRepository examDirectoryQueryRepository) {
        this.examDirectoryAccessService = examDirectoryAccessService;
        this.examDirectoryQueryRepository = examDirectoryQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamDirectoryUserInfo> execute(ViewExamDirectoryQuery input) {
        var scope = examDirectoryAccessService.resolveByExamId(input.examId());
        var search = StringNormalization.trimAndCollapseSpaces(input.search());

        if (scope.schoolWide()) {
            return examDirectoryQueryRepository.findUsersBySchoolId(
                scope.schoolId(), SchoolRoleCodes.STUDENT, search, input.page(), input.size());
        }
        return examDirectoryQueryRepository.findUsersByClassIds(
            examDirectoryAccessService.callerClassIds(scope),
            SchoolRoleCodes.STUDENT,
            search,
            input.page(),
            input.size()
        );
    }
}

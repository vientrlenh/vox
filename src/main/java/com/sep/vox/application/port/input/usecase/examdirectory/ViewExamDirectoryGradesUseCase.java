package com.sep.vox.application.port.input.usecase.examdirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewExamDirectoryQuery;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.ExamDirectoryGradeInfo;
import com.sep.vox.application.query.repository.ExamDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;

/**
 * Niên khóa có thể lấy thí sinh cho một kỳ thi.
 *
 * <p>Chỉ có nghĩa ở phạm vi toàn trường: nhập theo niên khóa là gom mọi lớp của niên
 * khóa đó, nên chủ tịch bài trên lớp không có đường này. Cùng luật với
 * {@code ImportExamCandidatesFromGradeUseCase}.
 */
@Service
public class ViewExamDirectoryGradesUseCase
        implements IUseCase<ViewExamDirectoryQuery, PageResult<ExamDirectoryGradeInfo>> {

    static final String CLASS_TEST_REJECTION = "Bài kiểm tra trên lớp không hỗ trợ nhập thí sinh theo niên khóa";

    private final ExamDirectoryAccessService examDirectoryAccessService;
    private final ExamDirectoryQueryRepository examDirectoryQueryRepository;

    public ViewExamDirectoryGradesUseCase(
            ExamDirectoryAccessService examDirectoryAccessService,
            ExamDirectoryQueryRepository examDirectoryQueryRepository) {
        this.examDirectoryAccessService = examDirectoryAccessService;
        this.examDirectoryQueryRepository = examDirectoryQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ExamDirectoryGradeInfo> execute(ViewExamDirectoryQuery input) {
        var scope = examDirectoryAccessService.resolveByExamId(input.examId());
        if (!scope.schoolWide()) {
            throw new ForbiddenException(CLASS_TEST_REJECTION);
        }

        return examDirectoryQueryRepository.findGradesBySchoolId(
            scope.schoolId(),
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.page(),
            input.size()
        );
    }
}

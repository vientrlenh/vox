package com.sep.vox.application.port.input.usecase.assessmentdirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.projection.dto.AssessmentDirectoryGradeInfo;
import com.sep.vox.application.projection.repository.AssessmentDirectoryQueryRepository;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.shared.PageResult;

/**
 * Niên khóa có thể lấy thí sinh cho một kỳ thi.
 *
 * <p>Chỉ có nghĩa ở phạm vi toàn trường: nhập theo niên khóa là gom mọi lớp của niên
 * khóa đó, nên chủ tịch bài trên lớp không có đường này. Cùng luật với
 * {@code ImportAssessmentCandidatesFromGradeUseCase}.
 */
@Service
public class ViewAssessmentDirectoryGradesUseCase
        implements IUseCase<ViewAssessmentDirectoryQuery, PageResult<AssessmentDirectoryGradeInfo>> {

    static final String CLASS_TEST_REJECTION = "Bài kiểm tra trên lớp không hỗ trợ nhập thí sinh theo niên khóa";

    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final AssessmentDirectoryQueryRepository assessmentDirectoryQueryRepository;

    public ViewAssessmentDirectoryGradesUseCase(
            AssessmentDirectoryAccessService assessmentDirectoryAccessService,
            AssessmentDirectoryQueryRepository assessmentDirectoryQueryRepository) {
        this.assessmentDirectoryAccessService = assessmentDirectoryAccessService;
        this.assessmentDirectoryQueryRepository = assessmentDirectoryQueryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentDirectoryGradeInfo> execute(ViewAssessmentDirectoryQuery input) {
        var scope = assessmentDirectoryAccessService.resolveByAssessmentId(input.assessmentId());
        if (!scope.schoolWide()) {
            throw new ForbiddenException(CLASS_TEST_REJECTION);
        }

        return assessmentDirectoryQueryRepository.findGradesBySchoolId(
            scope.schoolId(),
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.page(),
            input.size()
        );
    }
}

package com.sep.vox.application.port.input.usecase.assessmentdirectory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.assessmentdirectory.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.query.dto.AssessmentDirectoryGradeInfo;
import com.sep.vox.application.query.repository.AssessmentDirectoryQueryRepository;
import com.sep.vox.domain.common.PageResult;

/**
 * The school years from which candidates can be taken for an assessment.
 *
 * <p>Only meaningful at whole-school scope: importing by school year gathers every class of that school
 * year, so the chair of a class test has no such path. The same rule as
 * {@code ImportAssessmentCandidatesFromGradeUseCase}.
 */
@Service
@RequiredArgsConstructor
public class ViewAssessmentDirectoryGradesUseCase
        implements IUseCase<ViewAssessmentDirectoryQuery, PageResult<AssessmentDirectoryGradeInfo>> {

    static final String CLASS_TEST_REJECTION = "Bài kiểm tra trên lớp không hỗ trợ nhập thí sinh theo niên khóa";

    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final AssessmentDirectoryQueryRepository assessmentDirectoryQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssessmentDirectoryGradeInfo> execute(ViewAssessmentDirectoryQuery input) {
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolveByAssessmentId(input.assessmentId());
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

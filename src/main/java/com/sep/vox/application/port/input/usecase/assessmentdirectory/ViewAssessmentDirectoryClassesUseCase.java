package com.sep.vox.application.port.input.usecase.assessmentdirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.shared.PageResult;

/**
 * Lớp có thể lấy thí sinh cho một kỳ thi.
 *
 * <p>Toàn trường với school admin và chủ tịch kỳ thi tập trung; chỉ lớp của chính
 * người gọi với chủ tịch bài trên lớp — xem {@link AssessmentDirectoryAccessService}.
 */
@Service
public class ViewAssessmentDirectoryClassesUseCase
        implements IUseCase<ViewAssessmentDirectoryQuery, PageResult<SchoolClassDto>> {

    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final SchoolClassRepository schoolClassRepository;

    public ViewAssessmentDirectoryClassesUseCase(
            AssessmentDirectoryAccessService assessmentDirectoryAccessService,
            SchoolClassRepository schoolClassRepository) {
        this.assessmentDirectoryAccessService = assessmentDirectoryAccessService;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassDto> execute(ViewAssessmentDirectoryQuery input) {
        var scope = assessmentDirectoryAccessService.resolveByAssessmentId(input.assessmentId());
        var search = StringNormalization.trimAndCollapseSpaces(input.search());

        var page = scope.schoolWide()
            ? schoolClassRepository.findBySchoolId(
                scope.schoolId(), search, SchoolClassStatus.ACTIVE, null, null, input.page(), input.size())
            : schoolClassRepository.findByUserId(
                scope.schoolId(), scope.callerId(), search, SchoolClassStatus.ACTIVE, input.page(), input.size());

        return SchoolClassDto.toDtoPage(page);
    }
}

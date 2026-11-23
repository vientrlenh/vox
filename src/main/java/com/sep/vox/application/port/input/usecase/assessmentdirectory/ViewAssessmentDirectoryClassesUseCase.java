package com.sep.vox.application.port.input.usecase.assessmentdirectory;

import com.sep.vox.domain.model.school.SchoolClass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.assessmentdirectory.ViewAssessmentDirectoryQuery;
import com.sep.vox.application.port.input.service.AssessmentDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;

/**
 * The classes from which candidates can be taken for an assessment.
 *
 * <p>The whole school for the school admin and the chair of a centralized assessment; only the caller's own
 * classes for the chair of a class test -- see {@link AssessmentDirectoryAccessService}.
 */
@Service
@RequiredArgsConstructor
public class ViewAssessmentDirectoryClassesUseCase
        implements IUseCase<ViewAssessmentDirectoryQuery, PageResult<SchoolClassDto>> {

    private final AssessmentDirectoryAccessService assessmentDirectoryAccessService;
    private final SchoolClassRepository schoolClassRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassDto> execute(ViewAssessmentDirectoryQuery input) {
        AssessmentDirectoryAccessService.AssessmentDirectoryScope scope = assessmentDirectoryAccessService.resolveByAssessmentId(input.assessmentId());
        String search = StringNormalization.trimAndCollapseSpaces(input.search());

        PageResult<SchoolClass> page = scope.schoolWide()
            ? schoolClassRepository.findBySchoolId(
                scope.schoolId(), search, SchoolClassStatus.ACTIVE, null, null, input.page(), input.size())
            : schoolClassRepository.findByUserId(
                scope.schoolId(), scope.callerId(), search, SchoolClassStatus.ACTIVE, input.page(), input.size());

        return SchoolClassDtoMapper.toDtoPage(page);
    }
}

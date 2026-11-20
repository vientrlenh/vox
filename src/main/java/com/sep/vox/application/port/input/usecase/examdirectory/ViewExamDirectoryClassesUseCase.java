package com.sep.vox.application.port.input.usecase.examdirectory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.ViewExamDirectoryQuery;
import com.sep.vox.application.port.input.service.ExamDirectoryAccessService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;

/**
 * Lớp có thể lấy thí sinh cho một kỳ thi.
 *
 * <p>Toàn trường với school admin và chủ tịch kỳ thi tập trung; chỉ lớp của chính
 * người gọi với chủ tịch bài trên lớp — xem {@link ExamDirectoryAccessService}.
 */
@Service
public class ViewExamDirectoryClassesUseCase
        implements IUseCase<ViewExamDirectoryQuery, PageResult<SchoolClassDto>> {

    private final ExamDirectoryAccessService examDirectoryAccessService;
    private final SchoolClassRepository schoolClassRepository;

    public ViewExamDirectoryClassesUseCase(
            ExamDirectoryAccessService examDirectoryAccessService,
            SchoolClassRepository schoolClassRepository) {
        this.examDirectoryAccessService = examDirectoryAccessService;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassDto> execute(ViewExamDirectoryQuery input) {
        var scope = examDirectoryAccessService.resolveByExamId(input.examId());
        var search = StringNormalization.trimAndCollapseSpaces(input.search());

        var page = scope.schoolWide()
            ? schoolClassRepository.findBySchoolId(
                scope.schoolId(), search, SchoolClassStatus.ACTIVE, null, null, input.page(), input.size())
            : schoolClassRepository.findByUserId(
                scope.schoolId(), scope.callerId(), search, SchoolClassStatus.ACTIVE, input.page(), input.size());

        return SchoolClassDtoMapper.toDtoPage(page);
    }
}

package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.port.input.query.ViewSchoolGradesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolGradeDto;

import com.sep.vox.domain.mapper.SchoolGradeDtoMapper;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewSchoolGradesUseCase implements IUseCase<ViewSchoolGradesQuery, PageResult<SchoolGradeDto>> {

    private final SchoolGradeRepository schoolGradeRepository;

    public ViewSchoolGradesUseCase(SchoolGradeRepository schoolGradeRepository) {
        this.schoolGradeRepository = schoolGradeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolGradeDto> execute(ViewSchoolGradesQuery query) {

        // 1. Gọi DB lấy danh sách phân trang (PageResult<SchoolGrade>)
        PageResult<SchoolGrade> pageResult = schoolGradeRepository.findAllBySchoolId(
                query.schoolId(),
                query.schoolGradeLevelId(),
                query.page(),
                query.size()
        );

        // 2. Chuyển đổi sang DTO và trả về
        return new PageResult<>(
                pageResult.content().stream()
                        .map(SchoolGradeDtoMapper::toSchoolGradeDto)
                        .toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}
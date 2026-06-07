package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.port.input.query.ViewSchoolClassDetailsQuery;
import com.sep.vox.application.port.input.query.ViewSchoolGradeDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolGradeFromDto;
import com.sep.vox.domain.mapper.SchoolGradeDtoMapper;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewSchoolGradeDetailsUseCase implements IUseCase<ViewSchoolGradeDetailsQuery, SchoolGradeFromDto> {

    private final SchoolGradeRepository schoolGradeRepository;

    public ViewSchoolGradeDetailsUseCase(SchoolGradeRepository schoolGradeRepository) {
        this.schoolGradeRepository = schoolGradeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolGradeFromDto execute(ViewSchoolGradeDetailsQuery input) {
       return schoolGradeRepository.findById(input.id())
            .map(SchoolGradeDtoMapper::toDto)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khối học"));
    }
}

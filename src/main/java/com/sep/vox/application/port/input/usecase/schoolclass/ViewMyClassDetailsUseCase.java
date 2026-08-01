package com.sep.vox.application.port.input.usecase.schoolclass;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewMyClassDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.repository.SchoolClassRepository;

@Service
public class ViewMyClassDetailsUseCase implements IUseCase<ViewMyClassDetailsQuery, SchoolClassDto> {

    private final MyClassAccessGuard myClassAccessGuard;
    private final SchoolClassRepository schoolClassRepository;

    public ViewMyClassDetailsUseCase(
            MyClassAccessGuard myClassAccessGuard,
            SchoolClassRepository schoolClassRepository) {
        this.myClassAccessGuard = myClassAccessGuard;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassDto execute(ViewMyClassDetailsQuery input) {
        myClassAccessGuard.requireClassMembership(input.schoolClassId());

        var schoolClass = schoolClassRepository.findById(input.schoolClassId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));

        return SchoolClassDtoMapper.toDto(schoolClass);
    }
}

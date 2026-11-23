package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.SchoolClass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewMyClassDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.repository.SchoolClassRepository;

@Service
@RequiredArgsConstructor
public class ViewMyClassDetailsUseCase implements IUseCase<ViewMyClassDetailsQuery, SchoolClassDto> {

    private final MyClassAccessGuard myClassAccessGuard;
    private final SchoolClassRepository schoolClassRepository;

    @Override
    @Transactional(readOnly = true)
    public SchoolClassDto execute(ViewMyClassDetailsQuery input) {
        myClassAccessGuard.requireClassMembership(input.schoolClassId());

        SchoolClass schoolClass = schoolClassRepository.findById(input.schoolClassId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));

        return SchoolClassDtoMapper.toDto(schoolClass);
    }
}

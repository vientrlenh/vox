package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.SchoolClass;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.schoolclass.ViewMyClassesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.mapper.SchoolClassDtoMapper;
import com.sep.vox.domain.repository.SchoolClassRepository;

@Service
@RequiredArgsConstructor
public class ViewMyClassesUseCase implements IUseCase<ViewMyClassesQuery, PageResult<SchoolClassDto>> {

    private final MyClassAccessGuard myClassAccessGuard;
    private final SchoolClassRepository schoolClassRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassDto> execute(ViewMyClassesQuery input) {
        UUID callerId = myClassAccessGuard.requireSchoolMembership(input.schoolId());

        PageResult<SchoolClass> schoolClassesPage = schoolClassRepository.findByUserId(
            input.schoolId(),
            callerId,
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.status(),
            input.page(),
            input.size()
        );

        return SchoolClassDtoMapper.toDtoPage(schoolClassesPage);
    }
}

package com.sep.vox.application.port.input.usecase.schoolclass;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyClassesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.shared.StringNormalization;
import com.sep.vox.domain.dto.SchoolClassDto;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewMyClassesUseCase implements IUseCase<ViewMyClassesQuery, PageResult<SchoolClassDto>> {

    private final MyClassAccessGuard myClassAccessGuard;
    private final SchoolClassRepository schoolClassRepository;

    public ViewMyClassesUseCase(
            MyClassAccessGuard myClassAccessGuard,
            SchoolClassRepository schoolClassRepository) {
        this.myClassAccessGuard = myClassAccessGuard;
        this.schoolClassRepository = schoolClassRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassDto> execute(ViewMyClassesQuery input) {
        var callerId = myClassAccessGuard.requireSchoolMembership(input.schoolId());

        var schoolClassesPage = schoolClassRepository.findByUserId(
            input.schoolId(),
            callerId,
            StringNormalization.trimAndCollapseSpaces(input.search()),
            parseStatus(input.status()),
            input.pageRequest().repositoryPage(),
            input.pageRequest().size()
        );

        return SchoolClassDto.toDtoPage(schoolClassesPage);
    }

    private SchoolClassStatus parseStatus(String status) {
        var normalized = StringNormalization.trimAndCollapseSpaces(status);
        if (normalized == null) {
            return null;
        }
        try {
            return SchoolClassStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái lớp học không hợp lệ");
        }
    }
}

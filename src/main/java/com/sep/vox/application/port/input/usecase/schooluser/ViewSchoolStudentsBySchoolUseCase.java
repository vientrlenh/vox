package com.sep.vox.application.port.input.usecase.schooluser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolStudentsBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.school.SchoolUserRole;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewSchoolStudentsBySchoolUseCase implements IUseCase<ViewSchoolStudentsBySchoolQuery, PageResult<SchoolUserDto>> {

    private final ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;

    public ViewSchoolStudentsBySchoolUseCase(ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase) {
        this.viewSchoolUsersBySchoolUseCase = viewSchoolUsersBySchoolUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserDto> execute(ViewSchoolStudentsBySchoolQuery input) {
        return viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(
            input.schoolId(),
            input.pageRequest(),
            input.search(),
            SchoolUserRole.STUDENT,
            input.status()
        ));
    }
}

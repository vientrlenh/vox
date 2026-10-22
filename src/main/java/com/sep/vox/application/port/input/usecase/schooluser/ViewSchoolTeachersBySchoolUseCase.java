package com.sep.vox.application.port.input.usecase.schooluser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewSchoolTeachersBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.school.SchoolUserRole;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewSchoolTeachersBySchoolUseCase implements IUseCase<ViewSchoolTeachersBySchoolQuery, PageResult<SchoolUserDto>> {

    private final ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;

    public ViewSchoolTeachersBySchoolUseCase(ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase) {
        this.viewSchoolUsersBySchoolUseCase = viewSchoolUsersBySchoolUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserDto> execute(ViewSchoolTeachersBySchoolQuery input) {
        return viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(
            input.schoolId(),
            input.pageRequest(),
            input.search(),
            SchoolUserRole.TEACHER,
            input.status()
        ));
    }
}

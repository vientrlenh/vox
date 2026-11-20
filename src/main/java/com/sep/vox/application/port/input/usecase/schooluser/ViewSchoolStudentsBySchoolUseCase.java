package com.sep.vox.application.port.input.usecase.schooluser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewSchoolStudentsBySchoolQuery;
import com.sep.vox.application.port.input.query.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.RoleRepository;

@Service
public class ViewSchoolStudentsBySchoolUseCase implements IUseCase<ViewSchoolStudentsBySchoolQuery, PageResult<SchoolUserDto>> {

    private final RoleRepository roleRepository;
    private final ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;

    public ViewSchoolStudentsBySchoolUseCase(
            RoleRepository roleRepository,
            ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase) {
        this.roleRepository = roleRepository;
        this.viewSchoolUsersBySchoolUseCase = viewSchoolUsersBySchoolUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserDto> execute(ViewSchoolStudentsBySchoolQuery input) {
        var role = roleRepository.findByCode(SchoolRoleCodes.STUDENT)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò học sinh"));

        return viewSchoolUsersBySchoolUseCase.execute(new ViewSchoolUsersBySchoolQuery(
            input.schoolId(),
            input.page(),
            input.size(),
            input.search(),
            role.getId(),
            input.status()
        ));
    }
}

package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.domain.model.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolTeachersBySchoolQuery;
import com.sep.vox.application.port.input.query.schooluser.ViewSchoolUsersBySchoolQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.model.user.SchoolRoleCodes;
import com.sep.vox.domain.repository.RoleRepository;

@Service
@RequiredArgsConstructor
public class ViewSchoolTeachersBySchoolUseCase implements IUseCase<ViewSchoolTeachersBySchoolQuery, PageResult<SchoolUserDto>> {

    private final RoleRepository roleRepository;
    private final ViewSchoolUsersBySchoolUseCase viewSchoolUsersBySchoolUseCase;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolUserDto> execute(ViewSchoolTeachersBySchoolQuery input) {
        Role role = roleRepository.findByCode(SchoolRoleCodes.TEACHER)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò giáo viên"));

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

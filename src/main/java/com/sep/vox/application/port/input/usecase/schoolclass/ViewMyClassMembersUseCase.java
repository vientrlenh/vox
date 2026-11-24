package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.SchoolClassUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.query.schoolclass.ViewMyClassMembersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.SchoolClassUserDto;
import com.sep.vox.domain.mapper.SchoolClassUserDtoMapper;
import com.sep.vox.domain.repository.SchoolClassUserRepository;

@Service
@RequiredArgsConstructor
public class ViewMyClassMembersUseCase implements IUseCase<ViewMyClassMembersQuery, PageResult<SchoolClassUserDto>> {

    private final MyClassAccessGuard myClassAccessGuard;
    private final SchoolClassUserRepository schoolClassUserRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<SchoolClassUserDto> execute(ViewMyClassMembersQuery input) {
        myClassAccessGuard.requireClassMembership(input.schoolClassId());

        PageResult<SchoolClassUser> membersPage = schoolClassUserRepository.findBySchoolClassId(
            input.schoolClassId(),
            normalizeRoleCode(input.roleCode()),
            StringNormalization.trimAndCollapseSpaces(input.search()),
            input.page(),
            input.size()
        );

        return SchoolClassUserDtoMapper.toSchoolClassUserDtoPage(membersPage);
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = StringNormalization.trimAndCollapseSpaces(roleCode);
        return normalized == null ? null : normalized.toUpperCase();
    }
}

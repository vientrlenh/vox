package com.sep.vox.application.port.input.usecase.role;

import com.sep.vox.domain.model.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewRolesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.mapper.RoleDtoMapper;
import com.sep.vox.domain.repository.RoleRepository;

@Service
@RequiredArgsConstructor
public class ViewRolesUseCase implements IUseCase<ViewRolesQuery, PageResult<RoleDto>>{

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoleDto> execute(ViewRolesQuery input) {
        PageResult<Role> result = roleRepository.findAll(input.page(), input.size());
        return RoleDtoMapper.toRoleDtoPage(result);
    }
    
}

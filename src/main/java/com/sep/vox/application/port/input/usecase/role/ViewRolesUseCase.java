package com.sep.vox.application.port.input.usecase.role;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewRolesQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.mapper.RoleDtoMapper;
import com.sep.vox.domain.repository.RoleRepository;

@Service
public class ViewRolesUseCase implements IUseCase<ViewRolesQuery, PageResult<RoleDto>>{

    private final RoleRepository roleRepository;

    public ViewRolesUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RoleDto> execute(ViewRolesQuery input) {
        var result = roleRepository.findAll(input.page(), input.size());
        return RoleDtoMapper.toRoleDtoPage(result);
    }
    
}

package com.sep.vox.application.port.input.usecase.role;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewRoleDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.mapper.RoleDtoMapper;
import com.sep.vox.domain.repository.RoleRepository;

@Service
public class ViewRoleDetailsUseCase implements IUseCase<ViewRoleDetailsQuery, RoleDto>{

    private final RoleRepository roleRepository;

    public ViewRoleDetailsUseCase(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto execute(ViewRoleDetailsQuery input) {
        var role = roleRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy vai trò yêu cầu"));
        return RoleDtoMapper.toRoleDto(role);
    }
    
}

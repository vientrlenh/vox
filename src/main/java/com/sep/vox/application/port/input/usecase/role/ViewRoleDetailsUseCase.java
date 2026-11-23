package com.sep.vox.application.port.input.usecase.role;

import com.sep.vox.domain.model.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.ViewRoleDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.RoleDto;
import com.sep.vox.domain.mapper.RoleDtoMapper;
import com.sep.vox.domain.repository.RoleRepository;

@Service
@RequiredArgsConstructor
public class ViewRoleDetailsUseCase implements IUseCase<ViewRoleDetailsQuery, RoleDto>{

    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public RoleDto execute(ViewRoleDetailsQuery input) {
        Role role = roleRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò yêu cầu"));
        return RoleDtoMapper.toRoleDto(role);
    }
    
}

package com.sep.vox.application.port.input.usecase.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewUsersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewUsersUseCase implements IUseCase<ViewUsersQuery, PageResult<UserDto>> {

    private final UserRepository userRepository;

    public ViewUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserDto> execute(ViewUsersQuery input) {
        var result = userRepository.findAll(input.page(), input.size());
        return UserDtoMapper.toUserDtoPage(result);
    }
    
}

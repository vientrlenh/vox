package com.sep.vox.application.port.input.usecase.user;

import com.sep.vox.domain.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.user.ViewUsersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewUsersUseCase implements IUseCase<ViewUsersQuery, PageResult<UserDto>> {

    private final UserRepository userRepository;

    @Override
    public PageResult<UserDto> execute(ViewUsersQuery input) {
        PageResult<User> result = userRepository.findAll(input.page(), input.size());
        return UserDtoMapper.toUserDtoPage(result);
    }
    
}

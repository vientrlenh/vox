package com.sep.vox.application.port.input.usecase.user;

import org.springframework.stereotype.Service;

import com.sep.vox.application.port.input.query.ViewUsersQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.shared.PageResult;

@Service
public class ViewUsersUseCase implements IUseCase<ViewUsersQuery, PageResult<UserDto>> {

    private final UserRepository userRepository;

    public ViewUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PageResult<UserDto> execute(ViewUsersQuery input) {
        int page = input.pageRequest().page();
        int size = input.pageRequest().size();
        PageResult<User> result = userRepository.findAll(page, size);
        return UserDto.toDtoPage(result);
    }
    
}

package com.sep.vox.application.port.input.usecase.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.query.ViewUserDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewUserDetailsUseCase implements IUseCase<ViewUserDetailsQuery, UserDto> {

    private final UserRepository userRepository; 

    public ViewUserDetailsUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto execute(ViewUserDetailsQuery input) {
        var user = userRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        return UserDtoMapper.toUserDto(user);
    }
    
}

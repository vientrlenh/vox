package com.sep.vox.application.port.input.usecase.user;

import com.sep.vox.domain.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.query.user.ViewUserDetailsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewUserDetailsUseCase implements IUseCase<ViewUserDetailsQuery, UserDto> {

    private final UserRepository userRepository; 

    @Override
    public UserDto execute(ViewUserDetailsQuery input) {
        User user = userRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDtoMapper.toUserDto(user);
    }
    
}

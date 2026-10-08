package com.sep.vox.application.port.input.usecase.user;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewProfileUseCase implements IUseCase<Void, UserDto>{

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public ViewProfileUseCase(UserContextPort userContextPort, UserRepository userRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    public UserDto execute(Void input) {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        User user = userRepository.findByIdAndStatusActive(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.toDto(user);
    }
    
}

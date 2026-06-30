package com.sep.vox.application.port.input.usecase.user;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
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
        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng yêu cầu"));
        
        return UserDtoMapper.toUserDto(user);
    }
    
}

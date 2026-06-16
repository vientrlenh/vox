package com.sep.vox.application.port.input.usecase.schooluser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.SchoolUserDto;
import com.sep.vox.domain.mapper.SchoolUserDtoMapper;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolUserUseCase implements IUseCase<ViewSchoolUserCommand, SchoolUserDto> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final SchoolUserRepository schoolUserRepository;

    public ViewSchoolUserUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    @Transactional
    public SchoolUserDto execute(ViewSchoolUserCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        var callerSchoolUser = schoolUserRepository.findByUserId(caller.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không có quyền thực hiện thao tác này"));
        if (!input.schoolId().equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var schoolUser = schoolUserRepository.findBySchoolIdAndUserId(input.schoolId(), input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng nhà trường theo yêu cầu"));

        return SchoolUserDtoMapper.toSchoolUserDto(schoolUser);
    }
}

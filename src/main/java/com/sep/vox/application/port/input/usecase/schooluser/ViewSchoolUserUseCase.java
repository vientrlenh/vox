package com.sep.vox.application.port.input.usecase.schooluser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.mapper.schooluser.SchoolUserResponseMapper;
import com.sep.vox.application.port.input.command.ViewSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.schooluser.SchoolUserResponse;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class ViewSchoolUserUseCase implements IUseCase<ViewSchoolUserCommand, SchoolUserResponse> {

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
    public SchoolUserResponse execute(ViewSchoolUserCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        SchoolUserStatusValidator.requireActive(caller);
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var targetUser = userRepository.findById(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        SchoolUserStatusValidator.requireActive(targetUser);
        if (!input.schoolId().equals(targetUser.getSchoolId())) {
            throw new NotFoundException("Không tìm thấy người dùng");
        }

        var roleInfo = userRoleQueryRepository.findByUserIdWithRoleInfo(input.userId());
        var roleCode = roleInfo.isEmpty() ? null : roleInfo.get(0).roleCode();

        var schoolUser = schoolUserRepository.findByUserId(input.userId()).orElse(null);

        return SchoolUserResponseMapper.toResponse(targetUser, roleCode, schoolUser);
    }
}

package com.sep.vox.application.port.input.usecase.schooluser;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.UserStatusValidator;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.DeleteSchoolUserResponse;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class DeleteSchoolUserUseCase implements IUseCase<DeleteSchoolUserCommand, DeleteSchoolUserResponse> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    public DeleteSchoolUserUseCase(UserContextPort userContextPort, UserRepository userRepository) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public DeleteSchoolUserResponse execute(DeleteSchoolUserCommand input) {
        var now = OffsetDateTime.now();
        var callerId = userContextPort.getCurrentAuthenticatedUserId();

        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        UserStatusValidator.requireActive(caller);
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }

        var targetUser = userRepository.findById(input.userId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        UserStatusValidator.requireActive(targetUser);
        if (!input.schoolId().equals(targetUser.getSchoolId())) {
            throw new NotFoundException("Không tìm thấy người dùng");
        }

        targetUser.softDelete(callerId, now);
        userRepository.save(targetUser);

        return new DeleteSchoolUserResponse(input.userId(), "SOFT", "DISABLED", now.toString());
    }
}

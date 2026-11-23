package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.DeleteSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DeleteSchoolUserUseCase implements IUseCase<DeleteSchoolUserCommand, Void> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public Void execute(DeleteSchoolUserCommand input) {
        Instant now = Instant.now();
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();

        userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        SchoolUser callerSchoolUser = schoolUserRepository.findByUserId(callerId)
            .orElseThrow(() -> new IllegalArgumentException("Không có quyền thực hiện thao tác này"));
        if (!input.schoolId().equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }
        if (callerId.equals(input.userId())) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản của chính mình");
        }

        User targetUser = userRepository.findById(input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        SchoolUser targetSchoolUser = schoolUserRepository.findByUserId(input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(targetSchoolUser.getSchoolId())) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng");
        }

        targetUser.softDelete();
        userRepository.save(targetUser);

        // SchoolUser has no isActive flag: the membership is deactivated by setting its endDate, which keeps the
        // database constraint start_date < end_date satisfied.
        Instant startDate = targetSchoolUser.getStartDate();
        Instant membershipEnd = (startDate != null && !startDate.isBefore(now))
            ? startDate.plusSeconds(1)
            : now;
        targetSchoolUser.setEndDate(membershipEnd);
        schoolUserRepository.save(targetSchoolUser);

        return null;
    }
}

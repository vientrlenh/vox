package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schooluser.DeleteSchoolUserCommand;
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
        requireCallerMayDeleteUsersOf(input);

        User targetUser = userRepository.findById(input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SchoolUser targetSchoolUser = schoolUserRepository.findByUserId(input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!input.schoolId().equals(targetSchoolUser.getSchoolId())) {
            throw new ResourceNotFoundException("User not found");
        }

        targetUser.softDelete();
        userRepository.save(targetUser);

        endMembership(targetSchoolUser, now);

        return null;
    }

    private void requireCallerMayDeleteUsersOf(DeleteSchoolUserCommand input) {
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();

        userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        SchoolUser callerSchoolUser = schoolUserRepository.findByUserId(callerId)
            .orElseThrow(() -> new IllegalArgumentException("You are not allowed to perform this action"));
        if (!input.schoolId().equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("You are not allowed to perform this action");
        }
        if (callerId.equals(input.userId())) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
    }

    // SchoolUser has no isActive flag: the membership is deactivated by setting its endDate, which keeps the
    // database constraint start_date < end_date satisfied.
    private void endMembership(SchoolUser targetSchoolUser, Instant now) {
        Instant startDate = targetSchoolUser.getStartDate();
        Instant membershipEnd = (startDate != null && !startDate.isBefore(now))
            ? startDate.plusSeconds(1)
            : now;
        targetSchoolUser.setEndDate(membershipEnd);
        schoolUserRepository.save(targetSchoolUser);
    }
}

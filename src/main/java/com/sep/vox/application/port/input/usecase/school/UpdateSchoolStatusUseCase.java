package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.school.UpdateSchoolStatusCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSchoolStatusUseCase implements IUseCase<UpdateSchoolStatusCommand, UUID> {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository; // Added this repository
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolStatusCommand command) {
        School school = schoolRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("No school found with the given ID."));

        requireCallerMayChangeStatusOf(school);

        // Avoid a redundant update
        if (school.isActive() == command.isActive()) {
            throw new IllegalArgumentException("This school is already " + (command.isActive() ? "active" : "deactivated") + ". No update needed.");
        }

        school.setActive(command.isActive());

        School updatedSchool = schoolRepository.save(school);

        return updatedSchool.getId();
    }

    private void requireCallerMayChangeStatusOf(School school) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("Your account does not exist or has been locked.");
        }

        // SECURITY GUARD: checked through the SchoolUser link
        Optional<SchoolUser> schoolUserOpt = schoolUserRepository.findByUserId(currentUserId);

        // A user linked to a school (a School Admin) may only change the status of their own school
        if (schoolUserOpt.isPresent()) {
            SchoolUser schoolUser = schoolUserOpt.get();
            if (!schoolUser.getSchoolId().equals(school.getId())) {
                throw new ForbiddenException("SECURITY: You are not allowed to update the status of another organization's school.");
            }
        }
    }
}
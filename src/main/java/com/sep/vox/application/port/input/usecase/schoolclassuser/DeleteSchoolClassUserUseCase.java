package com.sep.vox.application.port.input.usecase.schoolclassuser;

import com.sep.vox.domain.model.school.School;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schoolclassuser.DeleteSchoolClassUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schoolclassuser.DeleteSchoolClassUserResponse;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DeleteSchoolClassUserUseCase implements IUseCase<DeleteSchoolClassUserCommand, DeleteSchoolClassUserResponse> {

    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public DeleteSchoolClassUserResponse execute(DeleteSchoolClassUserCommand input) {
        validateCommand(input);
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);

        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);
        validateSchoolClass(input.classId(), schoolId);
        validateTargetUser(input.userId(), schoolId);
        SchoolClassUser membership = findMembership(input.userId(), input.classId());

        if (membership.isActive()) {
            membership.deactivate(Instant.now());
            schoolClassUserRepository.save(membership);
        }

        return new DeleteSchoolClassUserResponse(input.classId());
    }

    private void validateCommand(DeleteSchoolClassUserCommand input) {
        if (input.schoolId() == null) {
            throw new IllegalArgumentException("The school must not be empty");
        }
        if (input.classId() == null) {
            throw new IllegalArgumentException("The class must not be empty");
        }
        if (input.userId() == null) {
            throw new IllegalArgumentException("The user must not be empty");
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        validateCurrentUserIsActive(user);
        return user;
    }

    private void validateCurrentUserIsActive(User currentUser) {
        if (currentUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("The current user is not active");
        }
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("The current user does not belong to any school"));
        return schoolUser.getSchoolId();
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("The school does not match the current user");
        }
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("The school is not active");
        }
    }

    private SchoolClass validateSchoolClass(UUID classId, UUID schoolId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!Objects.equals(schoolClass.getSchoolId(), schoolId)) {
            throw new ResourceNotFoundException("Class not found");
        }
        if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
            throw new IllegalArgumentException("The class is not active");
        }
        return schoolClass;
    }

    private void validateTargetUser(UUID userId, UUID schoolId) {
        User targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("The user is not active");
        }
        SchoolUser targetSchoolUser = schoolUserRepository.findByUserId(targetUser.getId()).orElse(null);
        if (!Objects.equals(targetSchoolUser != null ? targetSchoolUser.getSchoolId() : null, schoolId)) {
            throw new IllegalArgumentException("The user does not belong to the current school");
        }
    }

    private SchoolClassUser findMembership(UUID userId, UUID classId) {
        return schoolClassUserRepository.findByUserIdAndSchoolClassId(userId, classId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found in the class"));
    }
}

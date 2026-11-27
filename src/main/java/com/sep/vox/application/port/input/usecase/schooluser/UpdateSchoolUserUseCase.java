package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;

import java.util.Optional;
import java.util.UUID;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.AvatarUrlPolicy;
import com.sep.vox.application.common.StringNormalization;

import com.sep.vox.application.exception.ResourceDuplicatedException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schooluser.UpdateSchoolUserCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AvatarConfigPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.UpdateSchoolUserResponse;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UpdateSchoolUserUseCase implements IUseCase<UpdateSchoolUserCommand, UpdateSchoolUserResponse> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AvatarConfigPort avatarConfigPort;

    @Override
    @Transactional
    public UpdateSchoolUserResponse execute(UpdateSchoolUserCommand input) {
        if (!input.fullNameProvided() && !input.phoneProvided() && !input.addressProvided()
                && !input.dateOfBirthProvided() && !input.avatarUrlProvided()) {
            throw new IllegalArgumentException("At least one field must be provided to update");
        }

        requireCallerBelongsToSchool(input);

        User target = findLockedTargetInSchool(input);

        applyProvidedFields(target, input);

        try {
            userRepository.saveAndFlush(target);
        } catch (DataConflictException e) {
            throw new ResourceDuplicatedException("Phone number already exists");
        }

        return new UpdateSchoolUserResponse(target.getId());
    }

    private User findLockedTargetInSchool(UpdateSchoolUserCommand input) {
        User target = userRepository.findByIdForUpdate(input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SchoolUser targetSchoolUser = schoolUserRepository.findByUserId(input.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!input.schoolId().equals(targetSchoolUser.getSchoolId())) {
            throw new ResourceNotFoundException("User not found");
        }
        return target;
    }

    private void requireCallerBelongsToSchool(UpdateSchoolUserCommand input) {
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();

        User caller = userRepository.findByIdAndStatus(callerId, UserStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SchoolUser callerSchoolUser = schoolUserRepository.findByUserId(caller.getId())
            .orElseThrow(() -> new IllegalArgumentException("You are not allowed to perform this action"));
        if (!input.schoolId().equals(callerSchoolUser.getSchoolId())) {
            throw new IllegalArgumentException("You are not allowed to perform this action");
        }
    }

    private void applyProvidedFields(User target, UpdateSchoolUserCommand input) {
        if (input.fullNameProvided()) {
            applyFullName(target, input);
        }

        if (input.phoneProvided()) {
            applyPhone(target, input);
        }

        if (input.addressProvided()) {
            target.setAddress(input.address() != null ? StringNormalization.trimAndCollapseSpaces(input.address()) : null);
        }

        if (input.dateOfBirthProvided()) {
            target.setDateOfBirth(input.dateOfBirth() != null ? new DateOfBirth(input.dateOfBirth()) : null);
        }

        if (input.avatarUrlProvided()) {
            applyAvatarUrl(target, input);
        }
    }

    // This is the ONLY way to set a student's avatar: UpdateProfileUseCase rejects students changing their own
    // photo, because in this product the portrait is identity data used by proctors for roll call. When a school
    // admin sets it on their behalf, someone is accountable for the photo.
    // The URL is still checked even though the caller is a school admin: accepting a URL on a foreign host
    // would make every teacher who opens the grading list call out to that host. See AvatarUrlPolicy.
    private void applyAvatarUrl(User target, UpdateSchoolUserCommand input) {
        target.setAvatarUrl(AvatarUrlPolicy.normalizeOrThrow(avatarConfigPort.allowedHosts(), input.avatarUrl()));
    }

    private void applyFullName(User target, UpdateSchoolUserCommand input) {
        String normalized = StringNormalization.trimAndCollapseSpaces(input.fullName());
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Full name must not be empty");
        }
        target.setFullName(new FullName(normalized));
    }

    private void applyPhone(User target, UpdateSchoolUserCommand input) {
        String normalized = StringNormalization.normalizePhone(input.phone());
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be empty");
        }
        Optional<User> existing = userRepository.findByPhone(normalized);
        if (existing.isPresent() && !existing.get().getId().equals(target.getId())) {
            throw new ResourceDuplicatedException("Phone number already exists");
        }
        target.setPhone(new Phone(normalized));
    }
}

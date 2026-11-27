package com.sep.vox.application.port.input.usecase.user;

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
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.user.UpdateProfileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AvatarConfigPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.UserDto;
import com.sep.vox.domain.mapper.UserDtoMapper;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.domain.valueobject.DateOfBirth;
import com.sep.vox.domain.valueobject.FullName;
import com.sep.vox.domain.valueobject.Phone;

/**
 * Lets users edit their own profile. Previously the {@code usecase/user} package had only three READ use cases,
 * so {@code users.avatar_url} was in practice always null: {@code OAuth2LoginUseCase} receives {@code avatarUrl}
 * in its command but does NOT use it, and the only writer was {@code CreateSchoolUseCase}, once at school
 * creation. This is the first write path after the account already exists.
 *
 * <p>Normalization and duplicate checks follow {@code UpdateSchoolUserUseCase} so that the two paths that edit
 * the same table do not drift apart (full name must not be empty, phone number must not be empty or duplicated).
 */
@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase implements IUseCase<UpdateProfileCommand, UserDto> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final AvatarConfigPort avatarConfigPort;

    @Override
    @Transactional
    public UserDto execute(UpdateProfileCommand input) {
        requireAtLeastOneField(input);

        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();

        User target = userRepository.findByIdForUpdate(callerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (target.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("The account is not active");
        }

        applyProvidedFields(target, input);

        try {
            userRepository.saveAndFlush(target);
        } catch (DataConflictException e) {
            throw new ResourceDuplicatedException("Phone number already exists");
        }

        return UserDtoMapper.toUserDto(target);
    }

    private void requireAtLeastOneField(UpdateProfileCommand input) {
        if (!input.fullNameProvided()
                && !input.phoneProvided()
                && !input.addressProvided()
                && !input.dateOfBirthProvided()
                && !input.avatarUrlProvided()) {
            throw new IllegalArgumentException("At least one field must be provided to update");
        }
    }

    private void applyProvidedFields(User target, UpdateProfileCommand input) {
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
            target.setAvatarUrl(resolveAvatarUrl(input.avatarUrl()));
        }
    }

    private void applyFullName(User target, UpdateProfileCommand input) {
        String normalized = StringNormalization.trimAndCollapseSpaces(input.fullName());
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Full name must not be empty");
        }
        target.setFullName(new FullName(normalized));
    }

    private void applyPhone(User target, UpdateProfileCommand input) {
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

    /**
     * Students do NOT change their own avatar; a student's photo is set by the school through
     * {@code updateSchoolUser}.
     *
     * <p>The reason is a business one, not a technical one: in this product the portrait photo is IDENTITY
     * data. The {@code TeacherProctorAttendancePage} screen lets the proctor mark "Present", flag the
     * session, block the candidate and end the exam -- a photo the candidate chose themselves would look like
     * identification evidence while actually being a self-declaration, which is worse than no photo. Besides,
     * the users here are high school students, and the repo has no image moderation tool yet.
     *
     * <p>Written as an allowlist (must be one of three roles) instead of {@code isStudent()} on purpose: an
     * account with no role at all is rejected too, instead of slipping through as "not a student".
     */
    private String resolveAvatarUrl(String rawUrl) {
        boolean mayManageOwnAvatar = userContextPort.isSystemAdmin()
            || userContextPort.isSchoolAdmin()
            || userContextPort.isTeacher();
        if (!mayManageOwnAvatar) {
            throw new ForbiddenException("A student's avatar is updated by the school");
        }

        // null/empty = request to REMOVE the photo; told apart from "field not sent" by the avatarUrlProvided flag.
        return AvatarUrlPolicy.normalizeOrThrow(avatarConfigPort.allowedHosts(), rawUrl);
    }
}

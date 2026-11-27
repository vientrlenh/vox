package com.sep.vox.application.port.input.usecase.schoolclass;

import com.sep.vox.domain.model.school.SchoolClassUser;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

/**
 * Shared access check for the teacher's "my class" use cases.
 * Kept in one place because all three use cases need exactly the same rule, and splitting it makes it easy to drift.
 */
@Service
@RequiredArgsConstructor
public class MyClassAccessGuard {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;

    /** Returns the caller's id after making sure the account is still active. */
    public UUID requireActiveCaller() {
        UUID callerId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(callerId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The user status is invalid");
        }
        return callerId;
    }

    /** As above, and additionally requires the caller to belong to the requested school. */
    public UUID requireSchoolMembership(UUID schoolId) {
        UUID callerId = requireActiveCaller();
        if (!schoolUserRepository.existsBySchoolIdAndUserId(schoolId, callerId)) {
            throw new ForbiddenException("The access permission is invalid");
        }
        return callerId;
    }

    /**
     * As above, and additionally requires the caller to be an active member of the class.
     * Throws NotFound on purpose rather than Forbidden: someone outside the class must not be able to tell
     * "the class does not exist" from "the class exists but is not mine".
     */
    public UUID requireClassMembership(UUID schoolClassId) {
        UUID callerId = requireActiveCaller();
        SchoolClassUser membership = schoolClassUserRepository
            .findByUserIdAndSchoolClassId(callerId, schoolClassId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (!membership.isActive()) {
            throw new ResourceNotFoundException("Class not found");
        }
        return callerId;
    }
}

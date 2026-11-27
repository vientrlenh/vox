package com.sep.vox.application.port.input.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;

/**
 * "Which school's data may be read" -- a system admin reads every school, a school admin reads only
 * their own school.
 *
 * <p>Split into one place because this is NOT a role but a relationship between the caller and the {@code schoolId}
 * they ask for, so {@code @PreAuthorize} on the controller cannot express it: {@code hasRole('SYSTEM_ADMIN')}
 * would block the school admin itself, and leaving it open would let one school read another school's balance. The check
 * must live in the use case, which knows both sides -- and so it was copied into every use case that reads by school.
 */
@Service
@RequiredArgsConstructor
public class SchoolScopedReadGuard {

    private final UserContextPort userContextPort;

    public void requireCanRead(UUID schoolId) {
        if (!userContextPort.isSystemAdmin() && !schoolId.equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
    }
}

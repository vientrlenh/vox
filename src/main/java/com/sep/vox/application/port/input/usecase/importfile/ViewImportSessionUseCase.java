package com.sep.vox.application.port.input.usecase.importfile;

import com.sep.vox.domain.model.school.School;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.importfile.ImportSessionResponseMapper;
import com.sep.vox.application.port.input.query.importfile.ViewImportSessionQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.importfile.ImportSessionDetailsResponse;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewImportSessionUseCase implements IUseCase<ViewImportSessionQuery, ImportSessionDetailsResponse> {

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final ImportSessionResponseMapper importSessionResponseMapper;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public ImportSessionDetailsResponse execute(ViewImportSessionQuery input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Import session must not be empty");
        }

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        ImportSession session = findSession(input.importSessionId());

        // SCHOOL_ADMIN keeps the old behaviour: may see every import session of their school (not only the ones they
        // created) to supervise bulk imports. Any other caller (TEACHER/SYSTEM_ADMIN, e.g. a Question import) may only
        // see the sessions they created themselves - no school is required (System Admin has no school).
        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (isSchoolAdmin) {
            UUID schoolId = getSchoolId(currentUser);
            validateSchool(schoolId);
            validateSessionBelongsToSchool(session, schoolId);
        } else if (!session.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("The import session does not belong to you");
        }

        return importSessionResponseMapper.toDetails(session);
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Current user is not active");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("Current user does not belong to any school"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("School is not active");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Import session not found"));
    }

    private void validateSessionBelongsToSchool(ImportSession session, UUID schoolId) {
        if (!Objects.equals(session.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("The import session does not belong to the current school");
        }
    }
}

package com.sep.vox.application.port.input.usecase.importfile;

import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.school.School;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.mapper.importfile.ImportRowResponseMapper;
import com.sep.vox.application.port.input.query.importfile.ViewImportRowsQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.input.importfile.ImportRowResponse;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ViewImportRowsUseCase implements IUseCase<ViewImportRowsQuery, PageResult<ImportRowResponse>> {

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final ImportRowResponseMapper importRowResponseMapper;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ImportRowResponse> execute(ViewImportRowsQuery input) {
        validateInput(input);

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        ImportSession session = findSession(input.sessionId());

        requireSessionAccess(session, currentUser, currentUserId);

        PageResult<ImportRow> rows = importRowRepository.findBySessionId(
            session.getId(),
            input.status(),
            input.page(), 
            input.size()
        );
        return importRowResponseMapper.toResponsePage(rows);
    }

    private void validateInput(ViewImportRowsQuery input) {
        if (input == null || input.sessionId() == null) {
            throw new IllegalArgumentException("Import session must not be empty");
        }
        if (input.page() <= 0 || input.size() <= 0) {
            throw new IllegalArgumentException("Invalid page number and page size");
        }
    }

    private void requireSessionAccess(ImportSession session, User currentUser, UUID currentUserId) {
        boolean isSchoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (isSchoolAdmin) {
            UUID schoolId = getSchoolId(currentUser);
            validateSchool(schoolId);
            validateSessionBelongsToSchool(session, schoolId);
        } else if (!session.isCreatedBy(currentUserId)) {
            throw new ForbiddenException("The import session does not belong to you");
        }
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

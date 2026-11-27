package com.sep.vox.application.port.input.usecase.schooluser;

import com.sep.vox.domain.model.school.School;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schooluser.AcceptSchoolUserImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AcceptSchoolUserImportUseCase
        implements IUseCase<AcceptSchoolUserImportCommand, Void> {

    // startDate/endDate are required only for students, so they are checked per row
    // (SchoolUserImportCommitHandler) and not enforced in the file-level mapping.
    private static final Set<String> REQUIRED_FIELDS = Set.of("email", "fullName", "roleCode", "phone",
            "dateOfBirth", "address");
    private static final String USER_TYPE = "USER";

    private final ImportSessionRepository importSessionRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final SchoolRepository schoolRepository;

    @Override
    @Transactional
    public Void execute(AcceptSchoolUserImportCommand input) {
        validateCommand(input);

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchoolIsActive(schoolId);

        ImportSession session = findSession(input.importSessionId());
        validateSession(session, schoolId, now);
        validateRequiredMapping(input.confirmedMapping());

        queueImport(input, now);
        return null;
    }

    private void validateSchoolIsActive(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("The school is not active");
        }
    }

    private void queueImport(AcceptSchoolUserImportCommand input, Instant now) {
        String confirmedMappingJson = jsonSerializationPort.toJson(input.confirmedMapping());
        int queued = importSessionRepository.markQueued(
                input.importSessionId(), USER_TYPE, confirmedMappingJson, now);
        if (queued == 0) {
            throw new IllegalArgumentException("The import session is not in a state that allows accepting, or it has expired");
        }
    }

    private void validateCommand(AcceptSchoolUserImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("The import session must not be empty");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("The import mapping must not be empty");
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("The current user does not belong to any school"));
        return schoolUser.getSchoolId();
    }

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("The school must not be empty");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("The school does not match the current user");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Import session not found"));
    }

    private void validateSession(ImportSession session, UUID schoolId, Instant now) {
        if (session.getType() != ImportType.USER) {
            throw new IllegalArgumentException("The import session is not a user import");
        }
        if (!Objects.equals(session.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("The import session does not belong to the current school");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalArgumentException("The import session is not in a state that allows accepting");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalArgumentException("The import session has expired");
        }
    }

    private void validateRequiredMapping(Map<String, String> confirmedMapping) {
        Set<String> mappedFields = new HashSet<String>();
        confirmedMapping.values().stream()
                .filter(value -> value != null)
                .map(value -> value.strip())
                .forEach(mappedFields::add);
        List<String> missingFields = REQUIRED_FIELDS.stream()
                .filter(field -> !mappedFields.contains(field))
                .toList();
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "The import mapping is missing required fields: " + String.join(", ", missingFields));
        }
    }
}

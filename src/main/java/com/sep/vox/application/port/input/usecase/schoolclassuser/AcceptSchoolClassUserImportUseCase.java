package com.sep.vox.application.port.input.usecase.schoolclassuser;

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
import com.sep.vox.application.port.input.command.schoolclassuser.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AcceptSchoolClassUserImportUseCase implements IUseCase<AcceptSchoolClassUserImportCommand, Void> {

    private static final String SCHOOL_CLASS_USER_TYPE = "SCHOOL_CLASS_USER";
    private static final Set<String> REQUIRED_FIELDS = Set.of("email", "classCode");

    private final ImportSessionRepository importSessionRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public Void execute(AcceptSchoolClassUserImportCommand input) {
        validateCommand(input);

        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = findCurrentUser(currentUserId);
        UUID schoolId = getSchoolId(currentUser);
        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);

        ImportSession session = findSession(input.importSessionId());
        validateSession(session, schoolId, now);
        validateRequiredMapping(input.confirmedMapping());

        String confirmedMappingJson = jsonSerializationPort.toJson(input.confirmedMapping());
        int queued = importSessionRepository.markQueued(
            input.importSessionId(), SCHOOL_CLASS_USER_TYPE, confirmedMappingJson, now);
        if (queued == 0) {
            throw new IllegalArgumentException("The import session is not in a state that allows accepting, or it has expired");
        }
        return null;
    }

    private void validateCommand(AcceptSchoolClassUserImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("The import session must not be empty");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("The import mapping must not be empty");
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("The current user is not active");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("The current user does not belong to any school"));
        return schoolUser.getSchoolId();
    }

    private void validateSchool(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new ResourceNotFoundException("School not found"));
        if (!school.isActive()) {
            throw new IllegalArgumentException("The school is not active");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Import session not found"));
    }

    private void validateSession(ImportSession session, UUID schoolId, Instant now) {
        if (session.getType() != ImportType.SCHOOL_CLASS_USER) {
            throw new IllegalArgumentException("The import session is not a class user import");
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

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("The school must not be empty");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("The school does not match the current user");
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
            throw new IllegalArgumentException("The import mapping is missing required fields: " + String.join(", ", missingFields));
        }
    }
}

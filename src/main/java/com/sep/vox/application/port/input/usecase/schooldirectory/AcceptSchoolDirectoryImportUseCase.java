package com.sep.vox.application.port.input.usecase.schooldirectory;

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
import com.sep.vox.application.port.input.command.schooldirectory.AcceptSchoolDirectoryImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
@RequiredArgsConstructor
public class AcceptSchoolDirectoryImportUseCase
        implements IUseCase<AcceptSchoolDirectoryImportCommand, Void> {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "code", "name", "provinceCode", "provinceName", "districtName", "address");
    private static final String SCHOOL_DIRECTORY_TYPE = "SCHOOL_DIRECTORY";

    private final ImportSessionRepository importSessionRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public Void execute(AcceptSchoolDirectoryImportCommand input) {
        validateCommand(input);

        Instant now = Instant.now();
        // Throws when nobody is authenticated; the session itself is not tied to the caller.
        userContextPort.getCurrentAuthenticatedUserId();

        ImportSession session = findSession(input.importSessionId());
        validateSession(session, now);
        validateRequiredMapping(input.confirmedMapping());

        String confirmedMappingJson = jsonSerializationPort.toJson(input.confirmedMapping());
        int queued = importSessionRepository.markQueued(
                input.importSessionId(), SCHOOL_DIRECTORY_TYPE, confirmedMappingJson, now);
        if (queued == 0) {
            throw new IllegalArgumentException("The import session is not in a state that allows accepting, or it has expired");
        }
        return null;
    }

    private void validateCommand(AcceptSchoolDirectoryImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("The import session must not be empty");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("The import mapping must not be empty");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Import session not found"));
    }

    private void validateSession(ImportSession session, Instant now) {
        if (session.getType() != ImportType.SCHOOL_DIRECTORY) {
            throw new IllegalArgumentException("The import session is not a school directory import");
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
                .filter(Objects::nonNull)
                .map(f -> f.strip())
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

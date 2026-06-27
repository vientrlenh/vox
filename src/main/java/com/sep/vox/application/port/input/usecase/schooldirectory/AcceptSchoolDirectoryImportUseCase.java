package com.sep.vox.application.port.input.usecase.schooldirectory;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptSchoolDirectoryImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.ImportSessionRepository;

@Service
public class AcceptSchoolDirectoryImportUseCase
        implements IUseCase<AcceptSchoolDirectoryImportCommand, Void> {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "code", "name", "provinceCode", "provinceName", "districtName", "address");
    private static final String SCHOOL_DIRECTORY_TYPE = "SCHOOL_DIRECTORY";

    private final ImportSessionRepository importSessionRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public AcceptSchoolDirectoryImportUseCase(
            ImportSessionRepository importSessionRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort) {
        this.importSessionRepository = importSessionRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public Void execute(AcceptSchoolDirectoryImportCommand input) {
        validateCommand(input);

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var session = findSession(input.importSessionId());
        validateSession(session, now);
        validateRequiredMapping(input.confirmedMapping());

        var confirmedMappingJson = jsonSerializationPort.toJson(input.confirmedMapping());
        var queued = importSessionRepository.markQueued(
                input.importSessionId(), SCHOOL_DIRECTORY_TYPE, confirmedMappingJson, now, currentUserId);
        if (queued == 0) {
            throw new IllegalStateException("Phiên import không ở trạng thái cho accept hoặc đã hết hạn");
        }
        return null;
    }

    private void validateCommand(AcceptSchoolDirectoryImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import không được để trống");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import"));
    }

    private void validateSession(ImportSession session, OffsetDateTime now) {
        if (session.getType() != ImportType.SCHOOL_DIRECTORY) {
            throw new IllegalArgumentException("Phiên import không phải là import danh mục trường");
        }
        if (session.getStatus() != ImportSessionStatus.PREVIEWED) {
            throw new IllegalStateException("Phiên import không ở trạng thái cho accept");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(now)) {
            session.setStatus(ImportSessionStatus.EXPIRED);
            importSessionRepository.save(session);
            throw new IllegalStateException("Phiên import đã hết hạn");
        }
    }

    private void validateRequiredMapping(Map<String, String> confirmedMapping) {
        var mappedFields = new HashSet<String>();
        confirmedMapping.values().stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .forEach(mappedFields::add);
        var missingFields = REQUIRED_FIELDS.stream()
                .filter(field -> !mappedFields.contains(field))
                .toList();
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "Mapping import thiếu trường bắt buộc: " + String.join(", ", missingFields));
        }
    }
}

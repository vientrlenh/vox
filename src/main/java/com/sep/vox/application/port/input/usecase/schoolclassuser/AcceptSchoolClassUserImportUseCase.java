package com.sep.vox.application.port.input.usecase.schoolclassuser;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptSchoolClassUserImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassUserImportResponse;
import com.sep.vox.application.service.UserSchoolResolver;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolClassStatus;
import com.sep.vox.domain.model.school.SchoolClassUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class AcceptSchoolClassUserImportUseCase implements IUseCase<AcceptSchoolClassUserImportCommand, AcceptSchoolClassUserImportResponse> {

    private static final Set<String> REQUIRED_FIELDS = Set.of("email", "classCode");
    private static final Set<String> SUPPORTED_FIELDS = Set.of("email", "classCode");

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final UserSchoolResolver userSchoolResolver;

    public AcceptSchoolClassUserImportUseCase(
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            SchoolClassUserRepository schoolClassUserRepository,
            SchoolClassRepository schoolClassRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort,
            UserSchoolResolver userSchoolResolver) {
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.schoolClassUserRepository = schoolClassUserRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.userSchoolResolver = userSchoolResolver;
    }

    @Override
    @Transactional
    public AcceptSchoolClassUserImportResponse execute(AcceptSchoolClassUserImportCommand input) {
        validateCommand(input);

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var currentUser = findCurrentUser(currentUserId);
        var schoolId = getSchoolId(currentUser);
        validateRequestedSchool(input.schoolId(), schoolId);
        validateSchool(schoolId);

        var session = findSession(input.importSessionId());
        validateSession(session, schoolId, now);
        validateRequiredMapping(input.confirmedMapping());

        session.setStatus(ImportSessionStatus.IMPORTING);
        session.setConfirmedMappingJson(jsonSerializationPort.toJson(input.confirmedMapping()));
        session.setUpdatedAt(now);
        session.setUpdatedBy(currentUserId);
        importSessionRepository.save(session);

        var rows = importRowRepository.findBySessionIdOrderByRowNumber(session.getId());
        var importedRows = processRows(rows, input.confirmedMapping(), schoolId, currentUserId, now);
        var invalidRows = rows.size() - importedRows;

        importRowRepository.saveAll(rows);
        session.setImportedRows(importedRows);
        session.setInvalidRows(invalidRows);
        session.setValidRows(importedRows);
        session.setSkippedRows(0L);
        session.setTotalRows(rows.size());
        session.setStatus(ImportSessionStatus.COMPLETED);
        session.setUpdatedAt(now);
        session.setUpdatedBy(currentUserId);
        var savedSession = importSessionRepository.save(session);

        return new AcceptSchoolClassUserImportResponse(
            savedSession.getId(),
            savedSession.getTotalRows(),
            savedSession.getImportedRows(),
            savedSession.getInvalidRows(),
            savedSession.getSkippedRows(),
            savedSession.getStatus().name()
        );
    }

    private void validateCommand(AcceptSchoolClassUserImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import không được để trống");
        }
    }

    private User findCurrentUser(UUID currentUserId) {
        var user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng hiện tại"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Người dùng hiện tại không hoạt động");
        }
        return user;
    }

    private UUID getSchoolId(User currentUser) {
        var schoolId = userSchoolResolver.findSchoolId(currentUser.getId()).orElse(null);
        if (schoolId == null) {
            throw new IllegalStateException("Người dùng hiện tại không thuộc trường nào");
        }
        return schoolId;
    }

    private void validateSchool(UUID schoolId) {
        var school = schoolRepository.findById(schoolId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy trường học"));
        if (!school.isActive()) {
            throw new IllegalStateException("Trường học không hoạt động");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import"));
    }

    private void validateSession(ImportSession session, UUID schoolId, OffsetDateTime now) {
        if (session.getType() != ImportType.SCHOOL_CLASS_USER) {
            throw new IllegalArgumentException("Phiên import không phải là import người dùng vào lớp học");
        }
        if (!Objects.equals(session.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Phiên import không thuộc trường hiện tại");
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

    private void validateRequestedSchool(UUID requestedSchoolId, UUID currentSchoolId) {
        if (requestedSchoolId == null) {
            throw new IllegalArgumentException("Trường học không được để trống");
        }
        if (!Objects.equals(requestedSchoolId, currentSchoolId)) {
            throw new IllegalArgumentException("Trường học không khớp với người dùng hiện tại");
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
            throw new IllegalArgumentException("Mapping import thiếu trường bắt buộc: " + String.join(", ", missingFields));
        }
    }

    private long processRows(List<ImportRow> rows, Map<String, String> confirmedMapping, UUID schoolId, UUID currentUserId, OffsetDateTime now) {
        var importedRows = 0L;
        var seenMemberships = new HashSet<String>();

        for (var row : rows) {
            var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, confirmedMapping);
            var normalized = normalize(mappedData);
            var validation = validateRow(normalized, schoolId, seenMemberships);

            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            if (!validation.errors().isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(validation.errors()));
                row.setStatus(ImportRowStatus.INVALID);
                continue;
            }

            schoolClassUserRepository.save(new SchoolClassUser(
                validation.user().getId(),
                validation.schoolClass().getId(),
                true,
                now,
                null,
                currentUserId
            ));
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
            importedRows++;
        }

        return importedRows;
    }

    private Map<String, String> mapRawData(Map<String, String> rawData, Map<String, String> confirmedMapping) {
        var mappedData = new LinkedHashMap<String, String>();
        rawData.forEach((originalHeader, value) -> {
            var systemField = confirmedMapping.get(originalHeader);
            if (systemField != null) {
                systemField = systemField.strip();
            }
            if (SUPPORTED_FIELDS.contains(systemField)) {
                mappedData.put(systemField, value);
            }
        });
        return mappedData;
    }

    private Map<String, String> normalize(Map<String, String> mappedData) {
        var normalized = new LinkedHashMap<String, String>();
        normalized.put("email", normalizeEmail(mappedData.get("email")));
        normalized.put("classCode", StringNormalization.normalizeCode(mappedData.get("classCode")));
        return normalized;
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    private RowValidation validateRow(Map<String, String> mappedData, UUID schoolId, Set<String> seenMemberships) {
        var errors = new java.util.ArrayList<Map<String, String>>();
        addMissingError(errors, mappedData, "email", "Email không được để trống");
        addMissingError(errors, mappedData, "classCode", "Mã lớp không được để trống");

        var email = mappedData.get("email");
        var classCode = mappedData.get("classCode");
        if (isPresent(email) && isPresent(classCode) && !seenMemberships.add(email + "|" + classCode)) {
            errors.add(error("email", "Người dùng và lớp học bị trùng trong file import"));
        }

        User user = null;
        if (isPresent(email)) {
            var foundUser = userRepository.findByEmail(email);
            if (foundUser.isEmpty()) {
                errors.add(error("email", "Không tìm thấy người dùng"));
            } else {
                user = foundUser.get();
                if (user.getStatus() != UserStatus.ACTIVE) {
                    errors.add(error("email", "Người dùng không hoạt động"));
                }
                if (!Objects.equals(userSchoolResolver.findSchoolId(user.getId()).orElse(null), schoolId)) {
                    errors.add(error("email", "Người dùng không thuộc trường hiện tại"));
                }
            }
        }

        SchoolClass schoolClass = null;
        if (isPresent(classCode)) {
            var foundClass = schoolClassRepository.findBySchoolIdAndCode(schoolId, classCode);
            if (foundClass.isEmpty()) {
                errors.add(error("classCode", "Không tìm thấy lớp học"));
            } else {
                schoolClass = foundClass.get();
                if (schoolClass.getStatus() != SchoolClassStatus.ACTIVE) {
                    errors.add(error("classCode", "Lớp học không hoạt động"));
                }
            }
        }

        if (user != null && schoolClass != null
                && schoolClassUserRepository.findByUserIdAndSchoolClassId(user.getId(), schoolClass.getId()).isPresent()) {
            errors.add(error("email", "Người dùng đã thuộc lớp học"));
        }

        return new RowValidation(user, schoolClass, errors);
    }

    private void addMissingError(List<Map<String, String>> errors, Map<String, String> mappedData, String field, String message) {
        if (!isPresent(mappedData.get(field))) {
            errors.add(error(field, message));
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private record RowValidation(User user, SchoolClass schoolClass, List<Map<String, String>> errors) {
    }
}

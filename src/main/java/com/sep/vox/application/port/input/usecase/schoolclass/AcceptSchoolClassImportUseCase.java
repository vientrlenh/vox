package com.sep.vox.application.port.input.usecase.schoolclass;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.JsonSerialization;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class AcceptSchoolClassImportUseCase implements IUseCase<AcceptSchoolClassImportCommand, AcceptSchoolClassImportResponse> {

    private static final Set<String> REQUIRED_FIELDS = Set.of("code", "name", "languageCode", "schoolGradeCode");
    private static final Set<String> SUPPORTED_FIELDS = Set.of("code", "name", "languageCode", "schoolGradeCode", "description");

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;

    public AcceptSchoolClassImportUseCase(
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            SchoolClassRepository schoolClassRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            SchoolGradeRepository schoolGradeRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort) {
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public AcceptSchoolClassImportResponse execute(AcceptSchoolClassImportCommand input) {
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
        session.setConfirmedMappingJson(JsonSerialization.toJson(input.confirmedMapping()));
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

        return new AcceptSchoolClassImportResponse(
            savedSession.getId(),
            savedSession.getTotalRows(),
            savedSession.getImportedRows(),
            savedSession.getInvalidRows(),
            savedSession.getSkippedRows(),
            savedSession.getStatus().name()
        );
    }

    private void validateCommand(AcceptSchoolClassImportCommand input) {
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
        var schoolId = currentUser.getSchoolId();
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
        if (session.getType() != ImportType.SCHOOL_CLASS) {
            throw new IllegalArgumentException("Phiên import không phải là import lớp học");
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
        var seenCodes = new HashSet<String>();

        for (var row : rows) {
            var rawData = JsonSerialization.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, confirmedMapping);
            var normalized = normalize(mappedData);
            var errors = validateRow(normalized, schoolId, seenCodes);

            row.setMappedDataJson(JsonSerialization.toJson(normalized));
            if (!errors.isEmpty()) {
                row.setErrorsJson(JsonSerialization.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                continue;
            }

            try {
                var schoolClass = SchoolClass.create(
                    schoolId,
                    supportedLanguageRepository.findByCode(normalized.get("languageCode")).orElseThrow().getId(),
                    schoolGradeRepository.findBySchoolIdAndCode(schoolId, normalized.get("schoolGradeCode")).orElseThrow().getId(),
                    normalized.get("code"),
                    normalized.get("name"),
                    normalized.get("description"),
                    currentUserId,
                    now
                );
                schoolClassRepository.save(schoolClass);
                row.setErrorsJson(null);
                row.setStatus(ImportRowStatus.IMPORTED);
                importedRows++;
            } catch (IllegalArgumentException exception) {
                row.setErrorsJson(JsonSerialization.toJson(List.of(error("code", exception.getMessage()))));
                row.setStatus(ImportRowStatus.INVALID);
            }
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
        normalized.put("code", StringNormalization.normalizeCode(mappedData.get("code")));
        normalized.put("name", StringNormalization.trimAndCollapseSpaces(mappedData.get("name")));
        normalized.put("languageCode", StringNormalization.normalizeCode(mappedData.get("languageCode")));
        normalized.put("schoolGradeCode", StringNormalization.normalizeCode(mappedData.get("schoolGradeCode")));
        normalized.put("description", StringNormalization.trimAndCollapseSpaces(mappedData.get("description")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> mappedData, UUID schoolId, Set<String> seenCodes) {
        var errors = new java.util.ArrayList<Map<String, String>>();
        addMissingError(errors, mappedData, "code", "Mã lớp không được để trống");
        addMissingError(errors, mappedData, "name", "ên lớp không được để trống");
        addMissingError(errors, mappedData, "languageCode", "Mã ngôn ngữ không được để trống");
        addMissingError(errors, mappedData, "schoolGradeCode", "Mã khối không được để trống");

        var code = mappedData.get("code");
        if (isPresent(code)) {
            if (!seenCodes.add(code)) {
                errors.add(error("code", "Mã lớp bị trùng trong file import"));
            } else if (schoolClassRepository.findBySchoolIdAndCode(schoolId, code).isPresent()) {
                errors.add(error("code", "Mã lớp đã tồn tại trong hệ thống"));
            }
        }

        var languageCode = mappedData.get("languageCode");
        if (isPresent(languageCode)) {
            var language = supportedLanguageRepository.findByCode(languageCode);
            if (language.isEmpty()) {
                errors.add(error("languageCode", "Không tìm thấy ngôn ngữ"));
            } else if (!language.get().isActive()) {
                errors.add(error("languageCode", "Ngôn ngữ không hoạt động"));
            }
        }

        var schoolGradeCode = mappedData.get("schoolGradeCode");
        if (isPresent(schoolGradeCode)) {
            var schoolGrade = schoolGradeRepository.findBySchoolIdAndCode(schoolId, schoolGradeCode);
            if (schoolGrade.isEmpty()) {
                errors.add(error("schoolGradeCode", "Không tìm thấy khối học"));
            } else if (schoolGrade.get().getStatus() != SchoolGradeStatus.ACTIVE) {
                errors.add(error("schoolGradeCode", "Khối học không hoạt động"));
            }
        }

        return errors;
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
}

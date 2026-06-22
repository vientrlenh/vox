package com.sep.vox.application.port.input.usecase.schoolclass;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptSchoolClassImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptSchoolClassImportResponse;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
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
    private final JsonSerializationPort jsonSerializationPort;
    private final SchoolUserRepository schoolUserRepository;

    public AcceptSchoolClassImportUseCase(
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            SchoolClassRepository schoolClassRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            SchoolGradeRepository schoolGradeRepository,
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort,
            SchoolUserRepository schoolUserRepository) {
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.schoolGradeRepository = schoolGradeRepository;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.schoolUserRepository = schoolUserRepository;
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
        session.setConfirmedMappingJson(jsonSerializationPort.toJson(input.confirmedMapping()));
        session.setUpdatedAt(now);
        session.setUpdatedBy(currentUserId);
        importSessionRepository.save(session);

        var rows = importRowRepository.findBySessionIdOrderByRowNumber(session.getId());
        validateMappingKeys(rows, input.confirmedMapping());
        var result = processRows(rows, input.confirmedMapping(), schoolId, currentUserId, now);
        var invalidRows = rows.size() - result.importedRows();

        importRowRepository.saveAll(rows);
        session.setImportedRows(result.importedRows());
        session.setInvalidRows(invalidRows);
        session.setValidRows(result.importedRows());
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
            result.updatedRows(),
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
        return schoolUserRepository.findByUserId(currentUser.getId())
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new IllegalStateException("Người dùng hiện tại không thuộc trường nào"));
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

    /**
     * Defense-in-depth: bảo đảm mọi key trong confirmedMapping là cột thực sự có trong file đã preview,
     * và mọi value là trường hệ thống được hỗ trợ. Lưu ý: việc này KHÔNG ngăn admin hợp lệ tự chọn
     * mapping sai (vd. hoán đổi code↔name) — đó là quyền của admin trong luồng preview → confirm → accept.
     */
    private void validateMappingKeys(List<ImportRow> rows, Map<String, String> confirmedMapping) {
        if (rows.isEmpty()) {
            return;
        }
        var validHeaders = jsonSerializationPort.toStringMap(rows.get(0).getRawDataJson()).keySet();
        var invalidKeys = confirmedMapping.keySet().stream()
            .filter(key -> !validHeaders.contains(key))
            .toList();
        if (!invalidKeys.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa cột không tồn tại trong file: " + String.join(", ", invalidKeys));
        }
        var invalidValues = confirmedMapping.values().stream()
            .filter(Objects::nonNull)
            .map(String::strip)
            .filter(value -> !value.isEmpty() && !SUPPORTED_FIELDS.contains(value))
            .toList();
        if (!invalidValues.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa trường hệ thống không hợp lệ: " + String.join(", ", invalidValues));
        }
    }

    private ProcessResult processRows(List<ImportRow> rows, Map<String, String> confirmedMapping, UUID schoolId, UUID currentUserId, OffsetDateTime now) {
        var importedRows = 0L;
        var updatedRows = 0L;
        var seenCodes = new HashSet<String>();
        var rowContexts = new ArrayList<RowContext>();
        var classCodes = new HashSet<String>();
        var languageCodes = new HashSet<String>();
        var schoolGradeCodes = new HashSet<String>();

        for (var row : rows) {
            var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, confirmedMapping);
            var normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));

            addIfPresent(classCodes, normalized.get("code"));
            addIfPresent(languageCodes, normalized.get("languageCode"));
            addIfPresent(schoolGradeCodes, normalized.get("schoolGradeCode"));
        }

        var existingClassesByCode = findExistingClassesByCode(schoolId, classCodes);
        var languagesByCode = findLanguagesByCode(languageCodes);
        var gradesByCode = findGradesByCode(schoolId, schoolGradeCodes);

        for (var rowContext : rowContexts) {
            var row = rowContext.row();
            var normalized = rowContext.normalized();
            var errors = validateRow(normalized, seenCodes, languagesByCode, gradesByCode);

            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                continue;
            }

            try {
                var language = languagesByCode.get(normalized.get("languageCode"));
                var schoolGrade = gradesByCode.get(normalized.get("schoolGradeCode"));
                var schoolClass = existingClassesByCode.get(normalized.get("code"));
                if (schoolClass == null) {
                    schoolClass = SchoolClass.create(
                        schoolId,
                        language.getId(),
                        schoolGrade.getId(),
                        normalized.get("code"),
                        normalized.get("name"),
                        normalized.get("description"),
                        currentUserId,
                        now
                    );
                } else {
                    schoolClass.setName(normalized.get("name"));
                    schoolClass.setDescription(normalized.get("description"));
                    schoolClass.setLanguageId(language.getId());
                    schoolClass.setSchoolGradeId(schoolGrade.getId());
                    schoolClass.setUpdatedAt(now);
                    schoolClass.setUpdatedBy(currentUserId);
                    updatedRows++;
                }
                schoolClassRepository.save(schoolClass);
                row.setErrorsJson(null);
                row.setStatus(ImportRowStatus.IMPORTED);
                importedRows++;
            } catch (IllegalArgumentException exception) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", exception.getMessage()))));
                row.setStatus(ImportRowStatus.INVALID);
            }
        }

        return new ProcessResult(importedRows, updatedRows);
    }

    private Map<String, SchoolClass> findExistingClassesByCode(UUID schoolId, Set<String> codes) {
        var existingClassesByCode = new LinkedHashMap<String, SchoolClass>();
        schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .forEach(schoolClass -> existingClassesByCode.putIfAbsent(schoolClass.getCode().value(), schoolClass));
        return existingClassesByCode;
    }

    private Map<String, SupportedLanguage> findLanguagesByCode(Set<String> codes) {
        var languagesByCode = new LinkedHashMap<String, SupportedLanguage>();
        supportedLanguageRepository.findByCodeIn(codes)
            .forEach(language -> languagesByCode.putIfAbsent(language.getCode().value(), language));
        return languagesByCode;
    }

    private Map<String, SchoolGrade> findGradesByCode(UUID schoolId, Set<String> codes) {
        var gradesByCode = new LinkedHashMap<String, SchoolGrade>();
        schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .forEach(grade -> gradesByCode.putIfAbsent(grade.getCode(), grade));
        return gradesByCode;
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

    private List<Map<String, String>> validateRow(Map<String, String> mappedData, Set<String> seenCodes,
            Map<String, SupportedLanguage> languagesByCode,
            Map<String, SchoolGrade> gradesByCode) {
        var errors = new ArrayList<Map<String, String>>();
        addMissingError(errors, mappedData, "code", "Mã lớp không được để trống");
        addMissingError(errors, mappedData, "name", "Tên lớp không được để trống");
        addMissingError(errors, mappedData, "languageCode", "Mã ngôn ngữ không được để trống");
        addMissingError(errors, mappedData, "schoolGradeCode", "Mã khối không được để trống");

        var code = mappedData.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Mã lớp bị trùng trong file import"));
        }

        var languageCode = mappedData.get("languageCode");
        if (isPresent(languageCode)) {
            var language = languagesByCode.get(languageCode);
            if (language == null) {
                errors.add(error("languageCode", "Không tìm thấy ngôn ngữ"));
            } else if (!language.isActive()) {
                errors.add(error("languageCode", "Ngôn ngữ không hoạt động"));
            }
        }

        var schoolGradeCode = mappedData.get("schoolGradeCode");
        if (isPresent(schoolGradeCode)) {
            var schoolGrade = gradesByCode.get(schoolGradeCode);
            if (schoolGrade == null) {
                errors.add(error("schoolGradeCode", "Không tìm thấy khối học"));
            } else if (schoolGrade.getStatus() != SchoolGradeStatus.ACTIVE) {
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

    private void addIfPresent(Set<String> values, String value) {
        if (isPresent(value)) {
            values.add(value);
        }
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private record RowContext(ImportRow row, Map<String, String> normalized) {
    }

    private record ProcessResult(long importedRows, long updatedRows) {
    }
}

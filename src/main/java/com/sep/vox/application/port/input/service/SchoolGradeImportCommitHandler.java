package com.sep.vox.application.port.input.service;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

@Service
public class SchoolGradeImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "schoolGradeLevelCode", "code", "name", "description", "startDate", "endDate");

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public SchoolGradeImportCommitHandler(
            SchoolGradeRepository schoolGradeRepository,
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.schoolGradeRepository = schoolGradeRepository;
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_GRADE;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        var schoolId = session.getSchoolId();
        var currentUserId = session.getCreatedBy();
        var mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        validateMappingKeys(rows, mapping);
        return processRows(rows, mapping, schoolId, currentUserId);
    }

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
            .map(value -> value == null ? null : value.strip())
            .filter(value -> value != null && !value.isEmpty() && !SUPPORTED_FIELDS.contains(value))
            .toList();
        if (!invalidValues.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa trường hệ thống không hợp lệ: " + String.join(", ", invalidValues));
        }
    }

    private ImportCommitResult processRows(List<ImportRow> rows, Map<String, String> confirmedMapping, UUID schoolId, UUID currentUserId) {
        var createdRows = 0L;
        var updatedRows = 0L;
        var invalidRows = 0L;
        var seenKeys = new HashSet<String>();
        var rowContexts = new ArrayList<RowContext>();
        var levelCodes = new HashSet<String>();

        for (var row : rows) {
            var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, confirmedMapping);
            var normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));
            addIfPresent(levelCodes, normalized.get("schoolGradeLevelCode"));
        }

        var levelsByCode = findGradeLevelsByCode(schoolId, levelCodes);

        for (var rowContext : rowContexts) {
            var row = rowContext.row();
            var normalized = rowContext.normalized();
            var errors = validateRow(normalized, seenKeys, levelsByCode);

            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            var gradeLevel = levelsByCode.get(normalized.get("schoolGradeLevelCode"));
            var startDate = DateMapper.toLocalDate(normalized.get("startDate"));
            var endDate = DateMapper.toLocalDate(normalized.get("endDate"));
            var existing = schoolGradeRepository
                    .findBySchoolGradeLevelIdAndCode(gradeLevel.getId(), normalized.get("code"))
                    .orElse(null);

            try {
                if (existing != null) {
                    transactionTemplate.executeWithoutResult(status ->
                            updateGrade(existing, normalized, startDate, endDate, currentUserId));
                    updatedRows++;
                } else {
                    transactionTemplate.executeWithoutResult(status ->
                            createGrade(normalized, gradeLevel.getId(), startDate, endDate, currentUserId));
                    createdRows++;
                }
            } catch (DataIntegrityViolationException | IllegalArgumentException exception) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "Mã năm học đã tồn tại hoặc không hợp lệ"))));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
                continue;
            }

            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
        }

        return new ImportCommitResult(createdRows, updatedRows, 0L, invalidRows);
    }

    private void createGrade(Map<String, String> data, UUID schoolGradeLevelId, LocalDate startDate, LocalDate endDate, UUID currentUserId) {
        var now = Instant.now();
        schoolGradeRepository.save(new SchoolGrade(
                schoolGradeLevelId,
                data.get("code"),
                data.get("name"),
                data.get("description"),
                startDate,
                endDate,
                SchoolGradeStatus.INACTIVE,
                now,
                now,
                currentUserId,
                currentUserId));
    }

    private void updateGrade(SchoolGrade existing, Map<String, String> data, LocalDate startDate, LocalDate endDate, UUID currentUserId) {
        var now = Instant.now();
        existing.setName(data.get("name"));
        existing.setDescription(data.get("description"));
        existing.setStartDate(startDate);
        existing.setEndDate(endDate);
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(currentUserId);
        schoolGradeRepository.save(existing);
    }

    private Map<String, SchoolGradeLevel> findGradeLevelsByCode(UUID schoolId, Set<String> codes) {
        var levelsByCode = new LinkedHashMap<String, SchoolGradeLevel>();
        schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .forEach(level -> levelsByCode.putIfAbsent(level.getCode(), level));
        return levelsByCode;
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
        normalized.put("schoolGradeLevelCode", StringNormalization.normalizeCode(mappedData.get("schoolGradeLevelCode")));
        normalized.put("code", StringNormalization.normalizeCode(mappedData.get("code")));
        normalized.put("name", StringNormalization.trimAndCollapseSpaces(mappedData.get("name")));
        normalized.put("description", StringNormalization.trimAndCollapseSpaces(mappedData.get("description")));
        normalized.put("startDate", trimToNull(mappedData.get("startDate")));
        normalized.put("endDate", trimToNull(mappedData.get("endDate")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, Set<String> seenKeys,
            Map<String, SchoolGradeLevel> levelsByCode) {
        var errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "schoolGradeLevelCode", "Mã khối không được để trống");
        addMissingError(errors, data, "code", "Mã năm học không được để trống");
        addMissingError(errors, data, "name", "Tên năm học không được để trống");
        addMissingError(errors, data, "startDate", "Ngày bắt đầu không được để trống");
        addMissingError(errors, data, "endDate", "Ngày kết thúc không được để trống");

        var levelCode = data.get("schoolGradeLevelCode");
        if (isPresent(levelCode) && !levelsByCode.containsKey(levelCode)) {
            errors.add(error("schoolGradeLevelCode", "Không tìm thấy khối lớp"));
        }

        var code = data.get("code");
        if (isPresent(levelCode) && isPresent(code) && !seenKeys.add(levelCode + "::" + code)) {
            errors.add(error("code", "Mã năm học bị trùng trong file import"));
        }

        var startDate = parseDateOrNull(data.get("startDate"));
        if (isPresent(data.get("startDate")) && startDate == null) {
            errors.add(error("startDate", "Ngày bắt đầu không hợp lệ"));
        }
        var endDate = parseDateOrNull(data.get("endDate"));
        if (isPresent(data.get("endDate")) && endDate == null) {
            errors.add(error("endDate", "Ngày kết thúc không hợp lệ"));
        }
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            errors.add(error("endDate", "Ngày kết thúc phải sau ngày bắt đầu"));
        }

        return errors;
    }

    private LocalDate parseDateOrNull(String value) {
        if (!isPresent(value)) {
            return null;
        }
        try {
            return DateMapper.toLocalDate(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void addMissingError(List<Map<String, String>> errors, Map<String, String> data, String field, String message) {
        if (!isPresent(data.get(field))) {
            errors.add(error(field, message));
        }
    }

    private void addIfPresent(Set<String> values, String value) {
        if (isPresent(value)) {
            values.add(value);
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        var stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private record RowContext(ImportRow row, Map<String, String> normalized) {}
}

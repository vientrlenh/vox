package com.sep.vox.application.port.input.service;

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

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolGradeLevel;
import com.sep.vox.domain.model.school.SchoolGradeLevelStatus;
import com.sep.vox.domain.repository.SchoolGradeLevelRepository;

@Service
public class SchoolGradeLevelImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of("code", "name", "order", "description");

    private final SchoolGradeLevelRepository schoolGradeLevelRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public SchoolGradeLevelImportCommitHandler(
            SchoolGradeLevelRepository schoolGradeLevelRepository,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.schoolGradeLevelRepository = schoolGradeLevelRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_GRADE_LEVEL;
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
        var seenCodes = new HashSet<String>();
        var rowContexts = new ArrayList<RowContext>();
        var codes = new HashSet<String>();

        for (var row : rows) {
            var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, confirmedMapping);
            var normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));
            addIfPresent(codes, normalized.get("code"));
        }

        var existingByCode = findExistingByCode(schoolId, codes);

        for (var rowContext : rowContexts) {
            var row = rowContext.row();
            var normalized = rowContext.normalized();
            var errors = validateRow(normalized, seenCodes);

            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            var order = Integer.parseInt(normalized.get("order"));
            var existing = existingByCode.get(normalized.get("code"));

            try {
                if (existing != null) {
                    transactionTemplate.executeWithoutResult(status ->
                            updateGradeLevel(existing, normalized, order, currentUserId));
                    updatedRows++;
                } else {
                    transactionTemplate.executeWithoutResult(status ->
                            createGradeLevel(normalized, order, schoolId, currentUserId));
                    createdRows++;
                }
            } catch (DataIntegrityViolationException | IllegalArgumentException exception) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "Mã khối hoặc thứ tự đã tồn tại"))));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
                continue;
            }

            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
        }

        return new ImportCommitResult(createdRows, updatedRows, 0L, invalidRows);
    }

    private void createGradeLevel(Map<String, String> data, int order, UUID schoolId, UUID currentUserId) {
        var now = Instant.now();
        schoolGradeLevelRepository.save(new SchoolGradeLevel(
                schoolId,
                data.get("code"),
                data.get("name"),
                data.get("description"),
                order,
                SchoolGradeLevelStatus.ACTIVE,
                now,
                now,
                currentUserId,
                currentUserId));
    }

    private void updateGradeLevel(SchoolGradeLevel existing, Map<String, String> data, int order, UUID currentUserId) {
        var now = Instant.now();
        existing.setName(data.get("name"));
        existing.setDescription(data.get("description"));
        existing.setOrder(order);
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(currentUserId);
        schoolGradeLevelRepository.save(existing);
    }

    private Map<String, SchoolGradeLevel> findExistingByCode(UUID schoolId, Set<String> codes) {
        var existingByCode = new LinkedHashMap<String, SchoolGradeLevel>();
        schoolGradeLevelRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .forEach(gradeLevel -> existingByCode.putIfAbsent(gradeLevel.getCode(), gradeLevel));
        return existingByCode;
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
        normalized.put("order", trimToNull(mappedData.get("order")));
        normalized.put("description", StringNormalization.trimAndCollapseSpaces(mappedData.get("description")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, Set<String> seenCodes) {
        var errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "code", "Mã khối không được để trống");
        addMissingError(errors, data, "name", "Tên khối không được để trống");
        addMissingError(errors, data, "order", "Thứ tự khối không được để trống");

        var code = data.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Mã khối bị trùng trong file import"));
        }

        var order = data.get("order");
        if (isPresent(order) && !isPositiveInteger(order)) {
            errors.add(error("order", "Thứ tự khối phải là số nguyên lớn hơn 0"));
        }

        return errors;
    }

    private boolean isPositiveInteger(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return false;
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

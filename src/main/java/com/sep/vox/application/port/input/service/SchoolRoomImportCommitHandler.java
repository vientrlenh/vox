package com.sep.vox.application.port.input.service;

import java.time.OffsetDateTime;
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
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;

@Service
public class SchoolRoomImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of("code", "name", "capacity", "description");

    private final SchoolRoomRepository schoolRoomRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public SchoolRoomImportCommitHandler(
            SchoolRoomRepository schoolRoomRepository,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.schoolRoomRepository = schoolRoomRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_ROOM;
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

            var capacity = Integer.parseInt(normalized.get("capacity"));
            var existing = existingByCode.get(normalized.get("code"));

            try {
                if (existing != null) {
                    transactionTemplate.executeWithoutResult(status ->
                            updateRoom(existing, normalized, capacity, currentUserId));
                    updatedRows++;
                } else {
                    transactionTemplate.executeWithoutResult(status ->
                            createRoom(normalized, capacity, schoolId, currentUserId));
                    createdRows++;
                }
            } catch (DataIntegrityViolationException | IllegalArgumentException exception) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "Mã phòng đã tồn tại"))));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
                continue;
            }

            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
        }

        return new ImportCommitResult(createdRows, updatedRows, 0L, invalidRows);
    }

    private void createRoom(Map<String, String> data, int capacity, UUID schoolId, UUID currentUserId) {
        var now = OffsetDateTime.now();
        schoolRoomRepository.save(new SchoolRoom(
                schoolId,
                data.get("code"),
                data.get("name"),
                data.get("description"),
                capacity,
                false,
                now,
                now,
                currentUserId,
                currentUserId));
    }

    private void updateRoom(SchoolRoom existing, Map<String, String> data, int capacity, UUID currentUserId) {
        var now = OffsetDateTime.now();
        existing.setName(data.get("name"));
        existing.setDescription(data.get("description"));
        existing.setCapacity(capacity);
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(currentUserId);
        schoolRoomRepository.save(existing);
    }

    private Map<String, SchoolRoom> findExistingByCode(UUID schoolId, Set<String> codes) {
        var existingByCode = new LinkedHashMap<String, SchoolRoom>();
        schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .forEach(room -> existingByCode.putIfAbsent(room.getCode(), room));
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
        normalized.put("capacity", trimToNull(mappedData.get("capacity")));
        normalized.put("description", StringNormalization.trimAndCollapseSpaces(mappedData.get("description")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, Set<String> seenCodes) {
        var errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "code", "Mã phòng không được để trống");
        addMissingError(errors, data, "name", "Tên phòng không được để trống");
        addMissingError(errors, data, "capacity", "Sức chứa phòng không được để trống");

        var code = data.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Mã phòng bị trùng trong file import"));
        }

        var capacity = data.get("capacity");
        if (isPresent(capacity) && !isPositiveInteger(capacity)) {
            errors.add(error("capacity", "Sức chứa phòng phải là số nguyên lớn hơn 0"));
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

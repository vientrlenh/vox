package com.sep.vox.application.port.input.service;

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

    private static final Set<String> SUPPORTED_FIELDS = Set.of("code", "name", "description");

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
        UUID schoolId = session.getSchoolId();
        Map<String, String> mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        validateMappingKeys(rows, mapping);
        return processRows(rows, mapping, schoolId);
    }

    private void validateMappingKeys(List<ImportRow> rows, Map<String, String> confirmedMapping) {
        if (rows.isEmpty()) {
            return;
        }
        Set<String> validHeaders = jsonSerializationPort.toStringMap(rows.get(0).getRawDataJson()).keySet();
        List<String> invalidKeys = confirmedMapping.keySet().stream()
            .filter(key -> !validHeaders.contains(key))
            .toList();
        if (!invalidKeys.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa cột không tồn tại trong file: " + String.join(", ", invalidKeys));
        }
        List<String> invalidValues = confirmedMapping.values().stream()
            .map(value -> value == null ? null : value.strip())
            .filter(value -> value != null && !value.isEmpty() && !SUPPORTED_FIELDS.contains(value))
            .toList();
        if (!invalidValues.isEmpty()) {
            throw new IllegalArgumentException("Mapping chứa trường hệ thống không hợp lệ: " + String.join(", ", invalidValues));
        }
    }

    private ImportCommitResult processRows(List<ImportRow> rows, Map<String, String> confirmedMapping, UUID schoolId) {
        long createdRows = 0L;
        long updatedRows = 0L;
        long invalidRows = 0L;
        Set<String> seenCodes = new HashSet<String>();
        List<RowContext> rowContexts = new ArrayList<RowContext>();
        Set<String> codes = new HashSet<String>();

        for (ImportRow row : rows) {
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = mapRawData(rawData, confirmedMapping);
            Map<String, String> normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));
            addIfPresent(codes, normalized.get("code"));
        }

        Map<String, SchoolRoom> existingByCode = findExistingByCode(schoolId, codes);

        for (RowContext rowContext : rowContexts) {
            ImportRow row = rowContext.row();
            Map<String, String> normalized = rowContext.normalized();
            List<Map<String, String>> errors = validateRow(normalized, seenCodes);

            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            SchoolRoom existing = existingByCode.get(normalized.get("code"));

            try {
                if (existing != null) {
                    transactionTemplate.executeWithoutResult(status ->
                            updateRoom(existing, normalized));
                    updatedRows++;
                } else {
                    transactionTemplate.executeWithoutResult(status ->
                            createRoom(normalized, schoolId));
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

    private void createRoom(Map<String, String> data, UUID schoolId) {
        schoolRoomRepository.save(SchoolRoom.builder()
            .schoolId(schoolId)
            .code(data.get("code"))
            .name(data.get("name"))
            .description(data.get("description"))
            // Same status as manual creation (AddSchoolRoomUseCase): isActive=false is the soft-delete flag,
            // DeleteSchoolRoomUseCase refuses to delete an inactive room and no API can re-activate it,
            // so an imported room would be stuck forever in the "deleted" state.
            .active(true)
            .build());
    }

    private void updateRoom(SchoolRoom existing, Map<String, String> data) {
        existing.setName(data.get("name"));
        existing.setDescription(data.get("description"));
        // Import lại mã phòng đang inactive = khôi phục phòng đó, vì không có API nào bật lại
        // và unique (school_id, code) chặn tạo mới cùng mã.
        existing.setActive(true);
        schoolRoomRepository.save(existing);
    }

    private Map<String, SchoolRoom> findExistingByCode(UUID schoolId, Set<String> codes) {
        Map<String, SchoolRoom> existingByCode = new LinkedHashMap<String, SchoolRoom>();
        schoolRoomRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .forEach(room -> existingByCode.putIfAbsent(room.getCode(), room));
        return existingByCode;
    }

    private Map<String, String> mapRawData(Map<String, String> rawData, Map<String, String> confirmedMapping) {
        Map<String, String> mappedData = new LinkedHashMap<String, String>();
        rawData.forEach((originalHeader, value) -> {
            String systemField = confirmedMapping.get(originalHeader);
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
        Map<String, String> normalized = new LinkedHashMap<String, String>();
        normalized.put("code", StringNormalization.normalizeCode(mappedData.get("code")));
        normalized.put("name", StringNormalization.trimAndCollapseSpaces(mappedData.get("name")));
        normalized.put("description", StringNormalization.trimAndCollapseSpaces(mappedData.get("description")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, Set<String> seenCodes) {
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "code", "Mã phòng không được để trống");
        addMissingError(errors, data, "name", "Tên phòng không được để trống");

        String code = data.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Mã phòng bị trùng trong file import"));
        }

        return errors;
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

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private record RowContext(ImportRow row, Map<String, String> normalized) {}
}

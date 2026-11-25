package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolRoom;
import com.sep.vox.domain.repository.SchoolRoomRepository;

@Service
@RequiredArgsConstructor
public class SchoolRoomImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of("code", "name", "description");

    private final SchoolRoomRepository schoolRoomRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_ROOM;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID schoolId = session.getSchoolId();
        Map<String, String> mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        validateMappingKeys(rows, mapping);
        return commitRowsAndCountOutcomes(rows, mapping, schoolId);
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

    private ImportCommitResult commitRowsAndCountOutcomes(List<ImportRow> rows, Map<String, String> confirmedMapping,
            UUID schoolId) {
        Set<String> codes = new HashSet<String>();
        List<RowContext> rowContexts = prepareRowContexts(rows, confirmedMapping, codes);
        Map<String, SchoolRoom> existingByCode = findExistingByCode(schoolId, codes);
        Set<String> seenCodes = new HashSet<String>();
        Map<RowOutcome, Long> outcomeCounts = new EnumMap<RowOutcome, Long>(RowOutcome.class);
        for (RowContext rowContext : rowContexts) {
            outcomeCounts.merge(commitRow(rowContext, schoolId, existingByCode, seenCodes), 1L, Long::sum);
        }
        return toResult(outcomeCounts);
    }

    private ImportCommitResult toResult(Map<RowOutcome, Long> outcomeCounts) {
        return new ImportCommitResult(
                outcomeCounts.getOrDefault(RowOutcome.CREATED, 0L),
                outcomeCounts.getOrDefault(RowOutcome.UPDATED, 0L),
                0L,
                outcomeCounts.getOrDefault(RowOutcome.INVALID, 0L));
    }

    private List<RowContext> prepareRowContexts(List<ImportRow> rows, Map<String, String> confirmedMapping,
            Set<String> codes) {
        List<RowContext> rowContexts = new ArrayList<RowContext>();
        for (ImportRow row : rows) {
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = mapRawData(rawData, confirmedMapping);
            Map<String, String> normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));
            addIfPresent(codes, normalized.get("code"));
        }
        return rowContexts;
    }

    private RowOutcome commitRow(RowContext rowContext, UUID schoolId, Map<String, SchoolRoom> existingByCode,
            Set<String> seenCodes) {
        ImportRow row = rowContext.row();
        Map<String, String> normalized = rowContext.normalized();
        List<Map<String, String>> errors = validateRow(normalized, seenCodes);

        if (!errors.isEmpty()) {
            row.setErrorsJson(jsonSerializationPort.toJson(errors));
            row.setStatus(ImportRowStatus.INVALID);
            return RowOutcome.INVALID;
        }

        SchoolRoom existing = existingByCode.get(normalized.get("code"));
        try {
            RowOutcome outcome = saveInNewTransaction(existing, normalized, schoolId);
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
            return outcome;
        } catch (DataConflictException | IllegalArgumentException exception) {
            row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "Mã phòng đã tồn tại"))));
            row.setStatus(ImportRowStatus.FAILED);
            return RowOutcome.INVALID;
        }
    }

    private RowOutcome saveInNewTransaction(SchoolRoom existing, Map<String, String> normalized, UUID schoolId) {
        if (existing != null) {
            requiresNewTransactionPort.runInNewTransaction(() ->
                    updateRoom(existing, normalized));
            return RowOutcome.UPDATED;
        }
        requiresNewTransactionPort.runInNewTransaction(() ->
                createRoom(normalized, schoolId));
        return RowOutcome.CREATED;
    }

    private void createRoom(Map<String, String> data, UUID schoolId) {
        // SchoolRoom.registered sets the same active flag as manual creation (AddSchoolRoomUseCase): isActive=false is
        // the soft-delete flag, DeleteSchoolRoomUseCase refuses to delete an inactive room and no API can
        // re-activate it, so an imported room would be stuck forever in the "deleted" state.
        schoolRoomRepository.save(SchoolRoom.registered(schoolId, data.get("code"), data.get("name"),
            data.get("description")));
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

    private enum RowOutcome { CREATED, UPDATED, INVALID }

    private record RowContext(ImportRow row, Map<String, String> normalized) {}
}

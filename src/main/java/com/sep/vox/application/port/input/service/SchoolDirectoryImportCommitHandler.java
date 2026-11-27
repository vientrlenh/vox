package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
@RequiredArgsConstructor
public class SchoolDirectoryImportCommitHandler implements ImportCommitHandler {

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+edu\\.vn$", Pattern.CASE_INSENSITIVE);

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "code", "name", "provinceCode", "provinceName", "districtName", "address", "domain");

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_DIRECTORY;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        Map<String, String> mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        return commitRowsAndCountOutcomes(rows, mapping);
    }

    private ImportCommitResult commitRowsAndCountOutcomes(List<ImportRow> rows, Map<String, String> confirmedMapping) {
        Set<String> codes = new HashSet<String>();
        List<RowContext> rowContexts = prepareRowContexts(rows, confirmedMapping, codes);
        Map<String, SchoolDirectory> existingByCode = findByCode(codes);
        Set<String> seenCodes = new HashSet<String>();
        Map<RowOutcome, Long> outcomeCounts = new EnumMap<RowOutcome, Long>(RowOutcome.class);
        for (RowContext rowContext : rowContexts) {
            outcomeCounts.merge(commitRow(rowContext, existingByCode, seenCodes), 1L, (a, b) -> Long.sum(a, b));
        }
        return toResult(outcomeCounts);
    }

    private ImportCommitResult toResult(Map<RowOutcome, Long> outcomeCounts) {
        return new ImportCommitResult(
                outcomeCounts.getOrDefault(RowOutcome.CREATED, 0L),
                outcomeCounts.getOrDefault(RowOutcome.UPDATED, 0L),
                outcomeCounts.getOrDefault(RowOutcome.SKIPPED, 0L),
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

    private RowOutcome commitRow(RowContext rowContext, Map<String, SchoolDirectory> existingByCode,
            Set<String> seenCodes) {
        ImportRow row = rowContext.row();
        Map<String, String> normalized = rowContext.normalized();
        List<Map<String, String>> errors = validateRow(normalized, seenCodes);

        if (!errors.isEmpty()) {
            row.setErrorsJson(jsonSerializationPort.toJson(errors));
            row.setStatus(ImportRowStatus.INVALID);
            return RowOutcome.INVALID;
        }

        SchoolDirectory existing = existingByCode.get(normalized.get("code"));
        if (existing != null && existing.isCurated()) {
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.SKIPPED);
            return RowOutcome.SKIPPED;
        }

        return importRow(row, existing, normalized);
    }

    private RowOutcome importRow(ImportRow row, SchoolDirectory existing, Map<String, String> normalized) {
        try {
            RowOutcome outcome = saveInNewTransaction(existing, normalized);
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
            return outcome;
        } catch (DataConflictException e) {
            row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "Mã trường đã tồn tại"))));
            row.setStatus(ImportRowStatus.FAILED);
            return RowOutcome.INVALID;
        }
    }

    private RowOutcome saveInNewTransaction(SchoolDirectory existing, Map<String, String> normalized) {
        if (existing != null) {
            requiresNewTransactionPort.runInNewTransaction(() ->
                    updateDirectory(existing, normalized));
            return RowOutcome.UPDATED;
        }
        requiresNewTransactionPort.runInNewTransaction(() ->
                createDirectory(normalized));
        return RowOutcome.CREATED;
    }

    private void createDirectory(Map<String, String> data) {
        schoolDirectoryRepository.save(SchoolDirectory.createByImport(
                data.get("code"),
                data.get("name"),
                data.get("provinceCode"),
                data.get("provinceName"),
                data.get("districtName"),
                data.get("domain"),
                data.get("address")));
    }

    private void updateDirectory(SchoolDirectory existing, Map<String, String> data) {
        existing.applyImportUpdate(
                data.get("name"),
                data.get("provinceCode"),
                data.get("provinceName"),
                data.get("districtName"),
                data.get("domain"),
                data.get("address"));
        schoolDirectoryRepository.save(existing);
    }

    private Map<String, SchoolDirectory> findByCode(Set<String> codes) {
        Map<String, SchoolDirectory> map = new LinkedHashMap<String, SchoolDirectory>();
        schoolDirectoryRepository.findByCodeIn(codes)
                .forEach(sd -> map.putIfAbsent(sd.getCode(), sd));
        return map;
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
        normalized.put("provinceCode", StringNormalization.normalizeCode(mappedData.get("provinceCode")));
        normalized.put("provinceName", StringNormalization.trimAndCollapseSpaces(mappedData.get("provinceName")));
        normalized.put("districtName", StringNormalization.trimAndCollapseSpaces(mappedData.get("districtName")));
        normalized.put("address", StringNormalization.trimAndCollapseSpaces(mappedData.get("address")));
        normalized.put("domain", domainOrNull(mappedData.get("domain")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, Set<String> seenCodes) {
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "code", "Mã trường không được để trống");
        addMissingError(errors, data, "name", "Tên trường không được để trống");
        addMissingError(errors, data, "provinceCode", "Mã tỉnh không được để trống");
        addMissingError(errors, data, "provinceName", "Tên tỉnh không được để trống");
        addMissingError(errors, data, "districtName", "Tên quận/huyện không được để trống");
        addMissingError(errors, data, "address", "Địa chỉ không được để trống");

        String code = data.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Mã trường bị trùng trong file import"));
        }

        String domain = data.get("domain");
        if (isPresent(domain) && !DOMAIN_PATTERN.matcher(domain).matches()) {
            errors.add(error("domain", "Tên miền trường không hợp lệ (phải kết thúc bằng .edu.vn)"));
        }

        return errors;
    }

    private void addMissingError(List<Map<String, String>> errors, Map<String, String> data, String field,
            String message) {
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

    private String domainOrNull(String value) {
        String normalized = StringNormalization.normalizeDomain(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private enum RowOutcome { CREATED, UPDATED, SKIPPED, INVALID }

    private record RowContext(ImportRow row, Map<String, String> normalized) {}
}

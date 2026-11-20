package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

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
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
public class SchoolDirectoryImportCommitHandler implements ImportCommitHandler {

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public SchoolDirectoryImportCommitHandler(SchoolDirectoryRepository schoolDirectoryRepository, JsonSerializationPort jsonSerializationPort, PlatformTransactionManager platformTransactionManager) {
        this.schoolDirectoryRepository = schoolDirectoryRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+edu\\.vn$", Pattern.CASE_INSENSITIVE);

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "code", "name", "provinceCode", "provinceName", "districtName", "address", "domain");

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_DIRECTORY;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        var mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        return processRows(rows, mapping, session.getCreatedBy());
    }

    private ImportCommitResult processRows(List<ImportRow> rows, Map<String, String> confirmedMapping, UUID currentUserId) {
        var createdRows = 0L;
        var updatedRows = 0L;
        var skippedRows = 0L;
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

        var existingByCode = findByCode(codes);

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

            var existing = existingByCode.get(normalized.get("code"));
            
            if (existing != null && existing.isCurated()) {
                row.setErrorsJson(null);
                row.setStatus(ImportRowStatus.SKIPPED);
                skippedRows++;
                continue;
            }

            try {
                if (existing != null) {
                    transactionTemplate.executeWithoutResult(status ->
                            updateDirectory(existing, normalized, currentUserId));
                    updatedRows++;
                } else {
                    transactionTemplate.executeWithoutResult(status ->
                            createDirectory(normalized, currentUserId));
                    createdRows++;
                }
            } catch (DataIntegrityViolationException e) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "Mã trường đã tồn tại"))));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
                continue;
            }

            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
        }

        return new ImportCommitResult(createdRows, updatedRows, skippedRows, invalidRows);
    }

    private void createDirectory(Map<String, String> data, UUID currentUserId) {
        var now = Instant.now();
        schoolDirectoryRepository.save(SchoolDirectory.createByImport(
                data.get("code"),
                data.get("name"),
                data.get("provinceCode"),
                data.get("provinceName"),
                data.get("districtName"),
                data.get("domain"),
                data.get("address"),
                now,
                currentUserId));
    }

    private void updateDirectory(SchoolDirectory existing, Map<String, String> data, UUID currentUserId) {
        var now = Instant.now();
        existing.applyImportUpdate(
                data.get("name"),
                data.get("provinceCode"),
                data.get("provinceName"),
                data.get("districtName"),
                data.get("domain"),
                data.get("address"),
                currentUserId,
                now);
        schoolDirectoryRepository.save(existing);
    }

    private Map<String, SchoolDirectory> findByCode(Set<String> codes) {
        var map = new LinkedHashMap<String, SchoolDirectory>();
        schoolDirectoryRepository.findByCodeIn(codes)
                .forEach(sd -> map.putIfAbsent(sd.getCode(), sd));
        return map;
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
        normalized.put("provinceCode", StringNormalization.normalizeCode(mappedData.get("provinceCode")));
        normalized.put("provinceName", StringNormalization.trimAndCollapseSpaces(mappedData.get("provinceName")));
        normalized.put("districtName", StringNormalization.trimAndCollapseSpaces(mappedData.get("districtName")));
        normalized.put("address", StringNormalization.trimAndCollapseSpaces(mappedData.get("address")));
        normalized.put("domain", domainOrNull(mappedData.get("domain")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, Set<String> seenCodes) {
        var errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "code", "Mã trường không được để trống");
        addMissingError(errors, data, "name", "Tên trường không được để trống");
        addMissingError(errors, data, "provinceCode", "Mã tỉnh không được để trống");
        addMissingError(errors, data, "provinceName", "Tên tỉnh không được để trống");
        addMissingError(errors, data, "districtName", "Tên quận/huyện không được để trống");
        addMissingError(errors, data, "address", "Địa chỉ không được để trống");

        var code = data.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Mã trường bị trùng trong file import"));
        }

        var domain = data.get("domain");
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
        var normalized = StringNormalization.normalizeDomain(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private record RowContext(ImportRow row, Map<String, String> normalized) {}
}

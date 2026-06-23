package com.sep.vox.application.port.input.usecase.schooldirectory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.AcceptSchoolDirectoryImportCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.AcceptSchoolDirectoryImportResponse;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolDirectory;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
public class AcceptSchoolDirectoryImportUseCase
        implements IUseCase<AcceptSchoolDirectoryImportCommand, AcceptSchoolDirectoryImportResponse> {

    private static final Set<String> REQUIRED_FIELDS = Set.of(
            "code", "name", "provinceCode", "provinceName", "districtName", "address");
    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "code", "name", "provinceCode", "provinceName", "districtName", "address", "domain");
    private static final String DOMAIN_PATTERN = "^[a-zA-Z0-9.-]+\\.edu\\.vn$";

    private final ImportSessionRepository importSessionRepository;
    private final ImportRowRepository importRowRepository;
    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public AcceptSchoolDirectoryImportUseCase(
            ImportSessionRepository importSessionRepository,
            ImportRowRepository importRowRepository,
            SchoolDirectoryRepository schoolDirectoryRepository,
            UserContextPort userContextPort,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.importSessionRepository = importSessionRepository;
        this.importRowRepository = importRowRepository;
        this.schoolDirectoryRepository = schoolDirectoryRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public AcceptSchoolDirectoryImportResponse execute(AcceptSchoolDirectoryImportCommand input) {
        validateCommand(input);

        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var session = findSession(input.importSessionId());
        validateSession(session, now);
        validateRequiredMapping(input.confirmedMapping());

        session.setStatus(ImportSessionStatus.IMPORTING);
        session.setConfirmedMappingJson(jsonSerializationPort.toJson(input.confirmedMapping()));
        session.setUpdatedAt(now);
        session.setUpdatedBy(currentUserId);
        importSessionRepository.save(session);

        var rows = importRowRepository.findBySessionIdOrderByRowNumber(session.getId());
        var result = processRows(rows, input.confirmedMapping(), currentUserId);

        importRowRepository.saveAll(rows);
        session.setImportedRows(result.createdRows());
        session.setValidRows(result.createdRows() + result.updatedRows() + result.skippedRows());
        session.setInvalidRows(result.invalidRows());
        session.setSkippedRows(result.skippedRows());
        session.setTotalRows(rows.size());
        session.setStatus(ImportSessionStatus.COMPLETED);
        session.setUpdatedAt(now);
        session.setUpdatedBy(currentUserId);
        var savedSession = importSessionRepository.save(session);

        return new AcceptSchoolDirectoryImportResponse(
                savedSession.getId(),
                savedSession.getTotalRows(),
                savedSession.getImportedRows(),
                result.updatedRows(),
                savedSession.getInvalidRows(),
                savedSession.getSkippedRows(),
                savedSession.getStatus().name());
    }

    private ProcessResult processRows(List<ImportRow> rows, Map<String, String> confirmedMapping, UUID currentUserId) {
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

        return new ProcessResult(createdRows, updatedRows, skippedRows, invalidRows);
    }

    private void createDirectory(Map<String, String> data, UUID currentUserId) {
        var now = OffsetDateTime.now();
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
        var now = OffsetDateTime.now();
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

    private void validateCommand(AcceptSchoolDirectoryImportCommand input) {
        if (input == null || input.importSessionId() == null) {
            throw new IllegalArgumentException("Phiên import không được để trống");
        }
        if (input.confirmedMapping() == null || input.confirmedMapping().isEmpty()) {
            throw new IllegalArgumentException("Mapping import không được để trống");
        }
    }

    private ImportSession findSession(UUID importSessionId) {
        return importSessionRepository.findById(importSessionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên import"));
    }

    private void validateSession(ImportSession session, OffsetDateTime now) {
        if (session.getType() != ImportType.SCHOOL_DIRECTORY) {
            throw new IllegalArgumentException("Phiên import không phải là import danh mục trường");
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
            throw new IllegalArgumentException(
                    "Mapping import thiếu trường bắt buộc: " + String.join(", ", missingFields));
        }
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
        if (isPresent(domain) && !domain.matches(DOMAIN_PATTERN)) {
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

    private record ProcessResult(long createdRows, long updatedRows, long skippedRows, long invalidRows) {}
}

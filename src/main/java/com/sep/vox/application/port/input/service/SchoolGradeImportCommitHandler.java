package com.sep.vox.application.port.input.service;

import java.time.LocalDate;
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

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.gradelevel.GradeLevel;
import com.sep.vox.domain.repository.GradeLevelRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;

@Service
@RequiredArgsConstructor
public class SchoolGradeImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "schoolGradeLevelCode", "code", "name", "description", "startDate", "endDate");

    private final SchoolGradeRepository schoolGradeRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_GRADE;
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
            throw new IllegalArgumentException("Mapping contains a column that does not exist in the file: " + String.join(", ", invalidKeys));
        }
        List<String> invalidValues = confirmedMapping.values().stream()
            .map(value -> value == null ? null : value.strip())
            .filter(value -> value != null && !value.isEmpty() && !SUPPORTED_FIELDS.contains(value))
            .toList();
        if (!invalidValues.isEmpty()) {
            throw new IllegalArgumentException("Mapping contains an invalid system field: " + String.join(", ", invalidValues));
        }
    }

    private ImportCommitResult commitRowsAndCountOutcomes(List<ImportRow> rows, Map<String, String> confirmedMapping,
            UUID schoolId) {
        Set<String> levelCodes = new HashSet<String>();
        List<RowContext> rowContexts = prepareRowContexts(rows, confirmedMapping, levelCodes);
        Map<String, GradeLevel> levelsByCode = findGradeLevelsByCode(levelCodes);
        Set<String> seenKeys = new HashSet<String>();
        Map<RowOutcome, Long> outcomeCounts = new EnumMap<RowOutcome, Long>(RowOutcome.class);
        for (RowContext rowContext : rowContexts) {
            outcomeCounts.merge(commitRow(rowContext, schoolId, levelsByCode, seenKeys), 1L, (a, b) -> Long.sum(a, b));
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
            Set<String> levelCodes) {
        List<RowContext> rowContexts = new ArrayList<RowContext>();
        for (ImportRow row : rows) {
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = mapRawData(rawData, confirmedMapping);
            Map<String, String> normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));
            addIfPresent(levelCodes, normalized.get("schoolGradeLevelCode"));
        }
        return rowContexts;
    }

    private RowOutcome commitRow(RowContext rowContext, UUID schoolId, Map<String, GradeLevel> levelsByCode,
            Set<String> seenKeys) {
        ImportRow row = rowContext.row();
        Map<String, String> normalized = rowContext.normalized();
        List<Map<String, String>> errors = validateRow(normalized, seenKeys, levelsByCode);

        if (!errors.isEmpty()) {
            row.setErrorsJson(jsonSerializationPort.toJson(errors));
            row.setStatus(ImportRowStatus.INVALID);
            return RowOutcome.INVALID;
        }

        GradeLevel gradeLevel = levelsByCode.get(normalized.get("schoolGradeLevelCode"));
        SchoolGrade existing = schoolGradeRepository
                .findBySchoolIdAndGradeLevelIdAndCode(schoolId, gradeLevel.getId(), normalized.get("code"))
                .orElse(null);
        return importRow(row, existing, normalized, schoolId, gradeLevel.getId());
    }

    private RowOutcome importRow(ImportRow row, SchoolGrade existing, Map<String, String> normalized, UUID schoolId,
            UUID gradeLevelId) {
        try {
            RowOutcome outcome = saveInNewTransaction(existing, normalized, schoolId, gradeLevelId);
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
            return outcome;
        } catch (DataConflictException | IllegalArgumentException exception) {
            row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "School-year grade code already exists or is invalid"))));
            row.setStatus(ImportRowStatus.FAILED);
            return RowOutcome.INVALID;
        }
    }

    private RowOutcome saveInNewTransaction(SchoolGrade existing, Map<String, String> normalized, UUID schoolId,
            UUID gradeLevelId) {
        LocalDate startDate = DateMapper.toLocalDate(normalized.get("startDate"));
        LocalDate endDate = DateMapper.toLocalDate(normalized.get("endDate"));
        if (existing != null) {
            requiresNewTransactionPort.runInNewTransaction(() ->
                    updateGrade(existing, normalized, startDate, endDate));
            return RowOutcome.UPDATED;
        }
        requiresNewTransactionPort.runInNewTransaction(() ->
                createGrade(normalized, schoolId, gradeLevelId, startDate, endDate));
        return RowOutcome.CREATED;
    }

    private void createGrade(Map<String, String> data, UUID schoolId, UUID gradeLevelId, LocalDate startDate, LocalDate endDate) {
        // SchoolGrade.opened sets the same ACTIVE status as manual creation (CreateSchoolGradeUseCase): an INACTIVE
        // school year is rejected by CreateSchoolClassUseCase / SchoolClassImportCommitHandler, and no API can
        // re-activate it, so the imported record would be stuck forever.
        schoolGradeRepository.save(SchoolGrade.opened(schoolId, gradeLevelId, data.get("code"), data.get("name"),
            data.get("description"), startDate, endDate));
    }

    private void updateGrade(SchoolGrade existing, Map<String, String> data, LocalDate startDate, LocalDate endDate) {
        existing.setName(data.get("name"));
        existing.setDescription(data.get("description"));
        existing.setStartDate(startDate);
        existing.setEndDate(endDate);
        schoolGradeRepository.save(existing);
    }

    // The grade-level catalog is shared, so looking up by code is enough; no filtering by school any more.
    private Map<String, GradeLevel> findGradeLevelsByCode(Set<String> codes) {
        Map<String, GradeLevel> levelsByCode = new LinkedHashMap<String, GradeLevel>();
        gradeLevelRepository.findByCodeIn(codes)
            .forEach(level -> levelsByCode.putIfAbsent(level.getCode(), level));
        return levelsByCode;
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
        normalized.put("schoolGradeLevelCode", StringNormalization.normalizeCode(mappedData.get("schoolGradeLevelCode")));
        normalized.put("code", StringNormalization.normalizeCode(mappedData.get("code")));
        normalized.put("name", StringNormalization.trimAndCollapseSpaces(mappedData.get("name")));
        normalized.put("description", StringNormalization.trimAndCollapseSpaces(mappedData.get("description")));
        normalized.put("startDate", trimToNull(mappedData.get("startDate")));
        normalized.put("endDate", trimToNull(mappedData.get("endDate")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> data, Set<String> seenKeys,
            Map<String, GradeLevel> levelsByCode) {
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "schoolGradeLevelCode", "Grade code must not be blank");
        addMissingError(errors, data, "code", "School-year grade code must not be blank");
        addMissingError(errors, data, "name", "School-year grade name must not be blank");
        addMissingError(errors, data, "startDate", "Start date must not be blank");
        addMissingError(errors, data, "endDate", "End date must not be blank");

        String levelCode = data.get("schoolGradeLevelCode");
        if (isPresent(levelCode) && !levelsByCode.containsKey(levelCode)) {
            errors.add(error("schoolGradeLevelCode", "Grade level not found"));
        }

        String code = data.get("code");
        if (isPresent(levelCode) && isPresent(code) && !seenKeys.add(levelCode + "::" + code)) {
            errors.add(error("code", "School-year grade code is duplicated in the import file"));
        }

        addDateErrors(errors, data);
        return errors;
    }

    private void addDateErrors(List<Map<String, String>> errors, Map<String, String> data) {
        LocalDate startDate = parseDateOrNull(data.get("startDate"));
        if (isPresent(data.get("startDate")) && startDate == null) {
            errors.add(error("startDate", "Start date is invalid"));
        }
        LocalDate endDate = parseDateOrNull(data.get("endDate"));
        if (isPresent(data.get("endDate")) && endDate == null) {
            errors.add(error("endDate", "End date is invalid"));
        }
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            errors.add(error("endDate", "End date must be after start date"));
        }
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
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private enum RowOutcome { CREATED, UPDATED, INVALID }

    private record RowContext(ImportRow row, Map<String, String> normalized) {}
}

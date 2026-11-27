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
import com.sep.vox.domain.model.school.SchoolClass;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.supportedlanguage.SupportedLanguage;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

@Service
@RequiredArgsConstructor
public class SchoolClassImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of("code", "name", "languageCode", "schoolGradeCode", "description");

    private final SchoolClassRepository schoolClassRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final SchoolGradeRepository schoolGradeRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    @Override
    public ImportType supportedType() {
        return ImportType.SCHOOL_CLASS;
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
        Set<String> classCodes = new HashSet<String>();
        Set<String> languageCodes = new HashSet<String>();
        Set<String> schoolGradeCodes = new HashSet<String>();
        List<RowContext> rowContexts = prepareRowContexts(rows, confirmedMapping, classCodes, languageCodes, schoolGradeCodes);

        RowLookups lookups = new RowLookups(schoolId,
                findExistingClassesByCode(schoolId, classCodes),
                findLanguagesByCode(languageCodes),
                findGradesByCode(schoolId, schoolGradeCodes),
                new HashSet<String>());
        Map<RowOutcome, Long> outcomeCounts = new EnumMap<RowOutcome, Long>(RowOutcome.class);
        for (RowContext rowContext : rowContexts) {
            outcomeCounts.merge(commitRow(rowContext, lookups), 1L, (a, b) -> Long.sum(a, b));
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
            Set<String> classCodes, Set<String> languageCodes, Set<String> schoolGradeCodes) {
        List<RowContext> rowContexts = new ArrayList<RowContext>();
        for (ImportRow row : rows) {
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = mapRawData(rawData, confirmedMapping);
            Map<String, String> normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));
            rowContexts.add(new RowContext(row, normalized));

            addIfPresent(classCodes, normalized.get("code"));
            addIfPresent(languageCodes, normalized.get("languageCode"));
            addIfPresent(schoolGradeCodes, normalized.get("schoolGradeCode"));
        }
        return rowContexts;
    }

    private RowOutcome commitRow(RowContext rowContext, RowLookups lookups) {
        ImportRow row = rowContext.row();
        Map<String, String> normalized = rowContext.normalized();
        List<Map<String, String>> errors = validateRow(normalized, lookups.seenCodes(),
                lookups.languagesByCode(), lookups.gradesByCode());

        if (!errors.isEmpty()) {
            row.setErrorsJson(jsonSerializationPort.toJson(errors));
            row.setStatus(ImportRowStatus.INVALID);
            return RowOutcome.INVALID;
        }

        try {
            RowOutcome outcome = saveInNewTransaction(normalized, lookups);
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
            return outcome;
        } catch (DataConflictException | IllegalArgumentException exception) {
            row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "Class code already exists or is invalid"))));
            row.setStatus(ImportRowStatus.FAILED);
            return RowOutcome.INVALID;
        }
    }

    private RowOutcome saveInNewTransaction(Map<String, String> normalized, RowLookups lookups) {
        SupportedLanguage language = lookups.languagesByCode().get(normalized.get("languageCode"));
        SchoolGrade schoolGrade = lookups.gradesByCode().get(normalized.get("schoolGradeCode"));
        SchoolClass existingClass = lookups.existingClassesByCode().get(normalized.get("code"));
        if (existingClass != null) {
            requiresNewTransactionPort.runInNewTransaction(() ->
                    updateClass(existingClass, normalized, language.getId(), schoolGrade.getId()));
            return RowOutcome.UPDATED;
        }
        requiresNewTransactionPort.runInNewTransaction(() ->
                createClass(normalized, lookups.schoolId(), language.getId(), schoolGrade.getId()));
        return RowOutcome.CREATED;
    }

    private void createClass(Map<String, String> data, UUID schoolId, UUID languageId, UUID schoolGradeId) {
        schoolClassRepository.save(SchoolClass.create(
                schoolId,
                languageId,
                schoolGradeId,
                data.get("code"),
                data.get("name"),
                data.get("description")));
    }

    private void updateClass(SchoolClass existing, Map<String, String> data, UUID languageId, UUID schoolGradeId) {
        existing.setName(data.get("name"));
        existing.setDescription(data.get("description"));
        existing.setLanguageId(languageId);
        existing.setSchoolGradeId(schoolGradeId);
        schoolClassRepository.save(existing);
    }

    private Map<String, SchoolClass> findExistingClassesByCode(UUID schoolId, Set<String> codes) {
        Map<String, SchoolClass> existingClassesByCode = new LinkedHashMap<String, SchoolClass>();
        schoolClassRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .forEach(schoolClass -> existingClassesByCode.putIfAbsent(schoolClass.getCode().value(), schoolClass));
        return existingClassesByCode;
    }

    private Map<String, SupportedLanguage> findLanguagesByCode(Set<String> codes) {
        Map<String, SupportedLanguage> languagesByCode = new LinkedHashMap<String, SupportedLanguage>();
        supportedLanguageRepository.findByCodeIn(codes)
            .forEach(language -> languagesByCode.putIfAbsent(language.getCode().value(), language));
        return languagesByCode;
    }

    private Map<String, SchoolGrade> findGradesByCode(UUID schoolId, Set<String> codes) {
        Map<String, SchoolGrade> gradesByCode = new LinkedHashMap<String, SchoolGrade>();
        schoolGradeRepository.findBySchoolIdAndCodeIn(schoolId, codes)
            .forEach(grade -> gradesByCode.putIfAbsent(grade.getCode(), grade));
        return gradesByCode;
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
        normalized.put("languageCode", StringNormalization.normalizeCode(mappedData.get("languageCode")));
        normalized.put("schoolGradeCode", StringNormalization.normalizeCode(mappedData.get("schoolGradeCode")));
        normalized.put("description", StringNormalization.trimAndCollapseSpaces(mappedData.get("description")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(Map<String, String> mappedData, Set<String> seenCodes,
            Map<String, SupportedLanguage> languagesByCode,
            Map<String, SchoolGrade> gradesByCode) {
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();
        addMissingError(errors, mappedData, "code", "Class code must not be blank");
        addMissingError(errors, mappedData, "name", "Class name must not be blank");
        addMissingError(errors, mappedData, "languageCode", "Language code must not be blank");
        addMissingError(errors, mappedData, "schoolGradeCode", "Grade code must not be blank");

        String code = mappedData.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Class code is duplicated in the import file"));
        }

        addLanguageErrors(errors, mappedData.get("languageCode"), languagesByCode);
        addSchoolGradeErrors(errors, mappedData.get("schoolGradeCode"), gradesByCode);
        return errors;
    }

    private void addLanguageErrors(List<Map<String, String>> errors, String languageCode,
            Map<String, SupportedLanguage> languagesByCode) {
        if (!isPresent(languageCode)) {
            return;
        }
        SupportedLanguage language = languagesByCode.get(languageCode);
        if (language == null) {
            errors.add(error("languageCode", "Language not found"));
        } else if (!language.isActive()) {
            errors.add(error("languageCode", "Language is not active"));
        }
    }

    private void addSchoolGradeErrors(List<Map<String, String>> errors, String schoolGradeCode,
            Map<String, SchoolGrade> gradesByCode) {
        if (!isPresent(schoolGradeCode)) {
            return;
        }
        SchoolGrade schoolGrade = gradesByCode.get(schoolGradeCode);
        if (schoolGrade == null) {
            errors.add(error("schoolGradeCode", "Grade not found"));
        } else if (schoolGrade.getStatus() != SchoolGradeStatus.ACTIVE) {
            errors.add(error("schoolGradeCode", "Grade is not active"));
        }
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

    private enum RowOutcome { CREATED, UPDATED, INVALID }

    private record RowLookups(
            UUID schoolId,
            Map<String, SchoolClass> existingClassesByCode,
            Map<String, SupportedLanguage> languagesByCode,
            Map<String, SchoolGrade> gradesByCode,
            Set<String> seenCodes) {
    }

    private record RowContext(ImportRow row, Map<String, String> normalized) {
    }
}

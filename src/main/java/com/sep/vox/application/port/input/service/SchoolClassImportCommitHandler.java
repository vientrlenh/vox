package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
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
        Set<String> classCodes = new HashSet<String>();
        Set<String> languageCodes = new HashSet<String>();
        Set<String> schoolGradeCodes = new HashSet<String>();

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

        Map<String, SchoolClass> existingClassesByCode = findExistingClassesByCode(schoolId, classCodes);
        Map<String, SupportedLanguage> languagesByCode = findLanguagesByCode(languageCodes);
        Map<String, SchoolGrade> gradesByCode = findGradesByCode(schoolId, schoolGradeCodes);

        for (RowContext rowContext : rowContexts) {
            ImportRow row = rowContext.row();
            Map<String, String> normalized = rowContext.normalized();
            List<Map<String, String>> errors = validateRow(normalized, seenCodes, languagesByCode, gradesByCode);

            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            SupportedLanguage language = languagesByCode.get(normalized.get("languageCode"));
            SchoolGrade schoolGrade = gradesByCode.get(normalized.get("schoolGradeCode"));
            SchoolClass existingClass = existingClassesByCode.get(normalized.get("code"));

            try {
                if (existingClass != null) {
                    requiresNewTransactionPort.runInNewTransaction(() ->
                            updateClass(existingClass, normalized, language.getId(), schoolGrade.getId()));
                    updatedRows++;
                } else {
                    requiresNewTransactionPort.runInNewTransaction(() ->
                            createClass(normalized, schoolId, language.getId(), schoolGrade.getId()));
                    createdRows++;
                }
            } catch (DataConflictException | IllegalArgumentException exception) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(error("code", "Mã lớp đã tồn tại hoặc không hợp lệ"))));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
                continue;
            }

            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
        }

        return new ImportCommitResult(createdRows, updatedRows, 0L, invalidRows);
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
        addMissingError(errors, mappedData, "code", "Mã lớp không được để trống");
        addMissingError(errors, mappedData, "name", "Tên lớp không được để trống");
        addMissingError(errors, mappedData, "languageCode", "Mã ngôn ngữ không được để trống");
        addMissingError(errors, mappedData, "schoolGradeCode", "Mã khối không được để trống");

        String code = mappedData.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Mã lớp bị trùng trong file import"));
        }

        String languageCode = mappedData.get("languageCode");
        if (isPresent(languageCode)) {
            SupportedLanguage language = languagesByCode.get(languageCode);
            if (language == null) {
                errors.add(error("languageCode", "Không tìm thấy ngôn ngữ"));
            } else if (!language.isActive()) {
                errors.add(error("languageCode", "Ngôn ngữ không hoạt động"));
            }
        }

        String schoolGradeCode = mappedData.get("schoolGradeCode");
        if (isPresent(schoolGradeCode)) {
            SchoolGrade schoolGrade = gradesByCode.get(schoolGradeCode);
            if (schoolGrade == null) {
                errors.add(error("schoolGradeCode", "Không tìm thấy khối học"));
            } else if (schoolGrade.getStatus() != SchoolGradeStatus.ACTIVE) {
                errors.add(error("schoolGradeCode", "Khối học không hoạt động"));
            }
        }

        return errors;
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

    private record RowContext(ImportRow row, Map<String, String> normalized) {
    }
}

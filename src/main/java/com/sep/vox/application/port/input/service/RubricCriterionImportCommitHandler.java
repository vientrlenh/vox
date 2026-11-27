package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RubricCriterionImportCommitHandler implements ImportCommitHandler {

    /** The Excel file stores percentages while the database stores fractions -- see where it is used below. */
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");


    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final FrameworkVersionRepository frameworkVersionRepository;

    @Override
    public ImportType supportedType() {
        return ImportType.RUBRIC_CRITERION;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        RubricVersion version = requireDraftVersion(session.getImportedEntityId());
        Rubric rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found during background processing."));
        CommitRun run = buildCommitRun(version, rubric, resolveMapping(session));

        List<RubricCriterion> criterionsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;
            if (commitRow(row, run, criterionsToSave)) {
                importedCount++;
            } else {
                invalidCount++;
            }
        }

        saveCriterions(criterionsToSave);
        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    private RubricVersion requireDraftVersion(UUID versionId) {
        RubricVersion version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Rubric version not found during background processing."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Criteria can only be imported when the Rubric version is in DRAFT status.");
        }
        return version;
    }

    private Map<String, String> resolveMapping(ImportSession session) {
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            return jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            return jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }
        return new HashMap<>();
    }

    private record CommitRun(
            RubricVersion version,
            Map<String, String> mapping,
            Map<String, UUID> fwCodeToIdMap,
            Map<String, RubricCriterion> existingCodeMap,
            Map<UUID, RubricCriterion> existingByFwCriterionId,
            Set<String> codesInFile,
            Set<Integer> ordersInFile,
            Set<UUID> fwCriterionIdsInFile) {}

    private CommitRun buildCommitRun(RubricVersion version, Rubric rubric, Map<String, String> mapping) {
        FrameworkVersion activeFwVersion = frameworkVersionRepository.findByFrameworkIdAndStatus(
                        rubric.getFrameworkId(), FrameworkVersionStatus.PUBLISHED)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active standard Framework version found for this Framework."));
        // Framework criteria are fetched by the framework VERSION id, not by the framework id.
        List<FrameworkCriterion> fwCriterions = frameworkCriterionRepository.findByFrameworkVersionId(activeFwVersion.getId());
        List<RubricCriterion> existingCriterions = rubricCriterionRepository.findByRubricVersionId(version.getId());

        return new CommitRun(version, mapping, indexFrameworkCriterionIdsByCode(fwCriterions),
                indexByCode(existingCriterions), indexByFrameworkCriterionId(existingCriterions),
                new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    // The merge function (existing, newValue) -> existing guards against the database accidentally
    // holding two equal codes: the first one wins instead of crashing the whole background flow.
    private Map<String, UUID> indexFrameworkCriterionIdsByCode(List<FrameworkCriterion> fwCriterions) {
        return fwCriterions.stream()
                .collect(Collectors.toMap(
                        fc -> normalizeCode(fc.getCode()),
                        fc -> fc.getId(),
                        (existingValue, newValue) -> existingValue
                ));
    }

    private Map<String, RubricCriterion> indexByCode(List<RubricCriterion> existingCriterions) {
        return existingCriterions.stream()
                .collect(Collectors.toMap(
                        c -> normalizeCode(c.getCode()),
                        c -> c,
                        (existingValue, newValue) -> existingValue
                ));
    }

    // Reverse lookup: which Rubric Criterion (code) of this version already holds a Framework Criterion
    private Map<UUID, RubricCriterion> indexByFrameworkCriterionId(List<RubricCriterion> existingCriterions) {
        return existingCriterions.stream()
                .collect(Collectors.toMap(
                        rc -> rc.getFrameworkCriterionId(),
                        c -> c,
                        (existingValue, newValue) -> existingValue
                ));
    }

    private record RowFields(String code, String name, String description, String frameworkCriterionCode,
                             String examples, String weight, String minScore, String maxScore,
                             String required, String order) {}

    private record ParsedNumbers(BigDecimal weight, BigDecimal minScore, BigDecimal maxScore, int order,
                                 boolean isRequired) {
        static final ParsedNumbers DEFAULT = new ParsedNumbers(null, null, null, 0, true);
    }

    /** Returns true when the row was imported, false when it was marked invalid. */
    private boolean commitRow(ImportRow row, CommitRun run, List<RubricCriterion> criterionsToSave) {
        List<Map<String, String>> errors = new ArrayList<>();
        Map<String, String> mappedData = mapRow(row, run.mapping());
        try {
            ValidatedRow validated = validateRow(run, mappedData, errors);
            if (!errors.isEmpty()) {
                markInvalid(row, errors);
                return false;
            }
            criterionsToSave.add(applyToCriterion(run.version().getId(), validated));
            markImported(row, mappedData);
            return true;
        } catch (Exception ex) {
            errors.add(error("general", "Background processing error: " + ex.getMessage()));
            markInvalid(row, errors);
            return false;
        }
    }

    private record ValidatedRow(RowFields fields, String safeCode, UUID fwCriterionId,
                                RubricCriterion targetCriterion, RubricCriterionExamples examples,
                                ParsedNumbers numbers) {}

    private ValidatedRow validateRow(CommitRun run, Map<String, String> mappedData, List<Map<String, String>> errors) {
        RowFields fields = readFields(mappedData);
        addMissingFieldErrors(fields, errors);
        String safeCode = fields.code() != null ? fields.code().trim() : "";
        addDuplicateCodeError(run, safeCode, errors);
        UUID fwCriterionId = errors.isEmpty() ? resolveFrameworkCriterionId(run, fields, errors) : null;
        RubricCriterion targetCriterion = errors.isEmpty() ? run.existingCodeMap().get(normalizeCode(safeCode)) : null;
        addFrameworkCriterionBindingErrors(run, fields, safeCode, fwCriterionId, targetCriterion, errors);
        RubricCriterionExamples examplesObj = errors.isEmpty() ? parseExamples(fields.examples(), errors) : null;
        ParsedNumbers numbers = parseNumbers(fields, run.ordersInFile(), errors);
        addScoreRangeError(run.version(), numbers, safeCode, errors);
        return new ValidatedRow(fields, safeCode, fwCriterionId, targetCriterion, examplesObj, numbers);
    }

    private void addDuplicateCodeError(CommitRun run, String safeCode, List<Map<String, String>> errors) {
        if (errors.isEmpty() && !run.codesInFile().add(normalizeCode(safeCode))) {
            errors.add(error("code", "Duplicate criterion code '" + safeCode + "' within the Excel file."));
        }
    }

    private void markImported(ImportRow row, Map<String, String> mappedData) {
        row.setStatus(ImportRowStatus.IMPORTED);
        row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
    }

    private Map<String, String> mapRow(ImportRow row, Map<String, String> mapping) {
        Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
        Map<String, String> mappedData = new HashMap<>();
        for (Map.Entry<String, String> entry : rawData.entrySet()) {
            String sysField = mapping.get(entry.getKey());
            if (sysField != null) mappedData.put(sysField, entry.getValue());
        }
        return mappedData;
    }

    private void markInvalid(ImportRow row, List<Map<String, String>> errors) {
        row.setStatus(ImportRowStatus.INVALID);
        row.setErrorsJson(jsonSerializationPort.toJson(errors));
    }

    private static RowFields readFields(Map<String, String> mappedData) {
        return new RowFields(
                mappedData.get("code"), mappedData.get("name"), mappedData.get("description"),
                mappedData.get("frameworkCriterionCode"), mappedData.get("examples"), mappedData.get("weight"),
                mappedData.get("minScore"), mappedData.get("maxScore"), mappedData.get("isRequired"),
                mappedData.get("order"));
    }

    private static void addMissingFieldErrors(RowFields fields, List<Map<String, String>> errors) {
        if (isBlank(fields.code())) errors.add(error("code", "Criterion code is missing."));
        if (isBlank(fields.name())) errors.add(error("name", "Criterion name is missing."));
        if (isBlank(fields.frameworkCriterionCode())) errors.add(error("frameworkCriterionCode", "Referenced standard framework code is missing."));
        if (isBlank(fields.weight())) errors.add(error("weight", "Weight is missing."));
        if (isBlank(fields.minScore())) errors.add(error("minScore", "Minimum score is missing."));
        if (isBlank(fields.maxScore())) errors.add(error("maxScore", "Maximum score is missing."));
        if (isBlank(fields.order())) errors.add(error("order", "Order is missing."));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static UUID resolveFrameworkCriterionId(CommitRun run, RowFields fields, List<Map<String, String>> errors) {
        UUID fwCriterionId = run.fwCodeToIdMap().get(normalizeCode(fields.frameworkCriterionCode()));
        if (fwCriterionId == null) {
            errors.add(error("frameworkCriterionCode", "Standard framework code '" + fields.frameworkCriterionCode() + "' does not exist in the parent Framework."));
        }
        return fwCriterionId;
    }

    // frameworkCriterionId is immutable after creation (like code):
    // - About to CREATE (targetCriterion == null): this framework must not duplicate another criterion in the file/DB.
    // - The criterion ALREADY EXISTS: reject the whole row if the file tries to switch to another framework, to avoid
    //   a half-applied state (the other fields updated by the "wrong" row while frameworkCriterionId stays unchanged).
    private void addFrameworkCriterionBindingErrors(CommitRun run, RowFields fields, String safeCode, UUID fwCriterionId,
            RubricCriterion targetCriterion, List<Map<String, String>> errors) {
        if (!errors.isEmpty() || fwCriterionId == null) {
            return;
        }
        String fwCodeStr = fields.frameworkCriterionCode();
        if (targetCriterion == null) {
            if (!run.fwCriterionIdsInFile().add(fwCriterionId)) {
                errors.add(error("frameworkCriterionCode", "Duplicate referenced standard framework code '" + fwCodeStr + "' within the Excel file."));
            } else {
                RubricCriterion holder = run.existingByFwCriterionId().get(fwCriterionId);
                if (holder != null) {
                    errors.add(error("frameworkCriterionCode", "Standard framework code '" + fwCodeStr
                            + "' is already assigned to another criterion (code: " + holder.getCode() + ") in this version."));
                }
            }
        } else if (!fwCriterionId.equals(targetCriterion.getFrameworkCriterionId())) {
            errors.add(error("frameworkCriterionCode", "Cannot change the referenced standard framework of criterion '"
                    + safeCode + "' that already exists (the referenced standard framework code may not be edited after creation)."));
        }
    }

    private static RubricCriterionExamples parseExamples(String examplesStr, List<Map<String, String>> errors) {
        if (examplesStr == null || examplesStr.isBlank()) {
            return null;
        }
        try {
            List<String> rawExamples = Arrays.stream(examplesStr.split(";"))
                    .map(s -> s.trim())
                    .filter(s -> !s.isBlank())
                    .toList();
            if (rawExamples.isEmpty()) {
                return null;
            }
            return new RubricCriterionExamples(rawExamples.stream()
                    .map(exampleText -> new RubricCriterionExample(exampleText, exampleText, BigDecimal.ZERO))
                    .toList());
        } catch (Exception e) {
            errors.add(error("examples", "Example data is invalid (separate examples with a ; character). Error: " + e.getMessage()));
            return null;
        }
    }

    private static ParsedNumbers parseNumbers(RowFields fields, Set<Integer> ordersInFile, List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            return ParsedNumbers.DEFAULT;
        }
        try {
            BigDecimal weight = parseWeightFraction(trimOrEmpty(fields.weight()), errors);
            BigDecimal minScore = new BigDecimal(trimOrEmpty(fields.minScore()));
            BigDecimal maxScore = new BigDecimal(trimOrEmpty(fields.maxScore()));
            if (minScore.compareTo(maxScore) > 0) errors.add(error("minScore", "Minimum score must not exceed maximum score."));
            int order = parseOrder(trimOrEmpty(fields.order()), ordersInFile, errors);
            return new ParsedNumbers(weight, minScore, maxScore, order, parseIsRequired(fields.required()));
        } catch (NumberFormatException e) {
            errors.add(error("general", "Weight/Score/Order data is not a valid number."));
            return ParsedNumbers.DEFAULT;
        }
    }

    // The Excel file holds a PERCENTAGE (20), the DB column holds a FRACTION (0.2).
    //
    // Converted right here to match the manual entry screen: users work in percentages on both paths,
    // while the publishing gate (ChangeSchoolRubricVersionStatusUseCase) demands a total of exactly 1.
    // Without the division a file of 5 criteria x 20 reaches the DB as a total of 100, create/update
    // still pass because they only check >= 0, and it blows up only at publish time.
    //
    // Divide by 100 with scale 6: 20 -> 0.200000, enough room for fractional weights such as 12.5%
    // without losing digits.
    private static BigDecimal parseWeightFraction(String weightText, List<Map<String, String>> errors) {
        BigDecimal weightPercent = new BigDecimal(weightText);
        if (weightPercent.compareTo(BigDecimal.ZERO) < 0) errors.add(error("weight", "Weight must not be negative."));
        return weightPercent.divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
    }

    private static int parseOrder(String orderText, Set<Integer> ordersInFile, List<Map<String, String>> errors) {
        int order = Integer.parseInt(orderText.trim());
        if (order <= 0) errors.add(error("order", "Order must be greater than 0."));
        if (!ordersInFile.add(order)) {
            errors.add(error("order", "Duplicate order " + order + " within the Excel file."));
        }
        return order;
    }

    private static boolean parseIsRequired(String requiredText) {
        if (requiredText == null || requiredText.isBlank()) {
            return true;
        }
        String r = requiredText.trim().toLowerCase();
        return r.equals("true") || r.equals("yes") || r.equals("1") || r.equals("có") || r.equals("co");
    }

    private static void addScoreRangeError(RubricVersion version, ParsedNumbers numbers, String safeCode,
            List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            return;
        }
        try {
            ScoreRangeValidator.assertWithinScale(version.getScoringScaleMin(), version.getScoringScaleMax(),
                    numbers.minScore(), numbers.maxScore(), safeCode);
        } catch (IllegalArgumentException e) {
            errors.add(error("minScore", e.getMessage()));
        }
    }

    private RubricCriterion applyToCriterion(UUID versionId, ValidatedRow validated) {
        RowFields fields = validated.fields();
        ParsedNumbers numbers = validated.numbers();
        RubricCriterion targetCriterion = validated.targetCriterion();
        String safeNameStr = fields.name() == null ? "" : fields.name().trim();
        if (targetCriterion == null) {
            return RubricCriterion.create(
                    versionId, validated.fwCriterionId(), validated.safeCode(), safeNameStr, fields.description(), validated.examples(),
                    numbers.weight(), numbers.minScore(), numbers.maxScore(), numbers.order(), numbers.isRequired());
        }
        // frameworkCriterionId is deliberately not set: for an existing criterion the referenced framework
        // criterion is immutable, exactly how "code" is protected from edits after creation.
        targetCriterion.setName(safeNameStr);
        targetCriterion.setDescription(fields.description());
        targetCriterion.setExamples(validated.examples());
        targetCriterion.setWeight(numbers.weight());
        targetCriterion.setMinScore(numbers.minScore());
        targetCriterion.setMaxScore(numbers.maxScore());
        targetCriterion.setOrder(numbers.order());
        targetCriterion.setRequired(numbers.isRequired());
        return targetCriterion;
    }

    private void saveCriterions(List<RubricCriterion> criterionsToSave) {
        if (criterionsToSave.isEmpty()) {
            return;
        }
        try {
            rubricCriterionRepository.saveAll(criterionsToSave);
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Data save error: the criterion code or standard Framework is duplicated in this Rubric version.", e);
        }
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    /**
     * Normalizes a code for matching: removes non-breaking space / zero-width space / BOM
     * (very common when copying data from Excel) that String.strip() does not remove.
     */
    private static String normalizeCode(String raw) {
        if (raw == null) return null;
        return raw.strip()
                .replaceAll("[\\u00A0\\u200B\\u200C\\u200D\\uFEFF]", "")
                .toLowerCase(Locale.ROOT);
    }
}
package com.sep.vox.application.port.input.service;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RubricVersionImportCommitHandler implements ImportCommitHandler {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    public ImportType supportedType() {
        return ImportType.RUBRIC_VERSION;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        Rubric rubric = rubricRepository.findById(session.getImportedEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found during background processing."));
        CommitRun run = buildCommitRun(rubric, resolveMapping(session));

        List<RubricVersion> versionsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;

        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;
            if (commitRow(row, run, versionsToSave)) {
                importedCount++;
            } else {
                invalidCount++;
            }
        }

        if (!versionsToSave.isEmpty()) rubricVersionRepository.saveAll(versionsToSave);

        // The session itself is saved by ImportCommitService, not here.
        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
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
            Rubric rubric,
            Map<String, String> mapping,
            Map<Integer, RubricVersion> existingVersionMap,
            Set<Integer> versionsInThisFile,
            Instant now) {}

    private CommitRun buildCommitRun(Rubric rubric, Map<String, String> mapping) {
        Map<Integer, RubricVersion> existingVersionMap = rubricVersionRepository.findByRubricId(rubric.getId()).stream()
                .collect(Collectors.toMap(rv -> rv.getVersion(), v -> v, (u, v) -> u));
        return new CommitRun(rubric, mapping, existingVersionMap, new HashSet<>(), Instant.now());
    }

    private record RowFields(String version, String name, String description, String scoringScaleMin,
                             String scoringScaleMax, String totalScoreMethod, String effectiveFrom,
                             String effectiveTo) {}

    private record ScoreScale(BigDecimal min, BigDecimal max) {
        static final ScoreScale DEFAULT = new ScoreScale(null, null);
    }

    private record EffectivePeriod(Instant from, Instant to) {}

    private record ValidatedRow(RowFields fields, int versionNum, RubricVersion targetVersion, ScoreScale scale,
                                RubricTotalScoreMethod method, EffectivePeriod period) {}

    /** Returns true when the row was imported, false when it was marked invalid. */
    private boolean commitRow(ImportRow row, CommitRun run, List<RubricVersion> versionsToSave) {
        List<Map<String, String>> errors = new ArrayList<>();
        Map<String, String> mappedData = mapRow(row, run.mapping());
        try {
            ValidatedRow validated = validateRow(run, mappedData, errors);
            if (!errors.isEmpty()) {
                markInvalid(row, errors);
                return false;
            }
            versionsToSave.add(applyToVersion(run, validated));
            row.setStatus(ImportRowStatus.IMPORTED);
            row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
            return true;
        } catch (Exception ex) {
            errors.add(error("general", "Background processing error: " + ex.getMessage()));
            markInvalid(row, errors);
            return false;
        }
    }

    private Map<String, String> mapRow(ImportRow row, Map<String, String> mapping) {
        Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
        Map<String, String> mappedData = new HashMap<>();
        for (Map.Entry<String, String> entry : rawData.entrySet()) {
            String sysField = mapping.get(entry.getKey());
            if (sysField != null) {
                mappedData.put(sysField, entry.getValue());
            }
        }
        return mappedData;
    }

    private void markInvalid(ImportRow row, List<Map<String, String>> errors) {
        row.setStatus(ImportRowStatus.INVALID);
        row.setErrorsJson(jsonSerializationPort.toJson(errors));
    }

    private ValidatedRow validateRow(CommitRun run, Map<String, String> mappedData, List<Map<String, String>> errors) {
        RowFields fields = new RowFields(
                mappedData.get("version"), mappedData.get("name"), mappedData.get("description"),
                mappedData.get("scoringScaleMin"), mappedData.get("scoringScaleMax"),
                mappedData.get("totalScoreMethod"), mappedData.get("effectiveFrom"), mappedData.get("effectiveTo"));
        addMissingFieldErrors(fields, errors);
        int versionNum = parseVersionNumber(fields, run, errors);
        RubricVersion targetVersion = errors.isEmpty() ? run.existingVersionMap().get(versionNum) : null;
        if (targetVersion != null && targetVersion.getStatus() != RubricStatus.DRAFT) {
            errors.add(error("version", "Only Version " + versionNum + " can be edited via import when it is in DRAFT status."));
        }
        ScoreScale scale = parseScoreScale(fields, errors);
        RubricTotalScoreMethod method = parseTotalScoreMethod(fields, errors);
        EffectivePeriod period = parseEffectivePeriod(fields, run.now(), errors);
        return new ValidatedRow(fields, versionNum, targetVersion, scale, method, period);
    }

    private static void addMissingFieldErrors(RowFields fields, List<Map<String, String>> errors) {
        if (fields.version() == null || fields.version().isBlank()) errors.add(error("version", "Version number is missing."));
        if (fields.scoringScaleMin() == null || fields.scoringScaleMin().isBlank()) errors.add(error("scoringScaleMin", "Minimum score (Min Score) is missing."));
        if (fields.scoringScaleMax() == null || fields.scoringScaleMax().isBlank()) errors.add(error("scoringScaleMax", "Maximum score (Max Score) is missing."));
        if (fields.totalScoreMethod() == null || fields.totalScoreMethod().isBlank()) errors.add(error("totalScoreMethod", "Total score method is missing."));
    }

    private static int parseVersionNumber(RowFields fields, CommitRun run, List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            return 0;
        }
        String safeVersionStr = fields.version() == null ? "" : fields.version().trim();
        int versionNum = 0;
        try {
            versionNum = Integer.parseInt(safeVersionStr);
            if (versionNum <= 0) errors.add(error("version", "Version must be greater than 0."));

            if (!run.versionsInThisFile().add(versionNum)) {
                errors.add(error("version", "Duplicate Version number " + versionNum + " within the Excel file."));
            }
        } catch (NumberFormatException e) {
            errors.add(error("version", "Version number is invalid (must be an integer)."));
        }
        return versionNum;
    }

    private static ScoreScale parseScoreScale(RowFields fields, List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            return ScoreScale.DEFAULT;
        }
        String safeMinStr = fields.scoringScaleMin() == null ? "" : fields.scoringScaleMin().trim();
        String safeMaxStr = fields.scoringScaleMax() == null ? "" : fields.scoringScaleMax().trim();
        try {
            BigDecimal minScore = new BigDecimal(safeMinStr);
            BigDecimal maxScore = new BigDecimal(safeMaxStr);
            if (minScore.compareTo(maxScore) > 0) errors.add(error("scoringScaleMin", "Minimum score must not exceed maximum score."));
            return new ScoreScale(minScore, maxScore);
        } catch (NumberFormatException e) {
            errors.add(error("scoringScaleMin", "Minimum or maximum score is not a valid number."));
            return ScoreScale.DEFAULT;
        }
    }

    private static RubricTotalScoreMethod parseTotalScoreMethod(RowFields fields, List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            return null;
        }
        String safeMethodStr = fields.totalScoreMethod() == null ? "" : fields.totalScoreMethod().trim();
        try {
            return RubricTotalScoreMethod.valueOf(safeMethodStr.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            errors.add(error("totalScoreMethod", "Scoring method is invalid (accepts only SUM or WEIGHTED_AVERAGE)."));
            return null;
        }
    }

    private static EffectivePeriod parseEffectivePeriod(RowFields fields, Instant now, List<Map<String, String>> errors) {
        Instant effectiveFrom = now;
        Instant effectiveTo = null;
        if (!errors.isEmpty()) {
            return new EffectivePeriod(effectiveFrom, effectiveTo);
        }
        try {
            if (fields.effectiveFrom() != null && !fields.effectiveFrom().isBlank()) {
                effectiveFrom = DateMapper.toImportedInstant(fields.effectiveFrom().trim(), DateMapper.DEFAULT_INPUT_ZONE);
            }
            if (fields.effectiveTo() != null && !fields.effectiveTo().isBlank()) {
                effectiveTo = DateMapper.toImportedInstant(fields.effectiveTo().trim(), DateMapper.DEFAULT_INPUT_ZONE);
            }

            if (effectiveFrom == null) effectiveFrom = now;
            if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                errors.add(error("effectiveTo", "End date must not be before start date."));
            }
        } catch (Exception e) {
            errors.add(error("effectiveFrom", "Date format is invalid."));
        }
        return new EffectivePeriod(effectiveFrom, effectiveTo);
    }

    private RubricVersion applyToVersion(CommitRun run, ValidatedRow validated) {
        RowFields fields = validated.fields();
        RubricVersion targetVersion = validated.targetVersion();
        if (targetVersion == null) {
            return insertVersion(run.rubric(), validated);
        }
        if (fields.name() != null && !fields.name().isBlank()) {
            targetVersion.setName(fields.name().trim());
        }
        if (fields.description() != null && !fields.description().isBlank()) {
            targetVersion.setDescription(fields.description().trim());
        }
        targetVersion.setScoringScaleMin(validated.scale().min());
        targetVersion.setScoringScaleMax(validated.scale().max());
        targetVersion.setTotalScoreMethod(validated.method());
        targetVersion.setEffectiveFrom(validated.period().from());
        targetVersion.setEffectiveTo(validated.period().to());
        return targetVersion;
    }

    private RubricVersion insertVersion(Rubric rubric, ValidatedRow validated) {
        RowFields fields = validated.fields();
        int versionNum = validated.versionNum();
        String safeCode = rubric.getCode() + "_V" + versionNum;
        String safeName = (fields.name() != null && !fields.name().isBlank())
                ? fields.name().trim()
                : rubric.getName() + " - Version " + versionNum;
        String safeDescription = (fields.description() != null && !fields.description().isBlank())
                ? fields.description().trim()
                : rubric.getDescription();
        return RubricVersion.draft(
                rubric.getId(), versionNum, safeCode, safeName, safeDescription,
                validated.period().from(), validated.period().to(), validated.scale().min(), validated.scale().max(),
                validated.method());
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}
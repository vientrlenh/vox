package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.service.rubric.RubricResultBandValidator;
import com.sep.vox.domain.service.rubric.ScoreRangeValidator;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.*;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RubricResultBandImportCommitHandler implements ImportCommitHandler {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    public ImportType supportedType() {
        return ImportType.RUBRIC_RESULT_BAND;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        RubricVersion version = requireDraftVersion(session.getImportedEntityId());
        CommitRun run = buildCommitRun(version, resolveMapping(session));

        List<RubricResultBand> bandsToSave = new ArrayList<>();
        long importedCount = 0; long invalidCount = 0;

        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;
            if (commitRow(row, run, bandsToSave)) {
                importedCount++;
            } else {
                invalidCount++;
            }
        }

        saveBands(bandsToSave);
        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    private RubricVersion requireDraftVersion(UUID versionId) {
        RubricVersion version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent version not found during background processing."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalArgumentException("Result Bands can only be imported when the Rubric version is in DRAFT status.");
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
            Map<String, RubricResultBand> existingCodeMap,
            NavigableMap<BigDecimal, RubricResultBand> bandsSoFarByMin,
            Set<String> codesInFile,
            Set<Integer> ordersInFile) {}

    private CommitRun buildCommitRun(RubricVersion version, Map<String, String> mapping) {
        List<RubricResultBand> existingBands = rubricResultBandRepository.findByRubricVersionId(version.getId());
        Map<String, RubricResultBand> existingCodeMap = existingBands.stream()
                .collect(Collectors.toMap(b -> normalizeCode(b.getCode()), b -> b, (u, v) -> u));

        // Accumulates the existing bands + bands already processed in the same file to check score-range overlap
        // between bands. A TreeMap (key = scoreMin) makes the check O(log n) per band instead of scanning a List
        // at O(n) per band (O(n log n) for the whole file instead of O(n^2)).
        NavigableMap<BigDecimal, RubricResultBand> bandsSoFarByMin = new TreeMap<>();
        existingBands.forEach(b -> bandsSoFarByMin.put(b.getScoreMin(), b));
        return new CommitRun(version, mapping, existingCodeMap, bandsSoFarByMin, new HashSet<>(), new HashSet<>());
    }

    private record RowFields(String code, String name, String description, String scoreMin, String scoreMax,
                             String order) {}

    private record ParsedScores(BigDecimal scoreMin, BigDecimal scoreMax, int order) {
        static final ParsedScores DEFAULT = new ParsedScores(null, null, 0);
    }

    private record ValidatedRow(RowFields fields, String safeCode, String safeName, ParsedScores scores,
                                RubricResultBand existingForCode, BigDecimal previousScoreMin) {}

    /** Returns true when the row was imported, false when it was marked invalid. */
    private boolean commitRow(ImportRow row, CommitRun run, List<RubricResultBand> bandsToSave) {
        List<Map<String, String>> errors = new ArrayList<>();
        Map<String, String> mappedData = mapRow(row, run.mapping());
        try {
            ValidatedRow validated = validateRow(run, mappedData, errors);
            if (!errors.isEmpty()) {
                restoreBandRemovedForOverlapCheck(run, validated);
                markInvalid(row, errors);
                return false;
            }
            RubricResultBand band = applyToBand(run.version().getId(), validated);
            bandsToSave.add(band);
            run.bandsSoFarByMin().put(validated.scores().scoreMin(), band);
            row.setStatus(ImportRowStatus.IMPORTED);
            row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
            return true;
        } catch (Exception ex) {
            errors.add(error("general", "System error: " + ex.getMessage()));
            markInvalid(row, errors);
            return false;
        }
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

    private ValidatedRow validateRow(CommitRun run, Map<String, String> mappedData, List<Map<String, String>> errors) {
        RowFields fields = new RowFields(
                mappedData.get("code"), mappedData.get("name"), mappedData.get("description"),
                mappedData.get("scoreMin"), mappedData.get("scoreMax"), mappedData.get("order"));
        addMissingFieldErrors(fields, errors);
        String safeCode = fields.code() != null ? fields.code().trim() : "";
        if (errors.isEmpty() && !run.codesInFile().add(normalizeCode(safeCode))) {
            errors.add(error("code", "Duplicate result band code '" + safeCode + "' within the Excel file."));
        }
        ParsedScores scores = parseScores(fields, safeCode, run, errors);

        // The band being edited (if this code already exists) must be taken out of the map temporarily before
        // the overlap check so it does not collide with itself, then re-inserted (at the new scoreMin) after
        // all validation passes.
        RubricResultBand existingForCode = errors.isEmpty() ? run.existingCodeMap().get(normalizeCode(safeCode)) : null;
        BigDecimal previousScoreMin = existingForCode != null ? existingForCode.getScoreMin() : null;
        String safeName = fields.name() == null ? "" : fields.name().trim();
        addOverlapError(run, scores, safeName, previousScoreMin, errors);
        return new ValidatedRow(fields, safeCode, safeName, scores, existingForCode, previousScoreMin);
    }

    private static void addMissingFieldErrors(RowFields fields, List<Map<String, String>> errors) {
        if (fields.code() == null || fields.code().isBlank()) errors.add(error("code", "Result band code is missing."));
        if (fields.name() == null || fields.name().isBlank()) errors.add(error("name", "Result band name is missing."));
        if (fields.scoreMin() == null || fields.scoreMin().isBlank()) errors.add(error("scoreMin", "Minimum score is missing."));
        if (fields.scoreMax() == null || fields.scoreMax().isBlank()) errors.add(error("scoreMax", "Maximum score is missing."));
        if (fields.order() == null || fields.order().isBlank()) errors.add(error("order", "Order is missing."));
    }

    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private ParsedScores parseScores(RowFields fields, String safeCode, CommitRun run, List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            return ParsedScores.DEFAULT;
        }
        try {
            BigDecimal scoreMin = new BigDecimal(trimOrEmpty(fields.scoreMin()));
            BigDecimal scoreMax = new BigDecimal(trimOrEmpty(fields.scoreMax()));
            int order = Integer.parseInt(trimOrEmpty(fields.order()));
            addScoreRangeErrors(run, scoreMin, scoreMax, order, safeCode, errors);
            return new ParsedScores(scoreMin, scoreMax, order);
        } catch (NumberFormatException e) {
            errors.add(error("general", "Score or order is not a valid number."));
            return ParsedScores.DEFAULT;
        }
    }

    private void addScoreRangeErrors(CommitRun run, BigDecimal scoreMin, BigDecimal scoreMax, int order,
            String safeCode, List<Map<String, String>> errors) {
        if (scoreMin.compareTo(BigDecimal.ZERO) < 0) errors.add(error("scoreMin", "Score must not be negative."));
        if (scoreMin.compareTo(scoreMax) > 0) errors.add(error("scoreMin", "Minimum score must not exceed the maximum."));
        if (order <= 0) errors.add(error("order", "Order must be greater than 0."));

        if (!run.ordersInFile().add(order)) errors.add(error("order", "Duplicate order " + order + " in the Excel file."));

        try {
            ScoreRangeValidator.assertWithinScale(run.version().getScoringScaleMin(), run.version().getScoringScaleMax(),
                    scoreMin, scoreMax, safeCode);
        } catch (IllegalArgumentException e) {
            errors.add(error("scoreMin", e.getMessage()));
        }
    }

    // No overlap (touching boundaries included) with the other existing bands or those already processed in the file
    private void addOverlapError(CommitRun run, ParsedScores scores, String safeName, BigDecimal previousScoreMin,
            List<Map<String, String>> errors) {
        if (!errors.isEmpty()) {
            return;
        }
        if (previousScoreMin != null) {
            run.bandsSoFarByMin().remove(previousScoreMin);
        }
        try {
            RubricResultBandValidator.assertNoOverlap(run.bandsSoFarByMin(), scores.scoreMin(), scores.scoreMax(), safeName);
        } catch (IllegalArgumentException e) {
            errors.add(error("scoreMin", e.getMessage()));
        }
    }

    // A failed row: if the old band was just taken out for the overlap check, put it back, because its old
    // value is still in force.
    private void restoreBandRemovedForOverlapCheck(CommitRun run, ValidatedRow validated) {
        if (validated.previousScoreMin() != null) {
            run.bandsSoFarByMin().put(validated.previousScoreMin(), validated.existingForCode());
        }
    }

    private RubricResultBand applyToBand(UUID versionId, ValidatedRow validated) {
        RubricResultBand targetBand = validated.existingForCode();
        ParsedScores scores = validated.scores();
        if (targetBand == null) {
            return RubricResultBand.create(versionId, validated.safeCode(), validated.safeName(),
                    validated.fields().description(), scores.scoreMin(), scores.scoreMax(), scores.order());
        }
        targetBand.setName(validated.safeName());
        targetBand.setDescription(validated.fields().description());
        targetBand.setScoreMin(scores.scoreMin());
        targetBand.setScoreMax(scores.scoreMax());
        targetBand.setOrder(scores.order());
        return targetBand;
    }

    private void saveBands(List<RubricResultBand> bandsToSave) {
        if (bandsToSave.isEmpty()) {
            return;
        }
        try {
            rubricResultBandRepository.saveAll(bandsToSave);
        } catch (DataConflictException e) {
            throw new IllegalArgumentException("Data save error: the result band code (Result Band) is duplicated in this Rubric version.", e);
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
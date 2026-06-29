package com.sep.vox.application.port.input.service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricCriterionBand;
import com.sep.vox.domain.repository.RubricCriterionBandRepository;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SystemRubricCriterionBandImportCommitHandler implements ImportCommitHandler {

    private final RubricCriterionBandRepository rubricCriterionBandRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public SystemRubricCriterionBandImportCommitHandler(
            RubricCriterionBandRepository rubricCriterionBandRepository,
            RubricCriterionRepository rubricCriterionRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.rubricCriterionBandRepository = rubricCriterionBandRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.RUBRIC_CRITERION_BAND;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID criterionId = session.getImportedEntityId();
        var criterion = rubricCriterionRepository.findById(criterionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Tiêu chí gốc khi xử lý ngầm."));

        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }

        // Tích hợp UPSERT
        List<RubricCriterionBand> existingBands = rubricCriterionBandRepository.findByCriterionId(criterionId);
        Map<String, RubricCriterionBand> existingCodeMap = existingBands.stream()
                .collect(Collectors.toMap(b -> b.getCode().toLowerCase().trim(), b -> b, (u, v) -> u));

        List<RubricCriterionBand> bandsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        OffsetDateTime now = OffsetDateTime.now();
        Set<String> codesInFile = new HashSet<>();

        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;

            List<String> errors = new ArrayList<>();
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());

            Map<String, String> mappedData = new HashMap<>();
            for (Map.Entry<String, String> entry : rawData.entrySet()) {
                String sysField = mapping.get(entry.getKey());
                if (sysField != null) mappedData.put(sysField, entry.getValue());
            }

            try {
                String codeStr = mappedData.get("code");
                String minStr = mappedData.get("scoreMin");
                String maxStr = mappedData.get("scoreMax");

                if (codeStr == null || codeStr.isBlank()) errors.add("Thiếu Mã mức độ (Code).");
                if (minStr == null || minStr.isBlank()) errors.add("Thiếu Điểm tối thiểu (Score Min).");
                if (maxStr == null || maxStr.isBlank()) errors.add("Thiếu Điểm tối đa (Score Max).");

                String safeCode = codeStr != null ? codeStr.trim() : "";
                if (errors.isEmpty() && !codesInFile.add(safeCode.toLowerCase())) {
                    errors.add("Bị trùng Mã mức độ '" + safeCode + "' ngay trong file Excel.");
                }

                BigDecimal scoreMin = null;
                BigDecimal scoreMax = null;
                if (errors.isEmpty()) {
                    try {
                        scoreMin = new BigDecimal(minStr.trim());
                        scoreMax = new BigDecimal(maxStr.trim());

                        if (scoreMin.compareTo(BigDecimal.ZERO) < 0) errors.add("Điểm số không được âm.");
                        if (scoreMin.compareTo(scoreMax) > 0) errors.add("Điểm tối thiểu không được lớn hơn tối đa.");

                        if (scoreMin.compareTo(criterion.getMinScore()) < 0 || scoreMax.compareTo(criterion.getMaxScore()) > 0) {
                            errors.add("Khoảng điểm của Band vượt quá giới hạn khoảng điểm của Tiêu chí gốc ("
                                    + criterion.getMinScore() + " - " + criterion.getMaxScore() + ").");
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Điểm số không hợp lệ.");
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                } else {
                    RubricCriterionBand targetBand = existingCodeMap.get(safeCode.toLowerCase());

                    if (targetBand != null) {
                        targetBand.setScoreMin(scoreMin);
                        targetBand.setScoreMax(scoreMax);
                        targetBand.setUpdatedAt(now);
                        targetBand.setUpdatedBy(session.getCreatedBy());
                        bandsToSave.add(targetBand);
                    } else {
                        targetBand = new RubricCriterionBand(
                                criterionId, safeCode, scoreMin, scoreMax,
                                now, now, session.getCreatedBy(), session.getCreatedBy()
                        );
                        bandsToSave.add(targetBand);
                    }

                    row.setStatus(ImportRowStatus.IMPORTED);
                    row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
                    importedCount++;
                }
            } catch (Exception ex) {
                errors.add("Lỗi xử lý luồng ngầm: " + ex.getMessage());
                row.setStatus(ImportRowStatus.INVALID);
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                invalidCount++;
            }
        }

        if (!bandsToSave.isEmpty()) rubricCriterionBandRepository.saveAll(bandsToSave);

        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }
}
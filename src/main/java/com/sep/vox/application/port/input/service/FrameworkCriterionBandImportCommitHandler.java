package com.sep.vox.application.port.input.service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FrameworkCriterionBandImportCommitHandler implements ImportCommitHandler {

    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public FrameworkCriterionBandImportCommitHandler(
            FrameworkCriterionBandRepository frameworkCriterionBandRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.FRAMEWORK_CRITERION_BAND;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID versionId = session.getImportedEntityId();
        FrameworkVersion version = frameworkVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản khung năng lực khi xử lý ngầm."));
        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể import Mức đánh giá tiêu chí khi phiên bản khung năng lực đang ở trạng thái DRAFT.");
        }

        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }

        List<FrameworkCriterion> criteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);
        Map<String, UUID> criterionCodeToId = criteria.stream()
                .collect(Collectors.toMap(c -> normalizeCode(c.getCode()), fc -> fc.getId(), (u, v) -> u));

        List<FrameworkResultBand> resultBands = frameworkResultBandRepository.findByFrameworkVersionId(versionId);
        Map<String, UUID> resultBandCodeToId = resultBands.stream()
                .collect(Collectors.toMap(b -> normalizeCode(b.getCode()), frb -> frb.getId(), (u, v) -> u));

        List<FrameworkCriterionBand> existingBands = criterionCodeToId.isEmpty()
                ? List.of()
                : frameworkCriterionBandRepository.findByFrameworkCriterionIdIn(criterionCodeToId.values());
        Map<String, FrameworkCriterionBand> existingPairMap = existingBands.stream()
                .collect(Collectors.toMap(
                        b -> pairKey(b.getFrameworkCriterionId(), b.getFrameworkResultBandId()),
                        b -> b, (u, v) -> u));

        List<FrameworkCriterionBand> bandsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        Instant now = Instant.now();

        Set<String> pairsInFile = new HashSet<>();

        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;

            List<Map<String, String>> errors = new ArrayList<>();
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());

            Map<String, String> mappedData = new HashMap<>();
            for (Map.Entry<String, String> entry : rawData.entrySet()) {
                String sysField = mapping.get(entry.getKey());
                if (sysField != null) mappedData.put(sysField, entry.getValue());
            }

            try {
                String criterionCodeStr = mappedData.get("criterionCode");
                String resultBandCodeStr = mappedData.get("resultBandCode");
                String descriptorStr = mappedData.get("descriptor");
                String positiveStr = mappedData.get("positiveSignals");
                String negativeStr = mappedData.get("negativeSignals");

                if (criterionCodeStr == null || criterionCodeStr.isBlank()) errors.add(error("criterionCode", "Thiếu Mã tiêu chí."));
                if (resultBandCodeStr == null || resultBandCodeStr.isBlank()) errors.add(error("resultBandCode", "Thiếu Mã mức kết quả."));
                if (descriptorStr == null || descriptorStr.isBlank()) errors.add(error("descriptor", "Thiếu Mô tả."));

                UUID criterionId = null;
                UUID resultBandId = null;
                if (errors.isEmpty() && criterionCodeStr != null && resultBandCodeStr != null) {
                    String safeCriterionCode = normalizeCode(criterionCodeStr);
                    String safeResultBandCode = normalizeCode(resultBandCodeStr);
                    criterionId = criterionCodeToId.get(safeCriterionCode);
                    resultBandId = resultBandCodeToId.get(safeResultBandCode);
                    if (criterionId == null) errors.add(error("criterionCode", "Mã tiêu chí '" + safeCriterionCode + "' không tồn tại trong phiên bản này."));
                    if (resultBandId == null) errors.add(error("resultBandCode", "Mã mức kết quả '" + safeResultBandCode + "' không tồn tại trong phiên bản này."));

                    if (criterionId != null && resultBandId != null) {
                        String pairKey = pairKey(criterionId, resultBandId);
                        if (!pairsInFile.add(pairKey)) {
                            errors.add(error("resultBandCode", "Bị trùng cặp Tiêu chí/Mức kết quả ngay trong file Excel."));
                        }
                    }
                }

                FrameworkCriterionSignals positiveSignals = errors.isEmpty()
                        ? parseSignals(positiveStr, "positiveSignals", errors) : null;
                FrameworkCriterionSignals negativeSignals = errors.isEmpty()
                        ? parseSignals(negativeStr, "negativeSignals", errors) : null;

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                } else {
                    var safeDescriptor = descriptorStr != null ? descriptorStr.trim() : "";
                    FrameworkCriterionBand targetBand = existingPairMap.get(pairKey(criterionId, resultBandId));
                    if (targetBand != null) {
                        targetBand.setDescriptor(safeDescriptor);
                        targetBand.setPositiveSignals(positiveSignals);
                        targetBand.setNegativeSignals(negativeSignals);
                        targetBand.setUpdatedAt(now);
                        targetBand.setUpdatedBy(session.getCreatedBy());
                        bandsToSave.add(targetBand);
                    } else {
                        targetBand = new FrameworkCriterionBand(
                                criterionId, resultBandId, safeDescriptor,
                                positiveSignals, negativeSignals,
                                now, now, session.getCreatedBy(), session.getCreatedBy()
                        );
                        bandsToSave.add(targetBand);
                    }

                    row.setStatus(ImportRowStatus.IMPORTED);
                    row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
                    importedCount++;
                }
            } catch (Exception ex) {
                errors.add(error("general", "Lỗi xử lý luồng ngầm: " + ex.getMessage()));
                row.setStatus(ImportRowStatus.INVALID);
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                invalidCount++;
            }
        }

        if (!bandsToSave.isEmpty()) {
            try {
                frameworkCriterionBandRepository.saveAll(bandsToSave);
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException("Lỗi lưu dữ liệu: Cặp Tiêu chí/Mức kết quả bị trùng lặp.", e);
            }
        }

        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    // Cú pháp mỗi dấu hiệu: code|description|importance|evidenceHint, các dấu hiệu cách nhau bởi ";"
    private static FrameworkCriterionSignals parseSignals(String raw, String field, List<Map<String, String>> errors) {
        if (raw == null || raw.isBlank()) return new FrameworkCriterionSignals(List.of());

        List<FrameworkCriterionSignal> signals = new ArrayList<>();
        String[] entries = raw.split(";");
        for (String entry : entries) {
            if (entry.isBlank()) continue;
            String[] parts = entry.split("\\|", -1);
            if (parts.length < 3) {
                errors.add(error(field, "Dấu hiệu '" + entry.trim() + "' sai định dạng (cần code|description|importance|evidenceHint)."));
                continue;
            }
            String code = parts[0].trim();
            String description = parts[1].trim();
            String importanceStr = parts[2].trim();
            String evidenceHint = parts.length > 3 && !parts[3].isBlank() ? parts[3].trim() : null;

            FrameworkCriterionSignalImportance importance;
            try {
                importance = FrameworkCriterionSignalImportance.valueOf(importanceStr.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                errors.add(error(field, "Độ quan trọng '" + importanceStr + "' không hợp lệ (Chỉ nhận: HIGH, MEDIUM, LOW)."));
                continue;
            }

            try {
                signals.add(new FrameworkCriterionSignal(code, description, importance, evidenceHint));
            } catch (IllegalArgumentException e) {
                errors.add(error(field, "Dấu hiệu '" + code + "' không hợp lệ: " + e.getMessage()));
            }
        }
        return new FrameworkCriterionSignals(signals);
    }

    private static String pairKey(UUID criterionId, UUID resultBandId) {
        return criterionId + "|" + resultBandId;
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private static String normalizeCode(String raw) {
        if (raw == null) return null;
        return raw.strip()
                .replaceAll("[\\u00A0\\u200B\\u200C\\u200D\\uFEFF]", "")
                .toUpperCase(Locale.ROOT);
    }
}

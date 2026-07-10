package com.sep.vox.application.port.input.service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.*;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RubricResultBandImportCommitHandler implements ImportCommitHandler {

    private final RubricResultBandRepository rubricResultBandRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public RubricResultBandImportCommitHandler(
            RubricResultBandRepository rubricResultBandRepository,
            RubricVersionRepository rubricVersionRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.rubricResultBandRepository = rubricResultBandRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.RUBRIC_RESULT_BAND;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID versionId = session.getImportedEntityId();
        var version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản gốc khi xử lý ngầm."));
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể import Xếp loại (Result Band) khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }

        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }

        List<RubricResultBand> existingBands = rubricResultBandRepository.findByRubricVersionId(versionId);
        Map<String, RubricResultBand> existingCodeMap = existingBands.stream()
                .collect(Collectors.toMap(b -> normalizeCode(b.getCode()), b -> b, (u, v) -> u));

        List<RubricResultBand> bandsToSave = new ArrayList<>();
        long importedCount = 0; long invalidCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        Set<String> codesInFile = new HashSet<>();
        Set<Integer> ordersInFile = new HashSet<>();

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
                String codeStr = mappedData.get("code");
                String nameStr = mappedData.get("name");
                String descStr = mappedData.get("description");
                String minStr = mappedData.get("scoreMin");
                String maxStr = mappedData.get("scoreMax");
                String orderStr = mappedData.get("order");

                if (codeStr == null || codeStr.isBlank()) errors.add(error("code", "Thiếu Mã xếp loại (Code)."));
                if (nameStr == null || nameStr.isBlank()) errors.add(error("name", "Thiếu Tên xếp loại."));
                if (minStr == null || minStr.isBlank()) errors.add(error("scoreMin", "Thiếu Điểm tối thiểu."));
                if (maxStr == null || maxStr.isBlank()) errors.add(error("scoreMax", "Thiếu Điểm tối đa."));
                if (orderStr == null || orderStr.isBlank()) errors.add(error("order", "Thiếu Thứ tự."));

                String safeCode = codeStr != null ? codeStr.trim() : "";
                if (errors.isEmpty() && !codesInFile.add(normalizeCode(safeCode))) {
                    errors.add(error("code", "Bị trùng Mã xếp loại '" + safeCode + "' ngay trong file Excel."));
                }

                BigDecimal scoreMin = null; BigDecimal scoreMax = null; int order = 0;
                if (errors.isEmpty()) {
                    try {
                        scoreMin = new BigDecimal(minStr.trim());
                        scoreMax = new BigDecimal(maxStr.trim());
                        order = Integer.parseInt(orderStr.trim());

                        if (scoreMin.compareTo(BigDecimal.ZERO) < 0) errors.add(error("scoreMin", "Điểm số không được âm."));
                        if (scoreMin.compareTo(scoreMax) > 0) errors.add(error("scoreMin", "Điểm tối thiểu không được lớn hơn tối đa."));
                        if (order <= 0) errors.add(error("order", "Thứ tự phải lớn hơn 0."));

                        if (!ordersInFile.add(order)) errors.add(error("order", "Bị trùng thứ tự " + order + " trong file Excel."));

                        // Kiểm tra khoảng điểm có vượt rào của Version không
                        if (version.getScoringScaleMin() != null && scoreMin.compareTo(version.getScoringScaleMin()) < 0) {
                            errors.add(error("scoreMin", "Điểm tối thiểu nhỏ hơn điểm sàn của phiên bản (" + version.getScoringScaleMin() + ")."));
                        }
                        if (version.getScoringScaleMax() != null && scoreMax.compareTo(version.getScoringScaleMax()) > 0) {
                            errors.add(error("scoreMax", "Điểm tối đa vượt quá điểm trần của phiên bản (" + version.getScoringScaleMax() + ")."));
                        }

                    } catch (NumberFormatException e) {
                        errors.add(error("general", "Điểm số hoặc thứ tự không đúng định dạng số."));
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                } else {
                    RubricResultBand targetBand = existingCodeMap.get(normalizeCode(safeCode));

                    if (targetBand != null) {
                        // Update
                        targetBand.setName(nameStr.trim());
                        targetBand.setDescription(descStr);
                        targetBand.setScoreMin(scoreMin);
                        targetBand.setScoreMax(scoreMax);
                        targetBand.setOrder(order);
                        targetBand.setUpdatedAt(now);
                        targetBand.setUpdatedBy(session.getCreatedBy());
                        bandsToSave.add(targetBand);
                    } else {
                        // Insert
                        targetBand = new RubricResultBand(
                                versionId, safeCode, nameStr.trim(), descStr, scoreMin, scoreMax, order,
                                now, now, session.getCreatedBy(), session.getCreatedBy()
                        );
                        bandsToSave.add(targetBand);
                    }
                    row.setStatus(ImportRowStatus.IMPORTED);
                    row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
                    importedCount++;
                }
            } catch (Exception ex) {
                errors.add(error("general", "Lỗi hệ thống: " + ex.getMessage()));
                row.setStatus(ImportRowStatus.INVALID);
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                invalidCount++;
            }
        }

        if (!bandsToSave.isEmpty()) {
            try {
                rubricResultBandRepository.saveAll(bandsToSave);
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException("Lỗi lưu dữ liệu: Mã xếp loại (Result Band) bị trùng lặp trong Phiên bản Rubric này.", e);
            }
        }

        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    /**
     * Chuẩn hóa code để so khớp: loại bỏ non-breaking space / zero-width space / BOM
     * (rất hay dính khi copy dữ liệu từ Excel) mà String.strip() không loại bỏ được.
     */
    private static String normalizeCode(String raw) {
        if (raw == null) return null;
        return raw.strip()
                .replaceAll("[\\u00A0\\u200B\\u200C\\u200D\\uFEFF]", "")
                .toLowerCase(Locale.ROOT);
    }
}
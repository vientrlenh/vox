package com.sep.vox.application.port.input.service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FrameworkResultBandImportCommitHandler implements ImportCommitHandler {

    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public FrameworkResultBandImportCommitHandler(
            FrameworkResultBandRepository frameworkResultBandRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.FRAMEWORK_RESULT_BAND;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID versionId = session.getImportedEntityId();
        FrameworkVersion version = frameworkVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản khung năng lực khi xử lý ngầm."));
        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể import Mức kết quả khi phiên bản khung năng lực đang ở trạng thái DRAFT.");
        }

        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }

        List<FrameworkResultBand> existingBands = frameworkResultBandRepository.findByFrameworkVersionId(versionId);
        Map<String, FrameworkResultBand> existingCodeMap = existingBands.stream()
                .collect(Collectors.toMap(b -> normalizeCode(b.getCode()), b -> b, (u, v) -> u));
        Map<Integer, FrameworkResultBand> existingOrderMap = existingBands.stream()
                .collect(Collectors.toMap(frb -> frb.getOrder(), b -> b, (u, v) -> u));

        List<FrameworkResultBand> bandsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        Instant now = Instant.now();

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
                String labelStr = mappedData.get("label");
                String descStr = mappedData.get("description");
                String orderStr = mappedData.get("order");

                if (codeStr == null || codeStr.isBlank()) errors.add(error("code", "Thiếu Mã kết quả."));
                if (labelStr == null || labelStr.isBlank()) errors.add(error("label", "Thiếu Nhãn kết quả."));
                if (orderStr == null || orderStr.isBlank()) errors.add(error("order", "Thiếu Thứ tự."));

                String safeCode = codeStr != null ? normalizeCode(codeStr) : "";
                if (errors.isEmpty() && !codesInFile.add(safeCode)) {
                    errors.add(error("code", "Bị trùng Mã kết quả '" + safeCode + "' ngay trong file Excel."));
                }
                FrameworkResultBand existingByCode = existingCodeMap.get(safeCode);

                int order = 0;
                if (errors.isEmpty() && orderStr != null) {
                    try {
                        order = Integer.parseInt(orderStr.trim());
                        if (order <= 0) errors.add(error("order", "Thứ tự phải lớn hơn 0."));
                        if (!ordersInFile.add(order)) {
                            errors.add(error("order", "Bị trùng Thứ tự " + order + " ngay trong file Excel."));
                        } else {
                            FrameworkResultBand orderOwner = existingOrderMap.get(order);
                            boolean isSelf = orderOwner != null && existingByCode != null
                                    && orderOwner.getId().equals(existingByCode.getId());
                            if (orderOwner != null && !isSelf) {
                                errors.add(error("order", "Thứ tự " + order + " đã được dùng bởi Mã kết quả '"
                                        + orderOwner.getCode() + "' đang có trong hệ thống."));
                            }
                        }
                    } catch (NumberFormatException e) {
                        errors.add(error("order", "Thứ tự không đúng định dạng số."));
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                } else {
                    var safeLabelStr = labelStr != null ? labelStr.trim() : "";
                    FrameworkResultBand targetBand = existingByCode;
                    if (targetBand != null) {
                        targetBand.setLabel(safeLabelStr);
                        targetBand.setDescription(descStr);
                        targetBand.setOrder(order);
                        targetBand.setUpdatedAt(now);
                        targetBand.setUpdatedBy(session.getCreatedBy());
                        bandsToSave.add(targetBand);
                    } else {
                        targetBand = new FrameworkResultBand(
                                versionId, safeCode, safeLabelStr, descStr, order,
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
                frameworkResultBandRepository.saveAll(bandsToSave);
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException("Lỗi lưu dữ liệu: Mã kết quả bị trùng lặp trong phiên bản này.", e);
            }
        }

        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
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

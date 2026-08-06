package com.sep.vox.application.port.input.service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FrameworkCriterionImportCommitHandler implements ImportCommitHandler {

    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public FrameworkCriterionImportCommitHandler(
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.FRAMEWORK_CRITERION;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID versionId = session.getImportedEntityId();
        FrameworkVersion version = frameworkVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Framework khi xử lý ngầm."));
        if (version.getStatus() != FrameworkVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể import Tiêu chí khi phiên bản Framework đang ở trạng thái DRAFT.");
        }

        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }

        List<FrameworkCriterion> existingCriteria = frameworkCriterionRepository.findByFrameworkVersionId(versionId);
        Map<String, FrameworkCriterion> existingCodeMap = existingCriteria.stream()
                .collect(Collectors.toMap(c -> normalizeCode(c.getCode()), c -> c, (u, v) -> u));
        Map<Integer, FrameworkCriterion> existingOrderMap = existingCriteria.stream()
                .collect(Collectors.toMap(fc -> fc.getOrder(), c -> c, (u, v) -> u));

        List<FrameworkCriterion> criteriaToSave = new ArrayList<>();
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
                String nameStr = mappedData.get("name");
                String descStr = mappedData.get("description");
                String orderStr = mappedData.get("order");

                if (codeStr == null || codeStr.isBlank()) errors.add(error("code", "Thiếu Mã tiêu chí."));
                if (nameStr == null || nameStr.isBlank()) errors.add(error("name", "Thiếu Tên tiêu chí."));
                if (orderStr == null || orderStr.isBlank()) errors.add(error("order", "Thiếu Thứ tự."));

                String safeCode = codeStr != null ? normalizeCode(codeStr) : "";
                if (errors.isEmpty()) {
                    if (!FrameworkCriterionCode.ALLOWED_CODES.contains(safeCode)) {
                        errors.add(error("code", "Mã tiêu chí '" + safeCode + "' không hợp lệ (Chỉ nhận: "
                                + String.join(", ", FrameworkCriterionCode.ALLOWED_CODES) + ")."));
                    } else if (!codesInFile.add(safeCode)) {
                        errors.add(error("code", "Bị trùng Mã tiêu chí '" + safeCode + "' ngay trong file Excel."));
                    }
                }
                FrameworkCriterion existingByCode = existingCodeMap.get(safeCode);

                int order = 0;
                if (errors.isEmpty() && orderStr != null) {
                    try {
                        order = Integer.parseInt(orderStr.trim());
                        if (order <= 0) errors.add(error("order", "Thứ tự phải lớn hơn 0."));
                        if (!ordersInFile.add(order)) {
                            errors.add(error("order", "Bị trùng Thứ tự " + order + " ngay trong file Excel."));
                        } else {
                            FrameworkCriterion orderOwner = existingOrderMap.get(order);
                            boolean isSelf = orderOwner != null && existingByCode != null
                                    && orderOwner.getId().equals(existingByCode.getId());
                            if (orderOwner != null && !isSelf) {
                                errors.add(error("order", "Thứ tự " + order + " đã được dùng bởi Mã tiêu chí '"
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
                    var safeNameStr = nameStr != null ? nameStr.trim() : "";
                    FrameworkCriterion targetCriterion = existingByCode;
                    if (targetCriterion != null) {
                        targetCriterion.setName(safeNameStr);
                        targetCriterion.setDescription(descStr);
                        targetCriterion.setOrder(order);
                        targetCriterion.setUpdatedAt(now);
                        targetCriterion.setUpdatedBy(session.getCreatedBy());
                        criteriaToSave.add(targetCriterion);
                    } else {
                        targetCriterion = new FrameworkCriterion(
                                versionId, safeCode, safeNameStr, descStr, order,
                                now, now, session.getCreatedBy(), session.getCreatedBy()
                        );
                        criteriaToSave.add(targetCriterion);
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

        if (!criteriaToSave.isEmpty()) {
            try {
                frameworkCriterionRepository.saveAll(criteriaToSave);
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException("Lỗi lưu dữ liệu: Mã tiêu chí bị trùng lặp trong phiên bản này.", e);
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

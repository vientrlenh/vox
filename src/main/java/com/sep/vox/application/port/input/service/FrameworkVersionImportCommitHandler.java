package com.sep.vox.application.port.input.service;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class FrameworkVersionImportCommitHandler implements ImportCommitHandler {

    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkRepository frameworkRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public FrameworkVersionImportCommitHandler(
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkRepository frameworkRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkRepository = frameworkRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.FRAMEWORK_VERSION;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID frameworkId = session.getImportedEntityId();
        Framework framework = frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khung năng lực gốc khi xử lý ngầm."));

        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }

        List<FrameworkVersion> versionsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        Instant now = Instant.now();

        // Chống trùng số Version ngay trong cùng một file Excel đầu vào
        Set<Integer> versionsInThisFile = new HashSet<>();

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
                String versionStr = mappedData.get("version");
                String nameStr = mappedData.get("name");
                String descriptionStr = mappedData.get("description");
                String fromStr = mappedData.get("effectiveFrom");
                String toStr = mappedData.get("effectiveTo");

                if (versionStr == null || versionStr.isBlank()) errors.add(error("version", "Thiếu số phiên bản."));

                int versionNum = 0;
                if (errors.isEmpty() && versionStr != null) {
                    try {
                        versionNum = Integer.parseInt(versionStr.trim());
                        if (versionNum <= 0) errors.add(error("version", "Phiên bản phải lớn hơn 0."));
                        if (!versionsInThisFile.add(versionNum)) {
                            errors.add(error("version", "Bị trùng số Version " + versionNum + " ngay trong file Excel."));
                        }
                    } catch (NumberFormatException e) {
                        errors.add(error("version", "Số phiên bản không hợp lệ (Phải là số nguyên)."));
                    }
                }

                FrameworkVersion targetVersion = errors.isEmpty()
                        ? frameworkVersionRepository.findByFrameworkIdAndVersion(frameworkId, versionNum).orElse(null)
                        : null;
                if (targetVersion != null && targetVersion.getStatus() != FrameworkVersionStatus.DRAFT) {
                    errors.add(error("version", "Chỉ có thể sửa phiên bản " + versionNum + " qua import khi đang ở trạng thái DRAFT."));
                }

                Instant effectiveFrom = now;
                Instant effectiveTo = null;
                if (errors.isEmpty()) {
                    try {
                        if (fromStr != null && !fromStr.isBlank()) {
                            effectiveFrom = DateMapper.toImportedInstant(fromStr.trim(), DateMapper.DEFAULT_INPUT_ZONE);
                        }
                        if (toStr != null && !toStr.isBlank()) {
                            effectiveTo = DateMapper.toImportedInstant(toStr.trim(), DateMapper.DEFAULT_INPUT_ZONE);
                        }
                        if (effectiveFrom == null) effectiveFrom = now;
                        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                            errors.add(error("effectiveTo", "Ngày kết thúc không được trước ngày bắt đầu."));
                        }
                    } catch (Exception e) {
                        errors.add(error("effectiveFrom", "Định dạng ngày tháng không hợp lệ."));
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                } else {
                    if (targetVersion != null) {
                        // UPDATE
                        if (nameStr != null && !nameStr.isBlank()) targetVersion.setName(nameStr.trim());
                        if (descriptionStr != null && !descriptionStr.isBlank()) targetVersion.setDescription(descriptionStr.trim());
                        targetVersion.setEffectiveFrom(effectiveFrom);
                        targetVersion.setEffectiveTo(effectiveTo);
                        targetVersion.setUpdatedAt(now);
                        targetVersion.setUpdatedBy(session.getCreatedBy());
                        versionsToSave.add(targetVersion);
                    } else {
                        // INSERT
                        String safeCode = framework.getCode().value() + "_V" + versionNum;
                        String safeName = (nameStr != null && !nameStr.isBlank())
                                ? nameStr.trim()
                                : framework.getName() + " - Version " + versionNum;
                        String safeDescription = (descriptionStr != null && !descriptionStr.isBlank())
                                ? descriptionStr.trim()
                                : framework.getDescription();

                        targetVersion = new FrameworkVersion(
                                frameworkId, safeCode, safeName, safeDescription, versionNum,
                                effectiveFrom, effectiveTo, FrameworkVersionStatus.DRAFT,
                                now, now, session.getCreatedBy(), session.getCreatedBy()
                        );
                        versionsToSave.add(targetVersion);
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

        if (!versionsToSave.isEmpty()) {
            try {
                for (FrameworkVersion v : versionsToSave) frameworkVersionRepository.save(v);
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException("Lỗi lưu dữ liệu: Mã hoặc số Version bị trùng lặp.", e);
            }
        }

        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}

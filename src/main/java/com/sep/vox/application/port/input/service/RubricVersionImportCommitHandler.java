package com.sep.vox.application.port.input.service;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RubricVersionImportCommitHandler implements ImportCommitHandler {

    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public RubricVersionImportCommitHandler(
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.RUBRIC_VERSION;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID rubricId = session.getImportedEntityId();
        var rubric = rubricRepository.findById(rubricId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc khi xử lý ngầm."));

        // Lấy bộ từ điển map cột
        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }

        // Tích hợp UPSERT: Lấy toàn bộ Version cũ dưới DB lên lưu vào Map để đối chiếu
        List<RubricVersion> existingVersionsList = rubricVersionRepository.findByRubricId(rubricId);
        Map<Integer, RubricVersion> existingVersionMap = existingVersionsList.stream()
                .collect(Collectors.toMap(RubricVersion::getVersion, v -> v, (u, v) -> u));

        List<RubricVersion> versionsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        // Chống trùng số Version ngay trong cùng một file Excel đầu vào
        Set<Integer> versionsInThisFile = new HashSet<>();

        // Bóc tách từng dòng Excel
        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) continue;

            List<String> errors = new ArrayList<>();
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());

            Map<String, String> mappedData = new HashMap<>();
            for (Map.Entry<String, String> entry : rawData.entrySet()) {
                String sysField = mapping.get(entry.getKey());
                if (sysField != null) {
                    mappedData.put(sysField, entry.getValue());
                }
            }

            try {
                String versionStr = mappedData.get("version");
                String nameStr = mappedData.get("name");
                String descriptionStr = mappedData.get("description");
                String minStr = mappedData.get("scoringScaleMin");
                String maxStr = mappedData.get("scoringScaleMax");
                String methodStr = mappedData.get("totalScoreMethod");
                String fromStr = mappedData.get("effectiveFrom");
                String toStr = mappedData.get("effectiveTo");

                if (versionStr == null || versionStr.isBlank()) errors.add("Thiếu số Version.");
                if (minStr == null || minStr.isBlank()) errors.add("Thiếu điểm sàn (Min Score).");
                if (maxStr == null || maxStr.isBlank()) errors.add("Thiếu điểm trần (Max Score).");
                if (methodStr == null || methodStr.isBlank()) errors.add("Thiếu phương pháp tính điểm tổng.");

                int versionNum = 0;
                if (errors.isEmpty()) {
                    try {
                        versionNum = Integer.parseInt(versionStr.trim());
                        if (versionNum <= 0) errors.add("Version phải lớn hơn 0.");

                        if (!versionsInThisFile.add(versionNum)) {
                            errors.add("Bị trùng số Version " + versionNum + " ngay trong file Excel.");
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Số Version không hợp lệ (Phải là số nguyên).");
                    }
                }

                BigDecimal minScore = null;
                BigDecimal maxScore = null;
                if (errors.isEmpty()) {
                    try {
                        minScore = new BigDecimal(minStr.trim());
                        maxScore = new BigDecimal(maxStr.trim());
                        if (minScore.compareTo(maxScore) > 0) errors.add("Điểm sàn không được lớn hơn điểm trần.");
                    } catch (NumberFormatException e) {
                        errors.add("Điểm sàn hoặc điểm trần không đúng định dạng số.");
                    }
                }

                RubricTotalScoreMethod method = null;
                if (errors.isEmpty()) {
                    try {
                        method = RubricTotalScoreMethod.valueOf(methodStr.trim().toUpperCase());
                    } catch (Exception e) {
                        errors.add("Phương pháp tính điểm không hợp lệ (Chỉ nhận SUM hoặc WEIGHTED_AVERAGE).");
                    }
                }

                OffsetDateTime effectiveFrom = now;
                OffsetDateTime effectiveTo = null;
                if (errors.isEmpty()) {
                    try {
                        if (fromStr != null && !fromStr.isBlank()) {
                            effectiveFrom = DateMapper.toOffsetDateTime(fromStr.trim());
                        }
                        if (toStr != null && !toStr.isBlank()) {
                            effectiveTo = DateMapper.toOffsetDateTime(toStr.trim());
                        }

                        if (effectiveFrom == null) effectiveFrom = now;
                        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                            errors.add("Ngày kết thúc không được trước ngày bắt đầu.");
                        }
                    } catch (Exception e) {
                        errors.add("Định dạng ngày tháng không hợp lệ.");
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                } else {
                    // CƠ CHẾ UPSERT Lõi
                    RubricVersion targetVersion = existingVersionMap.get(versionNum);

                    if (targetVersion != null) {
                        // UPDATE (Đã tồn tại Version này -> Cập nhật thông tin mới)
                        if (nameStr != null && !nameStr.isBlank()) {
                            targetVersion.setName(nameStr.trim());
                        }
                        if (descriptionStr != null && !descriptionStr.isBlank()) {
                            targetVersion.setDescription(descriptionStr.trim());
                        }
                        targetVersion.setScoringScaleMin(minScore);
                        targetVersion.setScoringScaleMax(maxScore);
                        targetVersion.setTotalScoreMethod(method);
                        targetVersion.setEffectiveFrom(effectiveFrom);
                        targetVersion.setEffectiveTo(effectiveTo);
                        targetVersion.setUpdatedAt(now);
                        targetVersion.setUpdatedBy(session.getCreatedBy());

                        versionsToSave.add(targetVersion);
                    } else {
                        // INSERT (Tạo Version mới hoàn toàn)
                        String safeCode = rubric.getCode() + "_V" + versionNum;
                        String safeName = (nameStr != null && !nameStr.isBlank())
                                ? nameStr.trim()
                                : rubric.getName() + " - Version " + versionNum;
                        String safeDescription = (descriptionStr != null && !descriptionStr.isBlank())
                                ? descriptionStr.trim()
                                : rubric.getDescription();

                        targetVersion = new RubricVersion(
                                rubricId, versionNum, safeCode, safeName, safeDescription,
                                RubricStatus.DRAFT, effectiveFrom, effectiveTo, minScore, maxScore,
                                method, now, now, session.getCreatedBy(), session.getCreatedBy()
                        );
                        versionsToSave.add(targetVersion);
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

        // Lưu thông qua Spring Data JPA (Tự động Update nếu ID cũ, Insert nếu ID mới)
        if (!versionsToSave.isEmpty()) rubricVersionRepository.saveAll(versionsToSave);

        // Trả kết quả thống kê về cho ImportCommitService (Không gọi save Session ở đây nữa)
        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }
}
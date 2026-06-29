package com.sep.vox.application.port.input.service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RubricCriterionImportCommitHandler implements ImportCommitHandler {

    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricRepository rubricRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public RubricCriterionImportCommitHandler(
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.RUBRIC_CRITERION;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID versionId = session.getImportedEntityId();
        var version = rubricVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy Phiên bản Rubric khi xử lý ngầm."));
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc khi xử lý ngầm."));

        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }

        List<FrameworkCriterion> fwCriterions = frameworkCriterionRepository.findByFrameworkVersionId(rubric.getFrameworkId());
        Map<String, UUID> fwCodeToIdMap = fwCriterions.stream()
                .collect(Collectors.toMap(fc -> fc.getCode().toLowerCase().trim(), FrameworkCriterion::getId));

        List<RubricCriterion> existingCriterions = rubricCriterionRepository.findByRubricVersionId(versionId);

        Map<String, RubricCriterion> existingCodeMap = existingCriterions.stream()
                .collect(Collectors.toMap(
                        c -> c.getCode().toLowerCase().trim(),
                        c -> c,
                        (existingValue, newValue) -> existingValue
                ));

        List<RubricCriterion> criterionsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        Set<String> codesInFile = new HashSet<>();
        Set<Integer> ordersInFile = new HashSet<>();

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
                String nameStr = mappedData.get("name");
                String descStr = mappedData.get("description");
                String fwCodeStr = mappedData.get("frameworkCriterionCode");
                String examplesStr = mappedData.get("examples");
                String weightStr = mappedData.get("weight");
                String minStr = mappedData.get("minScore");
                String maxStr = mappedData.get("maxScore");
                String reqStr = mappedData.get("isRequired");
                String orderStr = mappedData.get("order");

                if (codeStr == null || codeStr.isBlank()) errors.add("Thiếu Mã tiêu chí.");
                if (nameStr == null || nameStr.isBlank()) errors.add("Thiếu Tên tiêu chí.");
                if (fwCodeStr == null || fwCodeStr.isBlank()) errors.add("Thiếu Mã Khung tiêu chuẩn tham chiếu.");
                if (weightStr == null || weightStr.isBlank()) errors.add("Thiếu Trọng số.");
                if (minStr == null || minStr.isBlank()) errors.add("Thiếu Điểm sàn.");
                if (maxStr == null || maxStr.isBlank()) errors.add("Thiếu Điểm trần.");
                if (orderStr == null || orderStr.isBlank()) errors.add("Thiếu Thứ tự.");

                String safeCode = codeStr != null ? codeStr.trim() : "";
                if (errors.isEmpty()) {
                    if (!codesInFile.add(safeCode.toLowerCase())) {
                        errors.add("Bị trùng Mã tiêu chí '" + safeCode + "' ngay trong file Excel.");
                    }
                }

                UUID fwCriterionId = null;
                if (errors.isEmpty()) {
                    fwCriterionId = fwCodeToIdMap.get(fwCodeStr.trim().toLowerCase());
                    if (fwCriterionId == null)
                        errors.add("Mã Khung tiêu chuẩn '" + fwCodeStr + "' không tồn tại trong Framework gốc.");
                }

                //  FIX LỖI THIẾU PARAMETER Ở ĐÂY
                RubricCriterionExamples examplesObj = null;
                if (errors.isEmpty() && examplesStr != null && !examplesStr.isBlank()) {
                    try {
                        List<String> rawExamples = Arrays.stream(examplesStr.split("[,;]"))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .toList();

                        if (!rawExamples.isEmpty()) {
                            List<RubricCriterionExample> exampleList = rawExamples.stream()
                                    .map(chuoi -> new RubricCriterionExample(
                                            chuoi,   // Truyền vào cho transcript
                                            chuoi,   // Truyền vào cho explanation
                                            BigDecimal.ZERO
                                    ))
                                    .toList();
                            examplesObj = new RubricCriterionExamples(exampleList);
                        }
                    } catch (Exception e) {
                        errors.add("Dữ liệu ví dụ không hợp lệ (Hãy phân cách các ví dụ bằng dấu ;). Lỗi: " + e.getMessage());
                    }
                }

                BigDecimal weight = null, minScore = null, maxScore = null;
                int order = 0;
                boolean isRequired = true;

                if (errors.isEmpty()) {
                    try {
                        weight = new BigDecimal(weightStr.trim());
                        if (weight.compareTo(BigDecimal.ZERO) < 0) errors.add("Trọng số không được âm.");

                        minScore = new BigDecimal(minStr.trim());
                        maxScore = new BigDecimal(maxStr.trim());
                        if (minScore.compareTo(maxScore) > 0) errors.add("Điểm sàn không được lớn hơn điểm trần.");

                        order = Integer.parseInt(orderStr.trim());
                        if (order <= 0) errors.add("Thứ tự phải lớn hơn 0.");

                        if (!ordersInFile.add(order)) {
                            errors.add("Bị trùng Thứ tự " + order + " ngay trong file Excel.");
                        }

                        if (reqStr != null && !reqStr.isBlank()) {
                            String r = reqStr.trim().toLowerCase();
                            isRequired = r.equals("true") || r.equals("yes") || r.equals("1") || r.equals("có") || r.equals("co");
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Dữ liệu Trọng số/Điểm/Thứ tự không đúng định dạng số.");
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                } else {
                    RubricCriterion targetCriterion = existingCodeMap.get(safeCode.toLowerCase());

                    if (targetCriterion != null) {
                        targetCriterion.setName(nameStr.trim());
                        targetCriterion.setDescription(descStr);
                        targetCriterion.setFrameworkCriterionId(fwCriterionId);
                        targetCriterion.setExamples(examplesObj);
                        targetCriterion.setWeight(weight);
                        targetCriterion.setMinScore(minScore);
                        targetCriterion.setMaxScore(maxScore);
                        targetCriterion.setOrder(order);
                        targetCriterion.setRequired(isRequired);
                        targetCriterion.setUpdatedAt(now);
                        targetCriterion.setUpdatedBy(session.getCreatedBy());

                        criterionsToSave.add(targetCriterion);
                    } else {
                        targetCriterion = new RubricCriterion(
                                versionId, fwCriterionId, safeCode, nameStr.trim(), descStr,
                                examplesObj, weight, minScore, maxScore, order, isRequired,
                                now, now, session.getCreatedBy(), session.getCreatedBy()
                        );
                        criterionsToSave.add(targetCriterion);
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

        if (!criterionsToSave.isEmpty()) rubricCriterionRepository.saveAll(criterionsToSave);

        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }
}
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
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.repository.*;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExample;
import com.sep.vox.domain.valueobject.rubric.RubricCriterionExamples;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final FrameworkVersionRepository frameworkVersionRepository;

    public RubricCriterionImportCommitHandler(
            RubricCriterionRepository rubricCriterionRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricRepository rubricRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            JsonSerializationPort jsonSerializationPort, FrameworkVersionRepository frameworkVersionRepository) {
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricRepository = rubricRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.frameworkVersionRepository = frameworkVersionRepository;
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
        if (version.getStatus() != RubricStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể import Tiêu chí khi phiên bản Rubric đang ở trạng thái DRAFT.");
        }
        var rubric = rubricRepository.findById(version.getRubricId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bộ Rubric gốc khi xử lý ngầm."));

        Map<String, String> mapping = new HashMap<>();
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        } else if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            mapping = jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }
        // 1. Lấy ra Phiên bản Khung năng lực đang PUBLISHED
        List<FrameworkVersion> activeVersions = frameworkVersionRepository.findByFrameworkVersionIdAndStatus(
                rubric.getFrameworkId(),
                FrameworkVersionStatus.PUBLISHED
        );

        var activeFwVersion = activeVersions.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phiên bản Khung tiêu chuẩn nào đang hoạt động cho Framework này."));

        // 2. SỬA CHỖ NÀY: Phải lấy theo ID của Phiên bản (Version ID), chứ không lấy theo Framework ID nữa!
        List<FrameworkCriterion> fwCriterions = frameworkCriterionRepository.findByFrameworkVersionId(activeFwVersion.getId());

        //  3. TỐI ƯU THÊM BỌC GIÁP: Thêm (existing, newValue) -> existing để đề phòng
        // dưới Database vô tình có 2 mã trùng nhau thì nó lấy thằng đầu tiên, không bị sập (Crash) toàn bộ luồng ngầm
        Map<String, UUID> fwCodeToIdMap = fwCriterions.stream()
                .collect(Collectors.toMap(
                        fc -> normalizeCode(fc.getCode()),
                        FrameworkCriterion::getId,
                        (existingValue, newValue) -> existingValue // Giáp chống sập Duplicate Key
                ));
        List<RubricCriterion> existingCriterions = rubricCriterionRepository.findByRubricVersionId(versionId);

        Map<String, RubricCriterion> existingCodeMap = existingCriterions.stream()
                .collect(Collectors.toMap(
                        c -> normalizeCode(c.getCode()),
                        c -> c,
                        (existingValue, newValue) -> existingValue
                ));

        // Tra cứu ngược: 1 Framework Criterion đang bị Rubric Criterion (code) nào trong version này chiếm giữ
        Map<UUID, RubricCriterion> existingByFwCriterionId = existingCriterions.stream()
                .collect(Collectors.toMap(
                        RubricCriterion::getFrameworkCriterionId,
                        c -> c,
                        (existingValue, newValue) -> existingValue
                ));

        List<RubricCriterion> criterionsToSave = new ArrayList<>();
        long importedCount = 0;
        long invalidCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        Set<String> codesInFile = new HashSet<>();
        Set<Integer> ordersInFile = new HashSet<>();
        Set<UUID> fwCriterionIdsInFile = new HashSet<>();

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
                String fwCodeStr = mappedData.get("frameworkCriterionCode");
                String examplesStr = mappedData.get("examples");
                String weightStr = mappedData.get("weight");
                String minStr = mappedData.get("minScore");
                String maxStr = mappedData.get("maxScore");
                String reqStr = mappedData.get("isRequired");
                String orderStr = mappedData.get("order");

                if (codeStr == null || codeStr.isBlank()) errors.add(error("code", "Thiếu Mã tiêu chí."));
                if (nameStr == null || nameStr.isBlank()) errors.add(error("name", "Thiếu Tên tiêu chí."));
                if (fwCodeStr == null || fwCodeStr.isBlank()) errors.add(error("frameworkCriterionCode", "Thiếu Mã Khung tiêu chuẩn tham chiếu."));
                if (weightStr == null || weightStr.isBlank()) errors.add(error("weight", "Thiếu Trọng số."));
                if (minStr == null || minStr.isBlank()) errors.add(error("minScore", "Thiếu Điểm sàn."));
                if (maxStr == null || maxStr.isBlank()) errors.add(error("maxScore", "Thiếu Điểm trần."));
                if (orderStr == null || orderStr.isBlank()) errors.add(error("order", "Thiếu Thứ tự."));

                String safeCode = codeStr != null ? codeStr.trim() : "";
                if (errors.isEmpty()) {
                    if (!codesInFile.add(normalizeCode(safeCode))) {
                        errors.add(error("code", "Bị trùng Mã tiêu chí '" + safeCode + "' ngay trong file Excel."));
                    }
                }

                UUID fwCriterionId = null;
                if (errors.isEmpty()) {
                    fwCriterionId = fwCodeToIdMap.get(normalizeCode(fwCodeStr));
                    if (fwCriterionId == null) {
                        errors.add(error("frameworkCriterionCode", "Mã Khung tiêu chuẩn '" + fwCodeStr + "' không tồn tại trong Framework gốc."));
                    }
                }

                RubricCriterion targetCriterion = errors.isEmpty() ? existingCodeMap.get(normalizeCode(safeCode)) : null;

                // frameworkCriterionId là bất biến sau khi tạo (giống code):
                // - Nếu sắp TẠO MỚI (targetCriterion == null): framework này không được trùng với tiêu chí khác trong file/DB.
                // - Nếu tiêu chí ĐÃ TỒN TẠI: chặn cả dòng nếu file cố đổi sang framework khác, tránh tạo trạng thái
                //   nửa-vời (các field khác bị update theo dòng "sai" trong khi frameworkCriterionId vẫn giữ nguyên).
                if (errors.isEmpty() && fwCriterionId != null) {
                    if (targetCriterion == null) {
                        if (!fwCriterionIdsInFile.add(fwCriterionId)) {
                            errors.add(error("frameworkCriterionCode", "Bị trùng Mã Khung tiêu chuẩn tham chiếu '" + fwCodeStr + "' ngay trong file Excel."));
                        } else {
                            RubricCriterion holder = existingByFwCriterionId.get(fwCriterionId);
                            if (holder != null) {
                                errors.add(error("frameworkCriterionCode", "Mã Khung tiêu chuẩn '" + fwCodeStr
                                        + "' đã được gán cho tiêu chí khác (mã: " + holder.getCode() + ") trong phiên bản này."));
                            }
                        }
                    } else if (!fwCriterionId.equals(targetCriterion.getFrameworkCriterionId())) {
                        errors.add(error("frameworkCriterionCode", "Không thể thay đổi Khung tiêu chuẩn tham chiếu của tiêu chí '"
                                + safeCode + "' đã tồn tại (Mã Khung tiêu chuẩn tham chiếu không được phép sửa sau khi tạo)."));
                    }
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
                        errors.add(error("examples", "Dữ liệu ví dụ không hợp lệ (Hãy phân cách các ví dụ bằng dấu ;). Lỗi: " + e.getMessage()));
                    }
                }

                BigDecimal weight = null, minScore = null, maxScore = null;
                int order = 0;
                boolean isRequired = true;

                if (errors.isEmpty()) {
                    try {
                        weight = new BigDecimal(weightStr.trim());
                        if (weight.compareTo(BigDecimal.ZERO) < 0) errors.add(error("weight", "Trọng số không được âm."));

                        minScore = new BigDecimal(minStr.trim());
                        maxScore = new BigDecimal(maxStr.trim());
                        if (minScore.compareTo(maxScore) > 0) errors.add(error("minScore", "Điểm sàn không được lớn hơn điểm trần."));

                        order = Integer.parseInt(orderStr.trim());
                        if (order <= 0) errors.add(error("order", "Thứ tự phải lớn hơn 0."));

                        if (!ordersInFile.add(order)) {
                            errors.add(error("order", "Bị trùng Thứ tự " + order + " ngay trong file Excel."));
                        }

                        if (reqStr != null && !reqStr.isBlank()) {
                            String r = reqStr.trim().toLowerCase();
                            isRequired = r.equals("true") || r.equals("yes") || r.equals("1") || r.equals("có") || r.equals("co");
                        }
                    } catch (NumberFormatException e) {
                        errors.add(error("general", "Dữ liệu Trọng số/Điểm/Thứ tự không đúng định dạng số."));
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                } else {
                    if (targetCriterion != null) {
                        // Không setFrameworkCriterionId: tiêu chí đã tồn tại thì framework tham chiếu là bất biến,
                        // giống hệt cách "code" được bảo vệ không cho sửa sau khi tạo.
                        targetCriterion.setName(nameStr.trim());
                        targetCriterion.setDescription(descStr);
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
                errors.add(error("general", "Lỗi xử lý luồng ngầm: " + ex.getMessage()));
                row.setStatus(ImportRowStatus.INVALID);
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                invalidCount++;
            }
        }

        if (!criterionsToSave.isEmpty()) {
            try {
                rubricCriterionRepository.saveAll(criterionsToSave);
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException("Lỗi lưu dữ liệu: Mã tiêu chí hoặc Khung tiêu chuẩn (Framework) bị trùng lặp trong phiên bản Rubric này.", e);
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
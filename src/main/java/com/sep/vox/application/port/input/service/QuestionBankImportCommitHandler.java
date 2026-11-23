package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.SupportedLanguageRepository;

/**
 * Nhập ngân hàng câu hỏi.
 *
 * <p>Phạm vi sở hữu suy từ chính phiên import chứ không lấy từ file: {@code session.schoolId} có
 * giá trị thì đây là ngân hàng của trường đó, {@code null} thì là ngân hàng hệ thống. Quyền đã
 * được chốt ở bước preview, nên file không có cách nào tự nâng phạm vi của mình.
 *
 * <p>Upsert theo {@code code} trong phạm vi đó. Trạng thái ngân hàng KHÔNG bị import ghi đè, cùng
 * lý do với chủ đề: một lần import lỡ tay không được kéo ngân hàng đang PUBLISHED về DRAFT và làm
 * biến mất câu hỏi khỏi tầm nhìn của giáo viên.
 */
@Service
@RequiredArgsConstructor
public class QuestionBankImportCommitHandler implements ImportCommitHandler {

    private final QuestionBankRepository questionBankRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    public ImportType supportedType() {
        return ImportType.QUESTION_BANK;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID schoolId = session.getSchoolId();
        QuestionBankOwnerType ownerType = schoolId == null ? QuestionBankOwnerType.SYSTEM : QuestionBankOwnerType.SCHOOL;
        Map<String, String> mapping = resolveMapping(session);

        Map<String, QuestionBank> existingByCode = new HashMap<String, QuestionBank>();
        for (QuestionBank bank : questionBankRepository.findByOwnerScope(ownerType, schoolId)) {
            existingByCode.putIfAbsent(normalizeCode(bank.getCode()), bank);
        }

        List<QuestionBank> toSave = new ArrayList<QuestionBank>();
        Set<String> codesInThisFile = new HashSet<String>();
        long importedCount = 0;
        long invalidCount = 0;

        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) {
                continue;
            }

            List<Map<String, String>> errors = new ArrayList<Map<String, String>>();
            try {
                Map<String, String> mappedData = mapRow(row, mapping);
                String code = StringNormalization.normalizeCode(mappedData.get("code"));
                String name = StringNormalization.trimAndCollapseSpaces(mappedData.get("name"));
                String description = StringNormalization.trimAndCollapseSpaces(mappedData.get("description"));
                String languageValue = StringNormalization.trimAndCollapseSpaces(mappedData.get("language"));

                if (isBlank(code)) {
                    errors.add(error("code", "Thiếu mã ngân hàng câu hỏi."));
                }
                if (isBlank(name)) {
                    errors.add(error("name", "Thiếu tên ngân hàng câu hỏi."));
                }
                if (errors.isEmpty() && !codesInThisFile.add(normalizeCode(code))) {
                    errors.add(error("code", "Mã ngân hàng '" + code + "' bị lặp ngay trong file."));
                }

                QuestionBank existing = errors.isEmpty() ? existingByCode.get(normalizeCode(code)) : null;

                // Ngôn ngữ bắt buộc khi TẠO MỚI (questionBank.languageId không nullable). Khi cập
                // nhật thì bỏ trống được -- giữ nguyên ngôn ngữ cũ, để file chỉ sửa tên/mô tả
                // không phải khai lại mọi thứ.
                java.util.UUID languageId = null;
                if (errors.isEmpty()) {
                    if (!isBlank(languageValue)) {
                        languageId = resolveLanguageId(languageValue);
                        if (languageId == null) {
                            errors.add(error("language", "Không tìm thấy ngôn ngữ '" + languageValue + "'."));
                        }
                    } else if (existing == null) {
                        errors.add(error("language", "Thiếu ngôn ngữ (bắt buộc khi tạo ngân hàng mới)."));
                    }
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                    continue;
                }

                if (existing != null) {
                    existing.setName(name);
                    if (!isBlank(description)) {
                        existing.setDescription(description);
                    }
                    if (languageId != null) {
                        existing.setLanguageId(languageId);
                    }
                    toSave.add(existing);
                } else {
                    toSave.add(QuestionBank.create(
                        languageId,
                        schoolId,
                        code,
                        name,
                        description,
                        ownerType
                    ));
                }

                row.setStatus(ImportRowStatus.IMPORTED);
                row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
                importedCount++;
            } catch (Exception ex) {
                errors.add(error("general", "Lỗi xử lý luồng ngầm: " + ex.getMessage()));
                row.setStatus(ImportRowStatus.INVALID);
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                invalidCount++;
            }
        }

        for (QuestionBank bank : toSave) {
            questionBankRepository.save(bank);
        }
        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    /** Nhận cả mã ("en", "vi") lẫn tên đầy đủ, vì người soạn file gõ kiểu nào cũng có. */
    private java.util.UUID resolveLanguageId(String value) {
        return supportedLanguageRepository.findByCode(value)
            .or(() -> supportedLanguageRepository.findByName(value))
            .map(language -> language.getId())
            .orElse(null);
    }

    private Map<String, String> resolveMapping(ImportSession session) {
        if (session.getConfirmedMappingJson() != null && !session.getConfirmedMappingJson().isBlank()) {
            return jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        }
        if (session.getSuggestedMappingJson() != null && !session.getSuggestedMappingJson().isBlank()) {
            return jsonSerializationPort.toStringMap(session.getSuggestedMappingJson());
        }
        return Map.of();
    }

    private Map<String, String> mapRow(ImportRow row, Map<String, String> mapping) {
        Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
        Map<String, String> mappedData = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : rawData.entrySet()) {
            String systemField = mapping.get(entry.getKey());
            if (systemField != null) {
                mappedData.put(systemField, entry.getValue());
            }
        }
        return mappedData;
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}

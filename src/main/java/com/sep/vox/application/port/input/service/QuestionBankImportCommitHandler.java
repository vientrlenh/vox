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
        CommitScope scope = new CommitScope(schoolId, ownerType, resolveMapping(session),
            loadExistingByCode(ownerType, schoolId), new HashSet<String>(), new ArrayList<QuestionBank>());

        long importedCount = 0;
        long invalidCount = 0;
        for (ImportRow row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) {
                continue;
            }
            if (commitRow(row, scope)) {
                importedCount++;
            } else {
                invalidCount++;
            }
        }

        for (QuestionBank bank : scope.toSave()) {
            questionBankRepository.save(bank);
        }
        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    private Map<String, QuestionBank> loadExistingByCode(QuestionBankOwnerType ownerType, UUID schoolId) {
        Map<String, QuestionBank> existingByCode = new HashMap<String, QuestionBank>();
        for (QuestionBank bank : questionBankRepository.findByOwnerScope(ownerType, schoolId)) {
            existingByCode.putIfAbsent(normalizeCode(bank.getCode()), bank);
        }
        return existingByCode;
    }

    private record CommitScope(
        UUID schoolId,
        QuestionBankOwnerType ownerType,
        Map<String, String> mapping,
        Map<String, QuestionBank> existingByCode,
        Set<String> codesInThisFile,
        List<QuestionBank> toSave
    ) {
    }

    private record BankFields(String code, String name, String description, String languageValue) {
    }

    /** Returns true when the row was imported, false when it was marked invalid. */
    private boolean commitRow(ImportRow row, CommitScope scope) {
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();
        try {
            Map<String, String> mappedData = mapRow(row, scope.mapping());
            BankFields fields = readFields(mappedData);
            validateRequiredFields(fields, scope.codesInThisFile(), errors);
            QuestionBank existing = errors.isEmpty() ? scope.existingByCode().get(normalizeCode(fields.code())) : null;
            UUID languageId = resolveLanguageForRow(fields.languageValue(), existing, errors);

            if (!errors.isEmpty()) {
                markInvalid(row, errors);
                return false;
            }

            scope.toSave().add(applyToBank(existing, languageId, fields, scope));
            row.setStatus(ImportRowStatus.IMPORTED);
            row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
            return true;
        } catch (Exception ex) {
            errors.add(error("general", "Lỗi xử lý luồng ngầm: " + ex.getMessage()));
            markInvalid(row, errors);
            return false;
        }
    }

    private BankFields readFields(Map<String, String> mappedData) {
        return new BankFields(
            StringNormalization.normalizeCode(mappedData.get("code")),
            StringNormalization.trimAndCollapseSpaces(mappedData.get("name")),
            StringNormalization.trimAndCollapseSpaces(mappedData.get("description")),
            StringNormalization.trimAndCollapseSpaces(mappedData.get("language"))
        );
    }

    private void validateRequiredFields(BankFields fields, Set<String> codesInThisFile, List<Map<String, String>> errors) {
        if (isBlank(fields.code())) {
            errors.add(error("code", "Thiếu mã ngân hàng câu hỏi."));
        }
        if (isBlank(fields.name())) {
            errors.add(error("name", "Thiếu tên ngân hàng câu hỏi."));
        }
        if (errors.isEmpty() && !codesInThisFile.add(normalizeCode(fields.code()))) {
            errors.add(error("code", "Mã ngân hàng '" + fields.code() + "' bị lặp ngay trong file."));
        }
    }

    // Language is required when CREATING (questionBank.languageId is not nullable). On update it
    // may be left blank -- the old language is kept, so a file that only edits name/description
    // does not have to restate everything.
    private UUID resolveLanguageForRow(String languageValue, QuestionBank existing, List<Map<String, String>> errors) {
        UUID languageId = null;
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
        return languageId;
    }

    private void markInvalid(ImportRow row, List<Map<String, String>> errors) {
        row.setStatus(ImportRowStatus.INVALID);
        row.setErrorsJson(jsonSerializationPort.toJson(errors));
    }

    private QuestionBank applyToBank(QuestionBank existing, UUID languageId, BankFields fields, CommitScope scope) {
        if (existing == null) {
            return QuestionBank.create(
                languageId,
                scope.schoolId(),
                fields.code(),
                fields.name(),
                fields.description(),
                scope.ownerType()
            );
        }
        existing.setName(fields.name());
        if (!isBlank(fields.description())) {
            existing.setDescription(fields.description());
        }
        if (languageId != null) {
            existing.setLanguageId(languageId);
        }
        return existing;
    }

    /** Nhận cả mã ("en", "vi") lẫn tên đầy đủ, vì người soạn file gõ kiểu nào cũng có. */
    private UUID resolveLanguageId(String value) {
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

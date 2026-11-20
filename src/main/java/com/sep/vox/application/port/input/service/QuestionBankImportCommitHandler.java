package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
public class QuestionBankImportCommitHandler implements ImportCommitHandler {

    private final QuestionBankRepository questionBankRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public QuestionBankImportCommitHandler(
            QuestionBankRepository questionBankRepository,
            SupportedLanguageRepository supportedLanguageRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.questionBankRepository = questionBankRepository;
        this.supportedLanguageRepository = supportedLanguageRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.QUESTION_BANK;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        var schoolId = session.getSchoolId();
        var ownerType = schoolId == null ? QuestionBankOwnerType.SYSTEM : QuestionBankOwnerType.SCHOOL;
        var mapping = resolveMapping(session);

        var existingByCode = new HashMap<String, QuestionBank>();
        for (var bank : questionBankRepository.findByOwnerScope(ownerType, schoolId)) {
            existingByCode.putIfAbsent(normalizeCode(bank.getCode()), bank);
        }

        var toSave = new ArrayList<QuestionBank>();
        var codesInThisFile = new HashSet<String>();
        long importedCount = 0;
        long invalidCount = 0;
        var now = Instant.now();

        for (var row : rows) {
            if (row.getStatus() != ImportRowStatus.PENDING) {
                continue;
            }

            var errors = new ArrayList<Map<String, String>>();
            try {
                var mappedData = mapRow(row, mapping);
                var code = StringNormalization.normalizeCode(mappedData.get("code"));
                var name = StringNormalization.trimAndCollapseSpaces(mappedData.get("name"));
                var description = StringNormalization.trimAndCollapseSpaces(mappedData.get("description"));
                var languageValue = StringNormalization.trimAndCollapseSpaces(mappedData.get("language"));

                if (isBlank(code)) {
                    errors.add(error("code", "Thiếu mã ngân hàng câu hỏi."));
                }
                if (isBlank(name)) {
                    errors.add(error("name", "Thiếu tên ngân hàng câu hỏi."));
                }
                if (errors.isEmpty() && !codesInThisFile.add(normalizeCode(code))) {
                    errors.add(error("code", "Mã ngân hàng '" + code + "' bị lặp ngay trong file."));
                }

                var existing = errors.isEmpty() ? existingByCode.get(normalizeCode(code)) : null;

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
                    existing.setUpdatedAt(now);
                    existing.setUpdatedBy(session.getCreatedBy());
                    toSave.add(existing);
                } else {
                    toSave.add(QuestionBank.create(
                        languageId,
                        schoolId,
                        code,
                        name,
                        description,
                        ownerType,
                        now,
                        session.getCreatedBy()
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

        for (var bank : toSave) {
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
        var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
        var mappedData = new HashMap<String, String>();
        for (var entry : rawData.entrySet()) {
            var systemField = mapping.get(entry.getKey());
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

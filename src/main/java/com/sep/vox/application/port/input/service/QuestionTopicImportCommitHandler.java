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
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

/**
 * Nhập chủ đề câu hỏi vào MỘT ngân hàng.
 *
 * <p>Ngân hàng đích ghim ở {@code session.importedEntityId} ngay lúc preview, đúng cách
 * {@code RubricVersionImportCommitHandler} ghim rubricId — file Excel không cần (và không được)
 * chứa id ngân hàng, nên không có đường nào để một dòng lạc sang ngân hàng khác.
 *
 * <p>Upsert theo {@code code} trong phạm vi ngân hàng: chạy lại cùng một file là cập nhật, không
 * đẻ thêm bản trùng. Trạng thái chủ đề KHÔNG bị import ghi đè — {@code UpdateQuestionTopicStatusUseCase}
 * là nơi duy nhất đổi trạng thái, để một lần import lỡ tay không kéo chủ đề đang PUBLISHED về DRAFT
 * và làm hỏng các đề thi đang trỏ vào nó.
 */
@Service
@RequiredArgsConstructor
public class QuestionTopicImportCommitHandler implements ImportCommitHandler {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    public ImportType supportedType() {
        return ImportType.QUESTION_TOPIC;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        UUID questionBankId = requirePinnedQuestionBankId(session);
        // Reject a duplicate code inside the file: two rows with the same code in one save pass would
        // let the later row silently overwrite the earlier one, and the user would not know data was lost.
        CommitScope scope = new CommitScope(questionBankId, resolveMapping(session), loadExistingByCode(questionBankId),
            new HashSet<String>(), new ArrayList<QuestionTopic>());

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

        for (QuestionTopic topic : scope.toSave()) {
            questionTopicRepository.save(topic);
        }
        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
    }

    private UUID requirePinnedQuestionBankId(ImportSession session) {
        UUID questionBankId = session.getImportedEntityId();
        if (questionBankId == null) {
            throw new ResourceNotFoundException("Phiên import không ghim ngân hàng câu hỏi đích");
        }
        questionBankRepository.findById(questionBankId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi khi xử lý ngầm"));
        return questionBankId;
    }

    private Map<String, QuestionTopic> loadExistingByCode(UUID questionBankId) {
        Map<String, QuestionTopic> existingByCode = new HashMap<String, QuestionTopic>();
        for (QuestionTopic topic : questionTopicRepository.findByQuestionBankId(questionBankId)) {
            existingByCode.putIfAbsent(normalizeCode(topic.getCode()), topic);
        }
        return existingByCode;
    }

    private record CommitScope(
        UUID questionBankId,
        Map<String, String> mapping,
        Map<String, QuestionTopic> existingByCode,
        Set<String> codesInThisFile,
        List<QuestionTopic> toSave
    ) {
    }

    /** Returns true when the row was imported, false when it was marked invalid. */
    private boolean commitRow(ImportRow row, CommitScope scope) {
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();
        try {
            Map<String, String> mappedData = mapRow(row, scope.mapping());
            String code = StringNormalization.normalizeCode(mappedData.get("code"));
            String name = StringNormalization.trimAndCollapseSpaces(mappedData.get("name"));
            String description = StringNormalization.trimAndCollapseSpaces(mappedData.get("description"));
            validateRequiredFields(code, name, scope.codesInThisFile(), errors);

            if (!errors.isEmpty()) {
                markInvalid(row, errors);
                return false;
            }

            scope.toSave().add(applyToTopic(scope, code, name, description));
            row.setStatus(ImportRowStatus.IMPORTED);
            row.setMappedDataJson(jsonSerializationPort.toJson(mappedData));
            return true;
        } catch (Exception ex) {
            errors.add(error("general", "Lỗi xử lý luồng ngầm: " + ex.getMessage()));
            markInvalid(row, errors);
            return false;
        }
    }

    private void validateRequiredFields(String code, String name, Set<String> codesInThisFile, List<Map<String, String>> errors) {
        if (isBlank(code)) {
            errors.add(error("code", "Thiếu mã chủ đề."));
        }
        if (isBlank(name)) {
            errors.add(error("name", "Thiếu tên chủ đề."));
        }
        if (errors.isEmpty() && !codesInThisFile.add(normalizeCode(code))) {
            errors.add(error("code", "Mã chủ đề '" + code + "' bị lặp ngay trong file."));
        }
    }

    private void markInvalid(ImportRow row, List<Map<String, String>> errors) {
        row.setStatus(ImportRowStatus.INVALID);
        row.setErrorsJson(jsonSerializationPort.toJson(errors));
    }

    private QuestionTopic applyToTopic(CommitScope scope, String code, String name, String description) {
        QuestionTopic existing = scope.existingByCode().get(normalizeCode(code));
        if (existing == null) {
            return QuestionTopic.draft(scope.questionBankId(), code, name, description);
        }
        existing.setName(name);
        if (!isBlank(description)) {
            existing.setDescription(description);
        }
        return existing;
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

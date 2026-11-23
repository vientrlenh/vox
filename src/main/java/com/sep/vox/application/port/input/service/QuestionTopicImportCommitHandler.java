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
import com.sep.vox.domain.model.question.QuestionTopicStatus;
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
        UUID questionBankId = session.getImportedEntityId();
        if (questionBankId == null) {
            throw new ResourceNotFoundException("Phiên import không ghim ngân hàng câu hỏi đích");
        }
        questionBankRepository.findById(questionBankId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi khi xử lý ngầm"));

        Map<String, String> mapping = resolveMapping(session);

        Map<String, QuestionTopic> existingByCode = new HashMap<String, QuestionTopic>();
        for (QuestionTopic topic : questionTopicRepository.findByQuestionBankId(questionBankId)) {
            existingByCode.putIfAbsent(normalizeCode(topic.getCode()), topic);
        }

        List<QuestionTopic> toSave = new ArrayList<QuestionTopic>();
        // Chặn trùng code ngay TRONG file: hai dòng cùng code mà cùng lượt lưu thì dòng sau ghi đè
        // dòng trước một cách âm thầm, người dùng không biết mình mất dữ liệu.
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

                if (isBlank(code)) {
                    errors.add(error("code", "Thiếu mã chủ đề."));
                }
                if (isBlank(name)) {
                    errors.add(error("name", "Thiếu tên chủ đề."));
                }
                if (errors.isEmpty() && !codesInThisFile.add(normalizeCode(code))) {
                    errors.add(error("code", "Mã chủ đề '" + code + "' bị lặp ngay trong file."));
                }

                if (!errors.isEmpty()) {
                    row.setStatus(ImportRowStatus.INVALID);
                    row.setErrorsJson(jsonSerializationPort.toJson(errors));
                    invalidCount++;
                    continue;
                }

                QuestionTopic existing = existingByCode.get(normalizeCode(code));
                if (existing != null) {
                    existing.setName(name);
                    if (!isBlank(description)) {
                        existing.setDescription(description);
                    }
                    toSave.add(existing);
                } else {
                    toSave.add(QuestionTopic.builder()
                            .questionBankId(questionBankId)
                            .code(code)
                            .name(name)
                            .description(description)
                            .status(QuestionTopicStatus.DRAFT)
                            .build());
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

        for (QuestionTopic topic : toSave) {
            questionTopicRepository.save(topic);
        }
        return new ImportCommitResult(importedCount, 0, 0, invalidCount);
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

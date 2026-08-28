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
import com.sep.vox.application.exception.NotFoundException;
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
public class QuestionTopicImportCommitHandler implements ImportCommitHandler {

    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionBankRepository questionBankRepository;
    private final JsonSerializationPort jsonSerializationPort;

    public QuestionTopicImportCommitHandler(
            QuestionTopicRepository questionTopicRepository,
            QuestionBankRepository questionBankRepository,
            JsonSerializationPort jsonSerializationPort) {
        this.questionTopicRepository = questionTopicRepository;
        this.questionBankRepository = questionBankRepository;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    public ImportType supportedType() {
        return ImportType.QUESTION_TOPIC;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        var questionBankId = session.getImportedEntityId();
        if (questionBankId == null) {
            throw new NotFoundException("Phiên import không ghim ngân hàng câu hỏi đích");
        }
        questionBankRepository.findById(questionBankId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi khi xử lý ngầm"));

        var mapping = resolveMapping(session);

        var existingByCode = new HashMap<String, QuestionTopic>();
        for (var topic : questionTopicRepository.findByQuestionBankId(questionBankId)) {
            existingByCode.putIfAbsent(normalizeCode(topic.getCode()), topic);
        }

        var toSave = new ArrayList<QuestionTopic>();
        // Chặn trùng code ngay TRONG file: hai dòng cùng code mà cùng lượt lưu thì dòng sau ghi đè
        // dòng trước một cách âm thầm, người dùng không biết mình mất dữ liệu.
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

                var existing = existingByCode.get(normalizeCode(code));
                if (existing != null) {
                    existing.setName(name);
                    if (!isBlank(description)) {
                        existing.setDescription(description);
                    }
                    existing.setUpdatedAt(now);
                    existing.setUpdatedBy(session.getCreatedBy());
                    toSave.add(existing);
                } else {
                    toSave.add(new QuestionTopic(
                        questionBankId,
                        code,
                        name,
                        description,
                        QuestionTopicStatus.DRAFT,
                        now,
                        now,
                        session.getCreatedBy(),
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

        for (var topic : toSave) {
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

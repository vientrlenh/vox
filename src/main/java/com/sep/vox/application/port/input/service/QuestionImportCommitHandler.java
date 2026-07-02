package com.sep.vox.application.port.input.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;

@Service
public class QuestionImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
        "questionBankId",
        "questionTopicId",
        "code",
        "type",
        "questionText",
        "instructionText",
        "promptText",
        "preparationText",
        "preparationTimeSeconds",
        "minResponseSeconds",
        "maxResponseSeconds",
        "sharing",
        "evaluationExpectedContent",
        "evaluationKeyPoints",
        "evaluationAcceptableResponses",
        "evaluationOffTopicExamples",
        "evaluationScoringHints",
        "evaluationCommonMistakes"
    );

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final TransactionTemplate transactionTemplate;

    public QuestionImportCommitHandler(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.jsonSerializationPort = jsonSerializationPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ImportType supportedType() {
        return ImportType.QUESTION;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        if (rows.isEmpty()) {
            return new ImportCommitResult(0L, 0L, 0L, 0L);
        }

        var importContext = resolveContext(rows.get(0));
        var confirmedMapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        var createdRows = 0L;
        var skippedRows = 0L;
        var invalidRows = 0L;
        var seenCodes = new HashSet<String>();

        for (var row : rows) {
            var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            var mappedData = mapRawData(rawData, confirmedMapping);
            var normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));

            var errors = validateRow(normalized, importContext, seenCodes);
            if (!errors.isEmpty()) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            if (hasDifferentContext(normalized, importContext)) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(
                    error("questionTopicId", "Du lieu row khong khop bank/topic da chon truoc khi import")
                )));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            var code = normalized.get("code");
            var questionText = normalized.get("questionText");
            if (isPresent(code) && questionRepository.existsByQuestionBankIdAndCode(importContext.questionBank().getId(), code)) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(
                    error("code", "Da ton tai cau hoi cung ma trong question bank nay")
                )));
                row.setStatus(ImportRowStatus.SKIPPED);
                skippedRows++;
                continue;
            }
            if (!isPresent(code)
                    && questionRepository.existsByQuestionBankIdAndQuestionTopicIdAndQuestionText(
                        importContext.questionBank().getId(),
                        importContext.questionTopic().getId(),
                        questionText)) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(
                    error("questionText", "Da ton tai cau hoi cung noi dung trong chu de nay")
                )));
                row.setStatus(ImportRowStatus.SKIPPED);
                skippedRows++;
                continue;
            }

            try {
                transactionTemplate.executeWithoutResult(status ->
                    createQuestion(normalized, importContext, session.getCreatedBy())
                );
                row.setErrorsJson(null);
                row.setStatus(ImportRowStatus.IMPORTED);
                createdRows++;
            } catch (DataIntegrityViolationException | IllegalArgumentException exception) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(
                    error("questionText", "Khong the tao cau hoi tu du lieu import")
                )));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
            }
        }

        return new ImportCommitResult(createdRows, 0L, skippedRows, invalidRows);
    }

    private ImportContext resolveContext(ImportRow row) {
        var rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
        var questionBankId = parseUuid(rawData.get("questionBankId"), "Khong tim thay question bank cua phien import");
        var questionTopicId = parseUuid(rawData.get("questionTopicId"), "Khong tim thay question topic cua phien import");
        var questionBank = questionBankRepository.findById(questionBankId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay ngan hang cau hoi"));
        var questionTopic = questionTopicRepository.findById(questionTopicId)
            .orElseThrow(() -> new NotFoundException("Khong tim thay chu de cau hoi"));
        if (!questionTopic.getQuestionBankId().equals(questionBank.getId())) {
            throw new IllegalStateException("Chu de khong thuoc ngan hang cau hoi da chon");
        }
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            throw new IllegalStateException("Chi duoc import cau hoi vao chu de da PUBLISHED");
        }
        return new ImportContext(questionBank, questionTopic);
    }

    private Map<String, String> mapRawData(Map<String, String> rawData, Map<String, String> confirmedMapping) {
        var mappedData = new LinkedHashMap<String, String>();
        rawData.forEach((originalHeader, value) -> {
            var systemField = confirmedMapping.get(originalHeader);
            if (systemField != null) {
                systemField = systemField.strip();
            }
            if (systemField != null && SUPPORTED_FIELDS.contains(systemField)) {
                mappedData.put(systemField, value);
            }
        });
        mappedData.put("questionBankId", rawData.get("questionBankId"));
        mappedData.put("questionTopicId", rawData.get("questionTopicId"));
        return mappedData;
    }

    private Map<String, String> normalize(Map<String, String> mappedData) {
        var normalized = new LinkedHashMap<String, String>();
        normalized.put("questionBankId", trimOrNull(mappedData.get("questionBankId")));
        normalized.put("questionTopicId", trimOrNull(mappedData.get("questionTopicId")));
        normalized.put("code", StringNormalization.normalizeCode(mappedData.get("code")));
        normalized.put("type", StringNormalization.normalizeCode(mappedData.get("type")));
        normalized.put("questionText", StringNormalization.trimAndCollapseSpaces(mappedData.get("questionText")));
        normalized.put("instructionText", StringNormalization.trimAndCollapseSpaces(mappedData.get("instructionText")));
        normalized.put("promptText", StringNormalization.trimAndCollapseSpaces(mappedData.get("promptText")));
        normalized.put("preparationText", StringNormalization.trimAndCollapseSpaces(mappedData.get("preparationText")));
        normalized.put("preparationTimeSeconds", trimOrNull(mappedData.get("preparationTimeSeconds")));
        normalized.put("minResponseSeconds", trimOrNull(mappedData.get("minResponseSeconds")));
        normalized.put("maxResponseSeconds", trimOrNull(mappedData.get("maxResponseSeconds")));
        normalized.put("sharing", mappedData.get("sharing") == null ? null : StringNormalization.normalizeCode(mappedData.get("sharing")));
        normalized.put("evaluationExpectedContent", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationExpectedContent")));
        normalized.put("evaluationKeyPoints", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationKeyPoints")));
        normalized.put("evaluationAcceptableResponses", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationAcceptableResponses")));
        normalized.put("evaluationOffTopicExamples", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationOffTopicExamples")));
        normalized.put("evaluationScoringHints", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationScoringHints")));
        normalized.put("evaluationCommonMistakes", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationCommonMistakes")));
        return normalized;
    }

    private List<Map<String, String>> validateRow(
            Map<String, String> data,
            ImportContext importContext,
            Set<String> seenCodes) {
        var errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "type", "Loai cau hoi khong duoc de trong");
        addMissingError(errors, data, "questionText", "Noi dung cau hoi khong duoc de trong");
        addMissingError(errors, data, "preparationTimeSeconds", "Thoi gian chuan bi khong duoc de trong");
        addMissingError(errors, data, "minResponseSeconds", "Thoi gian tra loi toi thieu khong duoc de trong");
        addMissingError(errors, data, "maxResponseSeconds", "Thoi gian tra loi toi da khong duoc de trong");

        validateEnumField(errors, data.get("type"), "type", QuestionType.class, "Loai cau hoi khong hop le");
        validateEnumField(errors, data.get("sharing"), "sharing", QuestionSharing.class, "Che do chia se khong hop le");
        validateNumberField(errors, data.get("preparationTimeSeconds"), "preparationTimeSeconds", "Thoi gian chuan bi phai la so nguyen >= 0");
        validateNumberField(errors, data.get("minResponseSeconds"), "minResponseSeconds", "Thoi gian tra loi toi thieu phai la so nguyen >= 0");
        validateNumberField(errors, data.get("maxResponseSeconds"), "maxResponseSeconds", "Thoi gian tra loi toi da phai la so nguyen >= 0");

        var minResponse = parseInteger(data.get("minResponseSeconds"));
        var maxResponse = parseInteger(data.get("maxResponseSeconds"));
        if (minResponse != null && maxResponse != null && minResponse > maxResponse) {
            errors.add(error("minResponseSeconds", "Thoi gian tra loi toi thieu khong duoc lon hon thoi gian tra loi toi da"));
        }

        var code = data.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Ma cau hoi bi trung trong file import"));
        }

        if (!Objects.equals(data.get("questionBankId"), importContext.questionBank().getId().toString())) {
            errors.add(error("questionBankId", "Question bank cua row khong hop le"));
        }
        if (!Objects.equals(data.get("questionTopicId"), importContext.questionTopic().getId().toString())) {
            errors.add(error("questionTopicId", "Question topic cua row khong hop le"));
        }

        return errors;
    }

    private void createQuestion(Map<String, String> data, ImportContext importContext, UUID currentUserId) {
        var now = OffsetDateTime.now();
        var question = new Question(
            importContext.questionBank().getId(),
            importContext.questionTopic().getId(),
            questionCodeOf(data.get("code")),
            data.get("instructionText"),
            data.get("questionText"),
            data.get("promptText"),
            data.get("preparationText"),
            QuestionType.valueOf(data.get("type")),
            Integer.parseInt(data.get("preparationTimeSeconds")),
            Integer.parseInt(data.get("minResponseSeconds")),
            Integer.parseInt(data.get("maxResponseSeconds")),
            data.get("sharing") == null ? QuestionSharing.PRIVATE : QuestionSharing.valueOf(data.get("sharing")),
            null,
            false,
            QuestionConfidentiality.OPEN,
            null,
            QuestionStatus.SUBMITTED_FOR_REVIEW,
            now,
            now,
            currentUserId,
            currentUserId
        );
        var savedQuestion = questionRepository.save(question);
        if (hasEvaluationGuide(data)) {
            questionEvaluationGuideRepository.save(new QuestionEvaluationGuide(
                savedQuestion.getId(),
                data.get("evaluationExpectedContent"),
                data.get("evaluationKeyPoints"),
                data.get("evaluationAcceptableResponses"),
                data.get("evaluationOffTopicExamples"),
                data.get("evaluationScoringHints"),
                data.get("evaluationCommonMistakes")
            ));
        }
    }

    private boolean hasDifferentContext(Map<String, String> data, ImportContext importContext) {
        return !importContext.questionBank().getId().toString().equals(data.get("questionBankId"))
            || !importContext.questionTopic().getId().toString().equals(data.get("questionTopicId"));
    }

    private <E extends Enum<E>> void validateEnumField(
            List<Map<String, String>> errors,
            String value,
            String field,
            Class<E> enumType,
            String message) {
        if (!isPresent(value)) {
            return;
        }
        try {
            Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            errors.add(error(field, message));
        }
    }

    private void validateNumberField(
            List<Map<String, String>> errors,
            String value,
            String field,
            String message) {
        if (!isPresent(value)) {
            return;
        }
        try {
            if (Integer.parseInt(value) < 0) {
                errors.add(error(field, message));
            }
        } catch (NumberFormatException exception) {
            errors.add(error(field, message));
        }
    }

    private Integer parseInteger(String value) {
        if (!isPresent(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private UUID parseUuid(String value, String message) {
        if (!isPresent(value)) {
            throw new IllegalStateException(message);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(message);
        }
    }

    private void addMissingError(List<Map<String, String>> errors, Map<String, String> data, String field, String message) {
        if (!isPresent(data.get(field))) {
            errors.add(error(field, message));
        }
    }

    private boolean hasEvaluationGuide(Map<String, String> data) {
        return isPresent(data.get("evaluationExpectedContent"))
            || isPresent(data.get("evaluationKeyPoints"))
            || isPresent(data.get("evaluationAcceptableResponses"))
            || isPresent(data.get("evaluationOffTopicExamples"))
            || isPresent(data.get("evaluationScoringHints"))
            || isPresent(data.get("evaluationCommonMistakes"));
    }

    private String questionCodeOf(String code) {
        if (isPresent(code)) {
            return code;
        }
        return "Q-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private String trimOrNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private record ImportContext(QuestionBank questionBank, QuestionTopic questionTopic) {
    }
}

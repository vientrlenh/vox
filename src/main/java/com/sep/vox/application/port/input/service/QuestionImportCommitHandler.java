package com.sep.vox.application.port.input.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.sep.vox.application.exception.DataConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.RequiresNewTransactionPort;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.application.port.input.usecase.question.QuestionAssetAnalysisRequestPublisher;
import com.sep.vox.application.port.input.usecase.question.QuestionAssetContentValidator;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolRepository;

@Service
@RequiredArgsConstructor
public class QuestionImportCommitHandler implements ImportCommitHandler {

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
        "questionBankId",
        "questionTopicId",
        "schoolCode",
        "questionBankCode",
        "questionTopicCode",
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
        "evaluationCommonMistakes",
        "assetType",
        "assetUrl",
        "assetTitle",
        "assetAltText",
        "assetTranscript",
        "assetDescription",
        "assetDurationSeconds"
    );

    private final QuestionRepository questionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final SchoolRepository schoolRepository;
    private final JsonSerializationPort jsonSerializationPort;
    private final RequiresNewTransactionPort requiresNewTransactionPort;

    @Override
    public ImportType supportedType() {
        return ImportType.QUESTION;
    }

    @Override
    public ImportCommitResult commit(ImportSession session, List<ImportRow> rows) {
        if (rows.isEmpty()) {
            return new ImportCommitResult(0L, 0L, 0L, 0L);
        }

        CommitRun run = buildCommitRun(session, rows.get(0), isBulkMode(rows.get(0)));
        long createdRows = 0L;
        long skippedRows = 0L;
        long invalidRows = 0L;

        for (ImportRow row : rows) {
            ImportRowStatus outcome = commitRow(row, run);
            if (outcome == ImportRowStatus.IMPORTED) {
                createdRows++;
            } else if (outcome == ImportRowStatus.SKIPPED) {
                skippedRows++;
            } else {
                invalidRows++;
            }
        }

        return new ImportCommitResult(createdRows, 0L, skippedRows, invalidRows);
    }

    // The single-target flow applies only when the preview pinned BOTH: the whole file goes into
    // exactly one topic. If either is missing, rows go through code resolution, where the bank and
    // the topic are resolved INDEPENDENTLY (whichever is pinned is used, whichever is not is read
    // from the code in the file). That covers the other three combinations, not only "both blank".
    //
    // Both are checked, not just questionBankId: pinning the bank but leaving the topic blank
    // would fall into the single-target branch and throw right away at parseUuid(questionTopicId).
    private boolean isBulkMode(ImportRow firstRow) {
        Map<String, String> firstRowRaw = jsonSerializationPort.toStringMap(firstRow.getRawDataJson());
        return !isPresent(firstRowRaw.get("questionBankId"))
            || !isPresent(firstRowRaw.get("questionTopicId"));
    }

    private CommitRun buildCommitRun(ImportSession session, ImportRow firstRow, boolean bulkMode) {
        return new CommitRun(
            session,
            bulkMode,
            bulkMode ? null : resolveContext(firstRow),
            bulkMode ? buildBulkScope(session) : null,
            jsonSerializationPort.toStringMap(session.getConfirmedMappingJson()),
            new HashSet<String>());
    }

    private record CommitRun(
        ImportSession session,
        boolean bulkMode,
        ImportContext sharedContext,
        BulkScope bulkScope,
        Map<String, String> confirmedMapping,
        Set<String> seenCodes
    ) {
    }

    private ImportRowStatus commitRow(ImportRow row, CommitRun run) {
        Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
        Map<String, String> mappedData = mapRawData(rawData, run.confirmedMapping());
        Map<String, String> normalized = normalize(mappedData);
        row.setMappedDataJson(jsonSerializationPort.toJson(normalized));

        List<Map<String, String>> errors = validateRow(normalized, run.sharedContext(), run.seenCodes());
        final ImportContext importContext = run.bulkMode()
            ? resolveContextFromCodes(normalized, run.bulkScope(), errors)
            : run.sharedContext();
        if (!errors.isEmpty() || importContext == null) {
            return rejectRow(row, errors, ImportRowStatus.INVALID);
        }
        if (!run.bulkMode() && hasDifferentContext(normalized, importContext)) {
            return rejectRow(row, List.of(
                error("questionTopicId", "row data does not belong to the same question bank and question topic as the import session")
            ), ImportRowStatus.INVALID);
        }
        ImportRowStatus skipped = skipIfDuplicate(row, normalized, importContext);
        if (skipped != null) {
            return skipped;
        }
        return createRowQuestion(row, normalized, importContext, run.session());
    }

    private ImportRowStatus rejectRow(ImportRow row, List<Map<String, String>> errors, ImportRowStatus status) {
        row.setErrorsJson(jsonSerializationPort.toJson(errors));
        row.setStatus(status);
        return status;
    }

    /** Returns {@link ImportRowStatus#SKIPPED} after marking the row when the question already exists, otherwise null. */
    private ImportRowStatus skipIfDuplicate(ImportRow row, Map<String, String> normalized, ImportContext importContext) {
        String code = normalized.get("code");
        String questionText = normalized.get("questionText");
        if (isPresent(code) && questionRepository.existsByQuestionBankIdAndCode(importContext.questionBank().getId(), code)) {
            return rejectRow(row, List.of(
                error("code", "A question with the same code already exists in this question bank")
            ), ImportRowStatus.SKIPPED);
        }
        if (!isPresent(code)
                && questionRepository.existsByQuestionBankIdAndQuestionTopicIdAndQuestionText(
                    importContext.questionBank().getId(),
                    importContext.questionTopic().getId(),
                    questionText)) {
            return rejectRow(row, List.of(
                error("questionText", "A question with the same content already exists in this topic")
            ), ImportRowStatus.SKIPPED);
        }
        return null;
    }

    private ImportRowStatus createRowQuestion(
            ImportRow row,
            Map<String, String> normalized,
            ImportContext importContext,
            ImportSession session) {
        try {
            requiresNewTransactionPort.runInNewTransaction(() ->
                createQuestion(normalized, importContext, session.createdByUserId().orElseThrow())
            );
            row.setErrorsJson(null);
            row.setStatus(ImportRowStatus.IMPORTED);
            return ImportRowStatus.IMPORTED;
        } catch (DataConflictException | IllegalArgumentException exception) {
            return rejectRow(row, List.of(
                error("questionText", "Cannot create a question from the import data")
            ), ImportRowStatus.FAILED);
        }
    }

    private ImportContext resolveContext(ImportRow row) {
        Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
        UUID questionBankId = parseUuid(rawData.get("questionBankId"), "Question bank of the import session not found");
        UUID questionTopicId = parseUuid(rawData.get("questionTopicId"), "Question topic of the import session not found");
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
            .orElseThrow(() -> new ResourceNotFoundException("Question bank not found"));
        QuestionTopic questionTopic = questionTopicRepository.findById(questionTopicId)
            .orElseThrow(() -> new ResourceNotFoundException("Question topic not found"));
        if (!questionTopic.getQuestionBankId().equals(questionBank.getId())) {
            throw new IllegalArgumentException("The topic does not belong to the selected question bank");
        }
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            throw new IllegalArgumentException("Questions can only be imported into a PUBLISHED topic");
        }
        return new ImportContext(questionBank, questionTopic);
    }

    /**
     * Finds the target bank and topic of ONE row by the codes written in the file (bulk mode).
     *
     * <p><b>The school scope does not come from the file.</b> {@code session.schoolId} was fixed at
     * preview time from the logged-in user: a value means a school session (only that school's SCHOOL
     * banks are searched), {@code null} means a system-admin session (SYSTEM banks). The
     * {@code schoolCode} column in the file is only used to CROSS-CHECK -- a match continues, a
     * mismatch reports a row error. If it decided the scope, anyone could type another school's code
     * and push questions into that school's bank.
     *
     * <p>Bank and topic are resolved INDEPENDENTLY: whichever the user already picked is pinned, the
     * other is read from the code in the file. So "pick a bank, leave the topic blank" only needs
     * the topic-code column, not the code of the bank just chosen on screen.
     *
     * <p>Returns {@code null} with the error already added to {@code errors} when it cannot resolve.
     */
    private ImportContext resolveContextFromCodes(
            Map<String, String> data,
            BulkScope scope,
            List<Map<String, String>> errors) {
        UUID pinnedBankId = parseUuidOrNull(data.get("questionBankId"));
        UUID pinnedTopicId = parseUuidOrNull(data.get("questionTopicId"));

        addMissingTargetCodeErrors(data, pinnedBankId, pinnedTopicId, errors);
        addSchoolCodeMismatchError(data.get("schoolCode"), scope, errors);
        if (!errors.isEmpty()) {
            return null;
        }

        String bankCode = data.get("questionBankCode");
        QuestionBank questionBank = findTargetBank(pinnedBankId, scope, bankCode, errors);
        if (questionBank == null) {
            return null;
        }
        if (pinnedTopicId != null) {
            return resolvePinnedTopic(pinnedTopicId, questionBank, errors);
        }
        return resolveTopicByCode(scope, questionBank, bankCode, data.get("questionTopicCode"), errors);
    }

    private QuestionBank findTargetBank(UUID pinnedBankId, BulkScope scope, String bankCode, List<Map<String, String>> errors) {
        QuestionBank questionBank = pinnedBankId != null
            ? questionBankRepository.findById(pinnedBankId).orElse(null)
            : scope.banksByCode().get(normalizeLookupKey(bankCode));
        if (questionBank == null) {
            errors.add(error(
                "questionBankCode",
                pinnedBankId != null
                    ? "The question bank selected on the import screen was not found"
                    : "Question bank not found with code '" + bankCode + "'"));
        }
        return questionBank;
    }

    private void addMissingTargetCodeErrors(
            Map<String, String> data,
            UUID pinnedBankId,
            UUID pinnedTopicId,
            List<Map<String, String>> errors) {
        if (pinnedBankId == null && !isPresent(data.get("questionBankCode"))) {
            errors.add(error("questionBankCode", "Question bank code is missing"));
        }
        if (pinnedTopicId == null && !isPresent(data.get("questionTopicCode"))) {
            errors.add(error("questionTopicCode", "Question topic code is missing"));
        }
    }

    private void addSchoolCodeMismatchError(String schoolCode, BulkScope scope, List<Map<String, String>> errors) {
        if (isPresent(schoolCode) && !schoolCode.equalsIgnoreCase(scope.schoolCode())) {
            errors.add(error(
                "schoolCode",
                "School code '" + schoolCode + "' does not match the scope of the import session"
                    + (scope.schoolCode() == null ? " (system bank)" : " ('" + scope.schoolCode() + "')")
            ));
        }
    }

    private ImportContext resolvePinnedTopic(
            UUID pinnedTopicId,
            QuestionBank questionBank,
            List<Map<String, String>> errors) {
        QuestionTopic pinnedTopic = questionTopicRepository.findById(pinnedTopicId).orElse(null);
        if (pinnedTopic == null || !pinnedTopic.getQuestionBankId().equals(questionBank.getId())) {
            errors.add(error(
                "questionTopicCode",
                "The selected topic does not belong to bank '" + questionBank.getCode() + "'"));
            return null;
        }
        if (pinnedTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            errors.add(error(
                "questionTopicCode",
                "The selected topic is in status " + pinnedTopic.getStatus()
                    + ". Only a PUBLISHED topic can be imported into."));
            return null;
        }
        return new ImportContext(questionBank, pinnedTopic);
    }

    private ImportContext resolveTopicByCode(
            BulkScope scope,
            QuestionBank questionBank,
            String bankCode,
            String topicCode,
            List<Map<String, String>> errors) {
        QuestionTopic questionTopic = scope.topicsByBankAndCode()
            .computeIfAbsent(questionBank.getId(), this::loadTopicsByCode)
            .get(normalizeLookupKey(topicCode));
        if (questionTopic == null) {
            errors.add(error(
                "questionTopicCode",
                "Question topic not found with code '" + topicCode + "' in bank '" + bankCode + "'"
            ));
            return null;
        }
        return requirePublishedTopic(questionBank, questionTopic, topicCode, errors);
    }

    private ImportContext requirePublishedTopic(
            QuestionBank questionBank,
            QuestionTopic questionTopic,
            String topicCode,
            List<Map<String, String>> errors) {
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            errors.add(error(
                "questionTopicCode",
                "Topic '" + topicCode + "' is in status " + questionTopic.getStatus()
                    + ". Only a PUBLISHED topic can be imported into."
            ));
            return null;
        }
        return new ImportContext(questionBank, questionTopic);
    }

    private Map<String, QuestionTopic> loadTopicsByCode(UUID bankId) {
        Map<String, QuestionTopic> byCode = new HashMap<String, QuestionTopic>();
        for (QuestionTopic topic : questionTopicRepository.findByQuestionBankId(bankId)) {
            byCode.putIfAbsent(normalizeLookupKey(topic.getCode()), topic);
        }
        return byCode;
    }

    /**
     * Lookup scope of a whole bulk import session, built ONCE and reused for every row.
     * Topics are loaded lazily per bank that actually appears in the file, not all up front.
     */
    private record BulkScope(
        String schoolCode,
        Map<String, QuestionBank> banksByCode,
        Map<UUID, Map<String, QuestionTopic>> topicsByBankAndCode
    ) {
    }

    private BulkScope buildBulkScope(ImportSession session) {
        UUID schoolId = session.getSchoolId();
        QuestionBankOwnerType ownerType = schoolId == null ? QuestionBankOwnerType.SYSTEM : QuestionBankOwnerType.SCHOOL;
        Map<String, QuestionBank> banksByCode = new HashMap<String, QuestionBank>();
        for (QuestionBank bank : questionBankRepository.findByOwnerScope(ownerType, schoolId)) {
            banksByCode.putIfAbsent(normalizeLookupKey(bank.getCode()), bank);
        }
        String schoolCode = schoolId == null
            ? null
            : schoolRepository.findById(schoolId)
                .map(school -> school.getCode() == null ? null : school.getCode().value())
                .orElse(null);
        return new BulkScope(schoolCode, banksByCode, new HashMap<>());
    }

    private static String normalizeLookupKey(String value) {
        return value == null ? "" : value.strip().toUpperCase(java.util.Locale.ROOT);
    }

    private Map<String, String> mapRawData(Map<String, String> rawData, Map<String, String> confirmedMapping) {
        Map<String, String> mappedData = new LinkedHashMap<String, String>();
        rawData.forEach((originalHeader, value) -> {
            String systemField = confirmedMapping.get(originalHeader);
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

    /**
     * Copies into the normalized map. This is a HAND-WRITTEN list, and every field not named here is
     * silently dropped even when it is in {@link #SUPPORTED_FIELDS} and mapped from the right column --
     * adding a new field and forgetting this place makes data vanish without a word.
     *
     * <p>That has already happened twice: the three target-code columns (bulk mode always reported
     * "Question bank code is missing" even when the file had it) and the seven asset columns (the asset
     * was never created, because {@code createAssetIfPresent} reads {@code assetType} from this very map).
     */
    private Map<String, String> normalize(Map<String, String> mappedData) {
        Map<String, String> normalized = new LinkedHashMap<String, String>();
        normalizeTargetFields(mappedData, normalized);
        normalizeQuestionFields(mappedData, normalized);
        normalizeEvaluationFields(mappedData, normalized);
        normalizeAssetFields(mappedData, normalized);
        return normalized;
    }

    private void normalizeTargetFields(Map<String, String> mappedData, Map<String, String> normalized) {
        normalized.put("questionBankId", trimOrNull(mappedData.get("questionBankId")));
        normalized.put("questionTopicId", trimOrNull(mappedData.get("questionTopicId")));
        // Per-row target codes in bulk mode. normalizeCode upper-cases -- matching how banks/topics are
        // looked up by code, and schoolCode is compared with equalsIgnoreCase so it is harmless.
        normalized.put("schoolCode", StringNormalization.normalizeCode(mappedData.get("schoolCode")));
        normalized.put("questionBankCode", StringNormalization.normalizeCode(mappedData.get("questionBankCode")));
        normalized.put("questionTopicCode", StringNormalization.normalizeCode(mappedData.get("questionTopicCode")));
    }

    private void normalizeQuestionFields(Map<String, String> mappedData, Map<String, String> normalized) {
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
    }

    private void normalizeEvaluationFields(Map<String, String> mappedData, Map<String, String> normalized) {
        normalized.put("evaluationExpectedContent", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationExpectedContent")));
        normalized.put("evaluationKeyPoints", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationKeyPoints")));
        normalized.put("evaluationAcceptableResponses", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationAcceptableResponses")));
        normalized.put("evaluationOffTopicExamples", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationOffTopicExamples")));
        normalized.put("evaluationScoringHints", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationScoringHints")));
        normalized.put("evaluationCommonMistakes", StringNormalization.trimAndCollapseSpaces(mappedData.get("evaluationCommonMistakes")));
    }

    // Assets. Only trimOrNull for url, transcript and description -- NOT trimAndCollapseSpaces:
    // a url is case- and whitespace-sensitive and must not be touched, and a transcript/description
    // has paragraph breaks (\n\n) that collapsing whitespace would glue into one block. Title and
    // alt text are short labels, so collapsing is fine.
    private void normalizeAssetFields(Map<String, String> mappedData, Map<String, String> normalized) {
        normalized.put("assetType", StringNormalization.normalizeCode(mappedData.get("assetType")));
        normalized.put("assetUrl", trimOrNull(mappedData.get("assetUrl")));
        normalized.put("assetTitle", StringNormalization.trimAndCollapseSpaces(mappedData.get("assetTitle")));
        normalized.put("assetAltText", StringNormalization.trimAndCollapseSpaces(mappedData.get("assetAltText")));
        normalized.put("assetTranscript", trimOrNull(mappedData.get("assetTranscript")));
        normalized.put("assetDescription", trimOrNull(mappedData.get("assetDescription")));
        normalized.put("assetDurationSeconds", trimOrNull(mappedData.get("assetDurationSeconds")));
    }

    /**
     * @param importContext {@code null} in bulk mode -- the row's target is not known yet and is
     *                      resolved separately by code, so the id cross-check is skipped.
     */
    private List<Map<String, String>> validateRow(
            Map<String, String> data,
            ImportContext importContext,
            Set<String> seenCodes) {
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();
        addRequiredFieldErrors(errors, data);
        addFieldFormatErrors(errors, data);
        addResponseRangeError(errors, data);

        String code = data.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Question code is duplicated in the import file"));
        }
        if (importContext != null) {
            addContextIdErrors(errors, data, importContext);
        }
        return errors;
    }

    private void addRequiredFieldErrors(List<Map<String, String>> errors, Map<String, String> data) {
        addMissingError(errors, data, "type", "Question type must not be blank");
        addMissingError(errors, data, "questionText", "Question text must not be blank");
        addMissingError(errors, data, "preparationTimeSeconds", "Preparation time must not be blank");
        addMissingError(errors, data, "minResponseSeconds", "Minimum response time must not be blank");
        addMissingError(errors, data, "maxResponseSeconds", "Maximum response time must not be blank");
    }

    private void addFieldFormatErrors(List<Map<String, String>> errors, Map<String, String> data) {
        validateEnumField(errors, data.get("type"), "type", QuestionType.class, "Question type is invalid");
        validateEnumField(errors, data.get("sharing"), "sharing", QuestionSharing.class, "Sharing mode is invalid");
        validateNumberField(errors, data.get("preparationTimeSeconds"), "preparationTimeSeconds", "Preparation time must be an integer >= 0");
        validateNumberField(errors, data.get("minResponseSeconds"), "minResponseSeconds", "Minimum response time must be an integer >= 0");
        validateNumberField(errors, data.get("maxResponseSeconds"), "maxResponseSeconds", "Maximum response time must be an integer >= 0");
    }

    private void addResponseRangeError(List<Map<String, String>> errors, Map<String, String> data) {
        Integer minResponse = parseInteger(data.get("minResponseSeconds"));
        Integer maxResponse = parseInteger(data.get("maxResponseSeconds"));
        if (minResponse != null && maxResponse != null && minResponse > maxResponse) {
            errors.add(error("minResponseSeconds", "Minimum response time must not exceed maximum response time"));
        }
    }

    private void addContextIdErrors(List<Map<String, String>> errors, Map<String, String> data, ImportContext importContext) {
        if (!Objects.equals(data.get("questionBankId"), importContext.questionBank().getId().toString())) {
            errors.add(error("questionBankId", "Question bank of the row is invalid"));
        }
        if (!Objects.equals(data.get("questionTopicId"), importContext.questionTopic().getId().toString())) {
            errors.add(error("questionTopicId", "Question topic of the row is invalid"));
        }
    }

    private void createQuestion(Map<String, String> data, ImportContext importContext, UUID currentUserId) {
        Question savedQuestion = questionRepository.save(buildDraftQuestion(data, importContext));
        createAssetIfPresent(data, savedQuestion.getId());
        if (hasEvaluationGuide(data)) {
            questionEvaluationGuideRepository.save(QuestionEvaluationGuide.forQuestion(
                    savedQuestion.getId(),
                    data.get("evaluationExpectedContent"),
                    data.get("evaluationKeyPoints"),
                    data.get("evaluationAcceptableResponses"),
                    data.get("evaluationOffTopicExamples"),
                    data.get("evaluationScoringHints"),
                    data.get("evaluationCommonMistakes")));
        }
    }

    // The question is a DRAFT, not SUBMITTED_FOR_REVIEW: bulk-imported questions almost always still need
    // further edits -- typically uploading the real image/audio for the assets, because media files cannot
    // be carried in the Excel file. Sending them straight to the review queue would make reviewers
    // look at an unfinished question. The importer is createdBy, so they can bulk SUBMIT it
    // themselves once it is complete (QuestionStatusTransition: SUBMIT accepts DRAFT or
    // REVISION_REQUESTED).
    private Question buildDraftQuestion(Map<String, String> data, ImportContext importContext) {
        return Question.draft(
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
                data.get("sharing") == null ? QuestionSharing.PRIVATE : QuestionSharing.valueOf(data.get("sharing")));
    }

    /**
     * Creates the asset that goes with a question when the file declares an {@code assetType}.
     *
     * <p>It goes through the same {@link QuestionAssetContentValidator} as the manual flow: an image must have
     * a description, audio/video must have a transcript. The rule is not relaxed for import -- the AI only knows
     * an asset through those lines of text, so a bulk file that leaves them empty would produce a whole batch of
     * questions that the AI asks and grades in a generic way, and nobody would notice until grading is finished.
     */
    private void createAssetIfPresent(Map<String, String> data, UUID questionId) {
        String rawType = data.get("assetType");
        if (rawType == null || rawType.isBlank()) {
            return;
        }

        QuestionAssetType assetType = QuestionAssetType.valueOf(rawType.strip().toUpperCase(java.util.Locale.ROOT));
        String url = assetType == QuestionAssetType.TEXT_PASSAGE ? null : data.get("assetUrl");
        String transcript = QuestionAssetAnalysisRequestPublisher.supportsTranscript(assetType)
            ? data.get("assetTranscript")
            : null;
        String description = data.get("assetDescription");
        String altText = data.get("assetAltText");

        QuestionAssetContentValidator.validate(assetType, url, transcript, description, altText);

        questionAssetRepository.save(QuestionAsset.attachedTo(
                questionId, data.get("assetTitle"), parseAssetDurationSeconds(data), altText, assetType, url,
                transcript, description, 1));
    }

    private Integer parseAssetDurationSeconds(Map<String, String> data) {
        String rawDuration = data.get("assetDurationSeconds");
        if (rawDuration != null && !rawDuration.isBlank()) {
            return Integer.parseInt(rawDuration.strip());
        }
        return null;
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

    /** Unlike {@link #parseUuid}: a missing or malformed value returns null instead of throwing — in bulk
     *  mode "no pinned id" is normal, not corrupt data. */
    private UUID parseUuidOrNull(String value) {
        if (!isPresent(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private UUID parseUuid(String value, String message) {
        if (!isPresent(value)) {
            throw new IllegalArgumentException(message);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(message);
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

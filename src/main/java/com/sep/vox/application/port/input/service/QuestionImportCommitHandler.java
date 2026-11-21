package com.sep.vox.application.port.input.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.sep.vox.application.port.input.usecase.question.QuestionAssetAnalysisRequestPublisher;
import com.sep.vox.application.port.input.usecase.question.QuestionAssetContentValidator;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionAssetType;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionConfidentiality;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
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
    private final TransactionTemplate transactionTemplate;

    public QuestionImportCommitHandler(
            QuestionRepository questionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionAssetRepository questionAssetRepository,
            SchoolRepository schoolRepository,
            JsonSerializationPort jsonSerializationPort,
            PlatformTransactionManager transactionManager) {
        this.schoolRepository = schoolRepository;
        this.questionRepository = questionRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionAssetRepository = questionAssetRepository;
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

        // Luồng cũ chỉ áp dụng khi bước xem trước ghim ĐỦ CẢ HAI: cả tệp vào đúng một chủ đề.
        // Thiếu bất kỳ cái nào -> đi đường giải theo mã, ở đó ngân hàng và chủ đề được giải ĐỘC LẬP
        // (cái nào đã ghim thì dùng luôn, cái nào chưa thì lấy mã trong file). Nhờ vậy phủ được cả
        // ba tổ hợp còn lại, không riêng "để trống cả hai".
        //
        // Xét cả hai chứ không mỗi questionBankId: ghim ngân hàng mà bỏ trống chủ đề sẽ rơi vào
        // nhánh cũ rồi ném ngay ở parseUuid(questionTopicId).
        Map<String, String> firstRowRaw = jsonSerializationPort.toStringMap(rows.get(0).getRawDataJson());
        boolean bulkMode = !isPresent(firstRowRaw.get("questionBankId"))
            || !isPresent(firstRowRaw.get("questionTopicId"));
        ImportContext sharedContext = bulkMode ? null : resolveContext(rows.get(0));
        BulkScope bulkScope = bulkMode ? buildBulkScope(session) : null;
        Map<String, String> confirmedMapping = jsonSerializationPort.toStringMap(session.getConfirmedMappingJson());
        long createdRows = 0L;
        long skippedRows = 0L;
        long invalidRows = 0L;
        Set<String> seenCodes = new HashSet<String>();

        for (ImportRow row : rows) {
            Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
            Map<String, String> mappedData = mapRawData(rawData, confirmedMapping);
            Map<String, String> normalized = normalize(mappedData);
            row.setMappedDataJson(jsonSerializationPort.toJson(normalized));

            List<Map<String, String>> errors = validateRow(normalized, sharedContext, seenCodes);
            final ImportContext importContext = bulkMode
                ? resolveContextFromCodes(normalized, bulkScope, errors)
                : sharedContext;
            if (!errors.isEmpty() || importContext == null) {
                row.setErrorsJson(jsonSerializationPort.toJson(errors));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            if (!bulkMode && hasDifferentContext(normalized, importContext)) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(
                    error("questionTopicId", "dữ liệu row không thuộc cùng question bank và question topic với phiên import")
                )));
                row.setStatus(ImportRowStatus.INVALID);
                invalidRows++;
                continue;
            }

            String code = normalized.get("code");
            String questionText = normalized.get("questionText");
            if (isPresent(code) && questionRepository.existsByQuestionBankIdAndCode(importContext.questionBank().getId(), code)) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(
                    error("code", "Đã tồn tại câu hỏi cùng mã trong question bank này")
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
                    error("questionText", "Đã tồn tại câu hỏi cùng nội dung trong chủ đề này")
                )));
                row.setStatus(ImportRowStatus.SKIPPED);
                skippedRows++;
                continue;
            }

            try {
                transactionTemplate.executeWithoutResult(status ->
                    createQuestion(normalized, importContext, session.createdByUserId().orElseThrow())
                );
                row.setErrorsJson(null);
                row.setStatus(ImportRowStatus.IMPORTED);
                createdRows++;
            } catch (DataIntegrityViolationException | IllegalArgumentException exception) {
                row.setErrorsJson(jsonSerializationPort.toJson(List.of(
                    error("questionText", "Không thể tạo câu hỏi từ dữ liệu import")
                )));
                row.setStatus(ImportRowStatus.FAILED);
                invalidRows++;
            }
        }

        return new ImportCommitResult(createdRows, 0L, skippedRows, invalidRows);
    }

    private ImportContext resolveContext(ImportRow row) {
        Map<String, String> rawData = jsonSerializationPort.toStringMap(row.getRawDataJson());
        UUID questionBankId = parseUuid(rawData.get("questionBankId"), "Không tìm thấy question bank của phiên import");
        UUID questionTopicId = parseUuid(rawData.get("questionTopicId"), "Không tìm thấy question topic của phiên import");
        QuestionBank questionBank = questionBankRepository.findById(questionBankId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        QuestionTopic questionTopic = questionTopicRepository.findById(questionTopicId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ đề câu hỏi"));
        if (!questionTopic.getQuestionBankId().equals(questionBank.getId())) {
            throw new IllegalStateException("Chủ đề không thuộc ngân hàng câu hỏi đã chọn");
        }
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            throw new IllegalStateException("Chỉ được import câu hỏi vào chủ đề đã PUBLISHED");
        }
        return new ImportContext(questionBank, questionTopic);
    }

    /**
     * Tìm ngân hàng + chủ đề đích của MỘT dòng theo mã ghi trong file (chế độ hàng loạt).
     *
     * <p><b>Phạm vi trường không lấy từ file.</b> {@code session.schoolId} đã chốt ở bước xem
     * trước theo người đăng nhập: có giá trị nghĩa là phiên của một trường (chỉ tìm trong ngân
     * hàng SCHOOL của trường đó), {@code null} nghĩa là phiên của quản trị hệ thống (ngân hàng
     * SYSTEM). Cột {@code schoolCode} trong file chỉ dùng để ĐỐI CHIẾU — khớp thì đi tiếp, lệch
     * thì báo lỗi dòng. Nếu để nó quyết định phạm vi thì bất kỳ ai cũng gõ được mã trường khác
     * rồi đẩy câu hỏi vào ngân hàng của trường đó.
     *
     * <p>Trả về {@code null} kèm lỗi đã ghi vào {@code errors} khi không giải được.
     */
    private ImportContext resolveContextFromCodes(
            Map<String, String> data,
            BulkScope scope,
            List<Map<String, String>> errors) {
        String bankCode = data.get("questionBankCode");
        String topicCode = data.get("questionTopicCode");

        // Ngân hàng và chủ đề giải ĐỘC LẬP nhau: người dùng chọn sẵn cái nào thì cái đó được ghim,
        // cái còn lại lấy theo mã trong file. Nhờ vậy "chọn ngân hàng, để trống chủ đề" chỉ cần cột
        // Mã chủ đề, không phải khai lại cả mã ngân hàng vừa chọn trên màn hình.
        UUID pinnedBankId = parseUuidOrNull(data.get("questionBankId"));
        UUID pinnedTopicId = parseUuidOrNull(data.get("questionTopicId"));

        if (pinnedBankId == null && !isPresent(bankCode)) {
            errors.add(error("questionBankCode", "Thiếu mã ngân hàng câu hỏi"));
        }
        if (pinnedTopicId == null && !isPresent(topicCode)) {
            errors.add(error("questionTopicCode", "Thiếu mã chủ đề câu hỏi"));
        }

        String schoolCode = data.get("schoolCode");
        if (isPresent(schoolCode) && !schoolCode.equalsIgnoreCase(scope.schoolCode())) {
            errors.add(error(
                "schoolCode",
                "Mã trường '" + schoolCode + "' không khớp phạm vi của phiên import"
                    + (scope.schoolCode() == null ? " (ngân hàng hệ thống)" : " ('" + scope.schoolCode() + "')")
            ));
        }
        if (!errors.isEmpty()) {
            return null;
        }

        QuestionBank questionBank = pinnedBankId != null
            ? questionBankRepository.findById(pinnedBankId).orElse(null)
            : scope.banksByCode().get(normalizeLookupKey(bankCode));
        if (questionBank == null) {
            errors.add(error(
                "questionBankCode",
                pinnedBankId != null
                    ? "Không tìm thấy ngân hàng câu hỏi đã chọn ở màn nhập"
                    : "Không tìm thấy ngân hàng câu hỏi có mã '" + bankCode + "'"));
            return null;
        }

        if (pinnedTopicId != null) {
            QuestionTopic pinnedTopic = questionTopicRepository.findById(pinnedTopicId).orElse(null);
            if (pinnedTopic == null || !pinnedTopic.getQuestionBankId().equals(questionBank.getId())) {
                errors.add(error(
                    "questionTopicCode",
                    "Chủ đề đã chọn không thuộc ngân hàng '" + questionBank.getCode() + "'"));
                return null;
            }
            if (pinnedTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
                errors.add(error(
                    "questionTopicCode",
                    "Chủ đề đã chọn đang ở trạng thái " + pinnedTopic.getStatus()
                        + ". Chỉ import được vào chủ đề đã PUBLISHED."));
                return null;
            }
            return new ImportContext(questionBank, pinnedTopic);
        }

        QuestionTopic questionTopic = scope.topicsByBankAndCode()
            .computeIfAbsent(questionBank.getId(), bankId -> {
                Map<String, QuestionTopic> byCode = new HashMap<String, QuestionTopic>();
                for (QuestionTopic topic : questionTopicRepository.findByQuestionBankId(bankId)) {
                    byCode.putIfAbsent(normalizeLookupKey(topic.getCode()), topic);
                }
                return byCode;
            })
            .get(normalizeLookupKey(topicCode));
        if (questionTopic == null) {
            errors.add(error(
                "questionTopicCode",
                "Không tìm thấy chủ đề có mã '" + topicCode + "' trong ngân hàng '" + bankCode + "'"
            ));
            return null;
        }
        if (questionTopic.getStatus() != QuestionTopicStatus.PUBLISHED) {
            errors.add(error(
                "questionTopicCode",
                "Chủ đề '" + topicCode + "' đang ở trạng thái " + questionTopic.getStatus()
                    + ". Chỉ import được vào chủ đề đã PUBLISHED."
            ));
            return null;
        }
        return new ImportContext(questionBank, questionTopic);
    }

    /**
     * Phạm vi tra cứu của cả phiên import hàng loạt, dựng MỘT lần rồi dùng lại cho mọi dòng.
     * Chủ đề nạp lười theo từng ngân hàng thật sự xuất hiện trong file, không nạp trước tất cả.
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
     * Chép sang map đã chuẩn hoá. Đây là danh sách viết TAY, và mọi trường không có tên ở đây đều
     * bị bỏ im lặng dù đã nằm trong {@link #SUPPORTED_FIELDS} và đã được ánh xạ đúng cột — thêm
     * trường mới mà quên chỗ này là dữ liệu biến mất không một lời báo.
     *
     * <p>Đã xảy ra đúng vậy hai lần: ba cột mã đích (chế độ hàng loạt luôn báo "Thiếu mã ngân hàng
     * câu hỏi" dù file có ghi) và bảy cột tài nguyên (asset không bao giờ được tạo, vì
     * {@code createAssetIfPresent} đọc {@code assetType} từ chính map này).
     */
    private Map<String, String> normalize(Map<String, String> mappedData) {
        Map<String, String> normalized = new LinkedHashMap<String, String>();
        normalized.put("questionBankId", trimOrNull(mappedData.get("questionBankId")));
        normalized.put("questionTopicId", trimOrNull(mappedData.get("questionTopicId")));
        // Mã đích của từng dòng ở chế độ hàng loạt. normalizeCode viết hoa -- khớp với cách tra cứu
        // ngân hàng/chủ đề theo mã, và schoolCode thì đối chiếu bằng equalsIgnoreCase nên vô hại.
        normalized.put("schoolCode", StringNormalization.normalizeCode(mappedData.get("schoolCode")));
        normalized.put("questionBankCode", StringNormalization.normalizeCode(mappedData.get("questionBankCode")));
        normalized.put("questionTopicCode", StringNormalization.normalizeCode(mappedData.get("questionTopicCode")));
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

        // Tài nguyên. Chỉ trimOrNull cho url, bản chép lời và mô tả -- KHÔNG trimAndCollapseSpaces:
        // url phân biệt hoa thường và khoảng trắng không được đụng tới, còn bản chép lời/mô tả có
        // ngắt đoạn (\n\n) mà gộp khoảng trắng sẽ dính liền thành một khối. Tiêu đề và văn bản thay
        // thế là nhãn ngắn nên gộp được.
        normalized.put("assetType", StringNormalization.normalizeCode(mappedData.get("assetType")));
        normalized.put("assetUrl", trimOrNull(mappedData.get("assetUrl")));
        normalized.put("assetTitle", StringNormalization.trimAndCollapseSpaces(mappedData.get("assetTitle")));
        normalized.put("assetAltText", StringNormalization.trimAndCollapseSpaces(mappedData.get("assetAltText")));
        normalized.put("assetTranscript", trimOrNull(mappedData.get("assetTranscript")));
        normalized.put("assetDescription", trimOrNull(mappedData.get("assetDescription")));
        normalized.put("assetDurationSeconds", trimOrNull(mappedData.get("assetDurationSeconds")));
        return normalized;
    }

    /**
     * @param importContext {@code null} ở chế độ hàng loạt — lúc đó đích của dòng chưa xác định
     *                      và được giải riêng bằng mã, nên phần đối chiếu id ở cuối hàm bỏ qua.
     */
    private List<Map<String, String>> validateRow(
            Map<String, String> data,
            ImportContext importContext,
            Set<String> seenCodes) {
        List<Map<String, String>> errors = new ArrayList<Map<String, String>>();

        addMissingError(errors, data, "type", "Loại câu hỏi không được để trống");
        addMissingError(errors, data, "questionText", "Nội dung câu hỏi không được để trống");
        addMissingError(errors, data, "preparationTimeSeconds", "Thời gian chuẩn bị không được để trống");
        addMissingError(errors, data, "minResponseSeconds", "Thời gian trả lời tối thiểu không được để trống");
        addMissingError(errors, data, "maxResponseSeconds", "Thời gian trả lời tối đa không được để trống");

        validateEnumField(errors, data.get("type"), "type", QuestionType.class, "Loại câu hỏi không hợp lệ");
        validateEnumField(errors, data.get("sharing"), "sharing", QuestionSharing.class, "Chế độ chia sẻ không hợp lệ");
        validateNumberField(errors, data.get("preparationTimeSeconds"), "preparationTimeSeconds", "Thời gian chuẩn bị phải là số nguyên >= 0");
        validateNumberField(errors, data.get("minResponseSeconds"), "minResponseSeconds", "Thời gian trả lời tối thiểu phải là số nguyên >= 0");
        validateNumberField(errors, data.get("maxResponseSeconds"), "maxResponseSeconds", "Thời gian trả lời tối đa phải là số nguyên >= 0");

        Integer minResponse = parseInteger(data.get("minResponseSeconds"));
        Integer maxResponse = parseInteger(data.get("maxResponseSeconds"));
        if (minResponse != null && maxResponse != null && minResponse > maxResponse) {
            errors.add(error("minResponseSeconds", "Thời gian trả lời tối thiểu không được lớn hơn thời gian trả lời tối đa"));
        }

        String code = data.get("code");
        if (isPresent(code) && !seenCodes.add(code)) {
            errors.add(error("code", "Mã câu hỏi bị trùng trong file import"));
        }

        if (importContext == null) {
            return errors;
        }
        if (!Objects.equals(data.get("questionBankId"), importContext.questionBank().getId().toString())) {
            errors.add(error("questionBankId", "Question bank của row không hợp lệ"));
        }
        if (!Objects.equals(data.get("questionTopicId"), importContext.questionTopic().getId().toString())) {
            errors.add(error("questionTopicId", "Question topic của row không hợp lệ"));
        }

        return errors;
    }

    private void createQuestion(Map<String, String> data, ImportContext importContext, UUID currentUserId) {
        Instant now = Instant.now();
        Question question = Question.builder()
                .questionBankId(importContext.questionBank().getId())
                .questionTopicId(importContext.questionTopic().getId())
                .code(questionCodeOf(data.get("code")))
                .instructionText(data.get("instructionText"))
                .questionText(data.get("questionText"))
                .promptText(data.get("promptText"))
                .preparationText(data.get("preparationText"))
                .type(QuestionType.valueOf(data.get("type")))
                .preparationTimeSeconds(Integer.parseInt(data.get("preparationTimeSeconds")))
                .minResponseSeconds(Integer.parseInt(data.get("minResponseSeconds")))
                .maxResponseSeconds(Integer.parseInt(data.get("maxResponseSeconds")))
                .sharing(data.get("sharing") == null ? QuestionSharing.PRIVATE : QuestionSharing.valueOf(data.get("sharing")))
                .sourceQuestionId(null)
                .locked(false)
                .confidentiality(QuestionConfidentiality.OPEN)
                .securePoolId(null)
                // DRAFT, not SUBMITTED_FOR_REVIEW: bulk-imported questions almost always still need further
                // edits -- typically uploading the real image/audio for the assets, because media files cannot
                // be carried in the Excel file. Sending them straight to the review queue would make reviewers
                // look at an unfinished question. The importer is createdBy, so they can bulk SUBMIT it
                // themselves once it is complete (QuestionStatusTransition: SUBMIT accepts DRAFT or
                // REVISION_REQUESTED).
                .status(QuestionStatus.DRAFT)
                .build();
        Question savedQuestion = questionRepository.save(question);
        createAssetIfPresent(data, savedQuestion.getId());
        if (hasEvaluationGuide(data)) {
            questionEvaluationGuideRepository.save(QuestionEvaluationGuide.builder()
                    .questionId(savedQuestion.getId())
                    .expectedContent(data.get("evaluationExpectedContent"))
                    .keyPoints(data.get("evaluationKeyPoints"))
                    .acceptableResponses(data.get("evaluationAcceptableResponses"))
                    .offTopicExamples(data.get("evaluationOffTopicExamples"))
                    .scoringHints(data.get("evaluationScoringHints"))
                    .commonMistakes(data.get("evaluationCommonMistakes"))
                    .build());
        }
    }

    /**
     * Tạo tài nguyên đi kèm câu hỏi khi file có khai {@code assetType}.
     *
     * <p>Đi qua đúng {@link QuestionAssetContentValidator} như luồng nhập tay: ảnh phải có mô tả,
     * audio/video phải có bản chép lời. Không nới lỏng cho import — AI chỉ biết tài nguyên qua
     * mấy dòng chữ đó, nên một file nhập hàng loạt mà bỏ trống chúng sẽ sinh ra cả loạt câu hỏi
     * mà AI hỏi và chấm chung chung, không ai biết cho tới lúc chấm xong.
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

        Integer durationSeconds = null;
        String rawDuration = data.get("assetDurationSeconds");
        if (rawDuration != null && !rawDuration.isBlank()) {
            durationSeconds = Integer.parseInt(rawDuration.strip());
        }

        questionAssetRepository.save(QuestionAsset.builder()
                .questionId(questionId)
                .title(data.get("assetTitle"))
                .durationSeconds(durationSeconds)
                .altText(altText)
                .type(assetType)
                .url(url)
                .transcript(transcript)
                .description(description)
                .order(1)
                .build());
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

    /** Khác {@link #parseUuid}: thiếu hoặc sai định dạng thì trả null chứ không ném — ở chế độ hàng
     *  loạt việc "không có id ghim" là bình thường, không phải hỏng dữ liệu. */
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

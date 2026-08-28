package com.sep.vox.application.port.input.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.dto.QuestionDto;
import com.sep.vox.domain.mapper.QuestionDtoMapper;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.question.QuestionType;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionEvaluationGuideRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class QuestionSpreadsheetService {

    private static final int EXPORT_PAGE_SIZE = 500;

    private static final List<String> IMPORT_HEADERS = List.of(
        "Mã câu hỏi",
        "Loại câu hỏi",
        "Nội dung câu hỏi",
        "Hướng dẫn",
        "Gợi ý",
        "Văn bản chuẩn bị",
        "Thời gian chuẩn bị",
        "Thời gian trả lời tối thiểu",
        "Thời gian trả lời tối đa",
        "Chia sẻ",
        "Nội dung mong đợi",
        "Ý chính",
        "Câu trả lời chấp nhận",
        "Ví dụ lạc đề",
        "Gợi ý chấm điểm",
        "Lỗi thường gặp"
    );

    private static final int TYPE_COLUMN_INDEX = 1;
    private static final int SHARING_COLUMN_INDEX = 9;
    private static final int VALIDATION_LAST_ROW = 1000;

    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public QuestionSpreadsheetService(
            QuestionRepository questionRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository) {
        this.questionRepository = questionRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
    }

    public byte[] exportQuestions(
            UUID questionBankId,
            UUID questionTopicId,
            String topicName,
            QuestionStatus status,
            QuestionType type,
            QuestionSharing sharing,
            String scope,
            String keyword) {
        var access = resolveAccess();
        var questions = new ArrayList<QuestionDto>();
        // findAccessible là 1-based (QuestionRepositoryImpl trừ 1 trước khi dựng PageRequest), nên
        // bắt đầu từ 1 chứ KHÔNG phải 0 -- số 0 thành -1 và Spring Data ném "Page index must not be
        // less than zero" ngay vòng lặp đầu, tức xuất Excel hỏng hoàn toàn.
        var page = 1;
        while (true) {
            var result = questionRepository.findAccessible(
                access.currentUserId(),
                access.currentSchoolId(),
                access.systemAdmin(),
                access.schoolAdmin(),
                questionBankId,
                questionTopicId,
                topicName,
                status,
                type,
                sharing,
                // Xuất Excel không lọc theo loại tài nguyên -- màn xuất không có ô đó.
                null,
                scope,
                keyword,
                page,
                EXPORT_PAGE_SIZE
            );
            questions.addAll(result.content().stream()
                .map(QuestionDtoMapper::toQuestionDto)
                .toList());
            page++;
            // `>` chứ không phải `>=`: đi kèm mốc bắt đầu 1-based ở trên. Giữ `>=` thì trang CUỐI
            // không bao giờ được đọc (totalPages=3: đọc 1, 2 rồi thoát ở 3>=3), mà mất dòng trong
            // file Excel thì không có gì báo -- lỗi lặng, tệ hơn cả ngoại lệ.
            if (page > result.totalPages()) {
                break;
            }
        }

        var workbook = new XSSFWorkbook();
        try (workbook) {
            var sheet = workbook.createSheet("Questions");
            writeHeaders(sheet, fullExportHeaders());
            var bankCache = new LinkedHashMap<UUID, String>();
            var topicCache = new LinkedHashMap<UUID, String>();
            var userCache = new LinkedHashMap<UUID, String>();

            var rowIndex = 1;
            for (var question : questions) {
                var evaluationGuide = questionEvaluationGuideRepository.findByQuestionId(question.id()).orElse(null);
                var row = sheet.createRow(rowIndex++);
                writeRow(row, rowValues(
                    question.code(),
                    question.type(),
                    question.questionText(),
                    question.instructionText(),
                    question.promptText(),
                    question.preparationText(),
                    String.valueOf(question.preparationTimeSeconds()),
                    String.valueOf(question.minResponseSeconds()),
                    String.valueOf(question.maxResponseSeconds()),
                    question.sharing(),
                    evaluationGuide == null ? null : evaluationGuide.getExpectedContent(),
                    evaluationGuide == null ? null : evaluationGuide.getKeyPoints(),
                    evaluationGuide == null ? null : evaluationGuide.getAcceptableResponses(),
                    evaluationGuide == null ? null : evaluationGuide.getOffTopicExamples(),
                    evaluationGuide == null ? null : evaluationGuide.getScoringHints(),
                    evaluationGuide == null ? null : evaluationGuide.getCommonMistakes(),
                    bankCodeOf(question.questionBankId(), bankCache),
                    topicNameOf(question.questionTopicId(), topicCache),
                    question.status(),
                    createdByNameOf(question.createdBy(), userCache),
                    question.createdAt()
                ));
            }
            autoSize(sheet, fullExportHeaders().size());
            return toBytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo file export câu hỏi", exception);
        }
    }

    public byte[] downloadTemplate(String type) {
        var workbook = new XSSFWorkbook();
        try (workbook) {
            var sheet = workbook.createSheet("Questions");
            writeHeaders(sheet, IMPORT_HEADERS);
            var normalizedType = normalizeQuestionType(type);

            writeRow(sheet.createRow(1), rowValues(
                "Q-SAMPLE-001",
                normalizedType,
                "Read the passage aloud clearly.",
                "Speak naturally and clearly.",
                "You can take a short breath before starting.",
                "A short text passage for reading aloud.",
                "15",
                "30",
                "60",
                "PRIVATE",
                "Pronounce clearly and read the whole passage.",
                "Pronunciation; fluency",
                "Reads all key sentences accurately.",
                "Stops too early or changes the text.",
                "Check pronunciation, pacing, and completeness.",
                "Skipping words or reading too fast."
            ));
            writeRow(sheet.createRow(2), rowValues(
                "",
                "SHORT_ANSWER",
                "What do you usually do on weekends?",
                "",
                "Mention one or two activities you often do.",
                "",
                "10",
                "20",
                "45",
                "SCHOOL_SHARED",
                "Answer directly and give a simple explanation.",
                "Direct answer; simple explanation",
                "I usually visit my grandparents and study English.",
                "",
                "Reward clear and relevant responses.",
                "Answering too vaguely."
            ));
            writeRow(sheet.createRow(3), rowValues(
                "",
                "OPINION",
                "Do you think students should use mobile phones in class?",
                "Give your opinion and support it with reasons.",
                "",
                "",
                "20",
                "40",
                "90",
                "PRIVATE",
                "State a clear opinion and provide supporting reasons.",
                "Clear opinion; reasons",
                "I think phones should only be used for learning tasks.",
                "Talking off topic about social media trends.",
                "Look for relevance and reasoning.",
                "No opinion stated."
            ));
            addListValidation(sheet, TYPE_COLUMN_INDEX, Arrays.stream(QuestionType.values()).map(e -> e.name()).toArray(String[]::new));
            addListValidation(sheet, SHARING_COLUMN_INDEX, Arrays.stream(QuestionSharing.values()).map(e -> e.name()).toArray(String[]::new));
            autoSize(sheet, IMPORT_HEADERS.size());
            return toBytes(workbook);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo file template import câu hỏi", exception);
        }
    }

    public String exportFileName() {
        return "questions-export-" + Instant.now().atZone(DateMapper.DEFAULT_INPUT_ZONE).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
    }

    public String templateFileName() {
        return "question-import-template.xlsx";
    }

    private AccessContext resolveAccess() {
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        var systemAdmin = userContextPort.isSystemAdmin();
        var currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        var schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return new AccessContext(currentUserId, currentSchoolId, systemAdmin, schoolAdmin);
    }

    private void writeHeaders(Sheet sheet, List<String> headers) {
        var row = sheet.createRow(0);
        writeRow(row, headers);
    }

    private void writeRow(org.apache.poi.ss.usermodel.Row row, List<String> values) {
        for (var index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index) == null ? "" : values.get(index));
        }
    }

    private void addListValidation(Sheet sheet, int columnIndex, String[] values) {
        var helper = sheet.getDataValidationHelper();
        var addressList = new CellRangeAddressList(1, VALIDATION_LAST_ROW, columnIndex, columnIndex);
        var constraint = helper.createExplicitListConstraint(values);
        var validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (var index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        try (var outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<String> fullExportHeaders() {
        var headers = new ArrayList<String>(IMPORT_HEADERS);
        headers.add("Mã ngân hàng");
        headers.add("Chủ đề");
        headers.add("Trạng thái");
        headers.add("Người tạo");
        headers.add("Ngày tạo");
        return headers;
    }

    private List<String> rowValues(String... values) {
        return Arrays.asList(values);
    }

    private String bankCodeOf(UUID questionBankId, Map<UUID, String> cache) {
        return cache.computeIfAbsent(
            questionBankId,
            id -> questionBankRepository.findById(id).map(bank -> bank.getCode()).orElse("")
        );
    }

    private String topicNameOf(UUID questionTopicId, Map<UUID, String> cache) {
        return cache.computeIfAbsent(
            questionTopicId,
            id -> questionTopicRepository.findById(id).map(topic -> topic.getName()).orElse("")
        );
    }

    private String createdByNameOf(UUID createdBy, Map<UUID, String> cache) {
        return cache.computeIfAbsent(
            createdBy,
            id -> userRepository.findById(id).map(user -> user.getFullName().value()).orElse("")
        );
    }

    private String normalizeQuestionType(String type) {
        var normalized = StringNormalization.normalizeCode(type);
        if (normalized == null || normalized.isBlank()) {
            return "READ_ALOUD";
        }
        try {
            QuestionType.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            return "READ_ALOUD";
        }
    }

    private record AccessContext(UUID currentUserId, UUID currentSchoolId, boolean systemAdmin, boolean schoolAdmin) {
    }
}

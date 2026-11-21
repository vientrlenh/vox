package com.sep.vox.application.port.input.service;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionEvaluationGuide;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sep.vox.application.common.DateMapper;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.SpreadsheetWriterPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.application.response.output.ColumnDropdown;
import com.sep.vox.application.response.output.SpreadsheetCell;
import com.sep.vox.application.response.output.SpreadsheetDocument;
import com.sep.vox.application.response.output.SpreadsheetOptions;
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
    private static final String SHEET_NAME = "Questions";

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
    private static final int VALIDATION_FIRST_ROW = 1;
    private static final int VALIDATION_LAST_ROW = 1000;

    private final QuestionRepository questionRepository;
    private final QuestionEvaluationGuideRepository questionEvaluationGuideRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final UserRepository userRepository;
    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final SpreadsheetWriterPort spreadsheetWriterPort;

    public QuestionSpreadsheetService(
            QuestionRepository questionRepository,
            QuestionEvaluationGuideRepository questionEvaluationGuideRepository,
            QuestionBankRepository questionBankRepository,
            QuestionTopicRepository questionTopicRepository,
            UserRepository userRepository,
            UserContextPort userContextPort,
            SchoolUserRepository schoolUserRepository,
            UserRoleQueryRepository userRoleQueryRepository,
            SpreadsheetWriterPort spreadsheetWriterPort) {
        this.questionRepository = questionRepository;
        this.questionEvaluationGuideRepository = questionEvaluationGuideRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.userRepository = userRepository;
        this.userContextPort = userContextPort;
        this.schoolUserRepository = schoolUserRepository;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.spreadsheetWriterPort = spreadsheetWriterPort;
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
        AccessContext access = resolveAccess();
        ExportFilter filter = new ExportFilter(
            questionBankId, questionTopicId, topicName, status, type, sharing, scope, keyword);
        List<QuestionDto> questions = loadAllAccessibleQuestions(access, filter);
        return spreadsheetWriterPort.write(new SpreadsheetDocument(
            SHEET_NAME, fullExportHeaders(), exportRows(questions), List.of(), SpreadsheetOptions.plain()));
    }

    public byte[] downloadTemplate(QuestionType type) {
        List<List<String>> sampleRows = List.of(
            firstSampleRow((type == null ? QuestionType.READ_ALOUD : type).name()),
            secondSampleRow(),
            thirdSampleRow());
        List<ColumnDropdown> dropdowns = List.of(
            dropdownOf(TYPE_COLUMN_INDEX, QuestionType.values()),
            dropdownOf(SHARING_COLUMN_INDEX, QuestionSharing.values()));
        return spreadsheetWriterPort.write(new SpreadsheetDocument(
            SHEET_NAME, IMPORT_HEADERS, textRows(sampleRows), dropdowns, SpreadsheetOptions.plain()));
    }

    public String exportFileName() {
        return "questions-export-" + Instant.now().atZone(DateMapper.DEFAULT_INPUT_ZONE).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
    }

    public String templateFileName() {
        return "question-import-template.xlsx";
    }

    private AccessContext resolveAccess() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean systemAdmin = userContextPort.isSystemAdmin();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = !systemAdmin && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (!systemAdmin && currentSchoolId == null) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return new AccessContext(currentUserId, currentSchoolId, systemAdmin, schoolAdmin);
    }

    /**
     * {@code findAccessible} is 1-based (QuestionRepositoryImpl subtracts 1 before building the PageRequest), so paging
     * starts at 1, NOT 0: 0 becomes -1 and Spring Data throws "Page index must not be less than zero" in the first
     * iteration, which breaks the Excel export completely.
     *
     * <p>The loop continues while {@code page <= totalPages}, matching the 1-based start. Stopping at
     * {@code page >= totalPages} would never read the LAST page (totalPages=3: reads 1 and 2, then stops at 3>=3), and
     * a row missing from the Excel file is reported by nothing: a silent error, worse than an exception.
     */
    private List<QuestionDto> loadAllAccessibleQuestions(AccessContext access, ExportFilter filter) {
        List<QuestionDto> questions = new ArrayList<QuestionDto>();
        int page = 1;
        PageResult<Question> result;
        do {
            result = findAccessiblePage(access, filter, page);
            questions.addAll(result.content().stream().map(QuestionDtoMapper::toQuestionDto).toList());
            page++;
        } while (page <= result.totalPages());
        return questions;
    }

    private PageResult<Question> findAccessiblePage(AccessContext access, ExportFilter filter, int page) {
        return questionRepository.findAccessible(
            access.currentUserId(),
            access.currentSchoolId(),
            access.systemAdmin(),
            access.schoolAdmin(),
            filter.questionBankId(),
            filter.questionTopicId(),
            filter.topicName(),
            filter.status(),
            filter.type(),
            filter.sharing(),
            // The export screen has no asset type filter.
            null,
            filter.scope(),
            filter.keyword(),
            page,
            EXPORT_PAGE_SIZE
        );
    }

    private List<List<SpreadsheetCell>> exportRows(List<QuestionDto> questions) {
        Map<UUID, String> bankCache = new LinkedHashMap<UUID, String>();
        Map<UUID, String> topicCache = new LinkedHashMap<UUID, String>();
        Map<UUID, String> userCache = new LinkedHashMap<UUID, String>();
        List<List<String>> rows = new ArrayList<List<String>>();
        for (QuestionDto question : questions) {
            QuestionEvaluationGuide guide = questionEvaluationGuideRepository.findByQuestionId(question.id()).orElse(null);
            List<String> values = new ArrayList<String>(questionValues(question, guide));
            values.add(bankCodeOf(question.questionBankId(), bankCache));
            values.add(topicNameOf(question.questionTopicId(), topicCache));
            values.add(question.status());
            values.add(createdByNameOf(question.createdBy(), userCache));
            values.add(question.createdAt() == null ? null : question.createdAt().toString());
            rows.add(values);
        }
        return textRows(rows);
    }

    private List<String> questionValues(QuestionDto question, QuestionEvaluationGuide guide) {
        return rowValues(
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
            guide == null ? null : guide.getExpectedContent(),
            guide == null ? null : guide.getKeyPoints(),
            guide == null ? null : guide.getAcceptableResponses(),
            guide == null ? null : guide.getOffTopicExamples(),
            guide == null ? null : guide.getScoringHints(),
            guide == null ? null : guide.getCommonMistakes()
        );
    }

    private List<List<SpreadsheetCell>> textRows(List<List<String>> rows) {
        return rows.stream()
            .map(row -> row.stream().map(value -> SpreadsheetCell.text(value == null ? "" : value)).toList())
            .toList();
    }

    private ColumnDropdown dropdownOf(int columnIndex, Enum<?>[] values) {
        List<String> names = Arrays.stream(values).map(e -> e.name()).toList();
        return new ColumnDropdown(columnIndex, VALIDATION_FIRST_ROW, VALIDATION_LAST_ROW, names);
    }

    private List<String> firstSampleRow(String sampleQuestionType) {
        return rowValues(
            "Q-SAMPLE-001",
            sampleQuestionType,
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
        );
    }

    private List<String> secondSampleRow() {
        return rowValues(
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
        );
    }

    private List<String> thirdSampleRow() {
        return rowValues(
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
        );
    }

    private List<String> fullExportHeaders() {
        List<String> headers = new ArrayList<String>(IMPORT_HEADERS);
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

    private record ExportFilter(
            UUID questionBankId,
            UUID questionTopicId,
            String topicName,
            QuestionStatus status,
            QuestionType type,
            QuestionSharing sharing,
            String scope,
            String keyword) {
    }

    private record AccessContext(UUID currentUserId, UUID currentSchoolId, boolean systemAdmin, boolean schoolAdmin) {
    }
}

package com.sep.vox.infrastructure.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.output.FileProcessingPort;
import com.sep.vox.application.response.output.ParseImportFileResult;
import com.sep.vox.domain.model.importfile.ImportType;

@Service
public class FileProcessingService implements FileProcessingPort {

    private static final int SAMPLE_ROW_LIMIT = 10;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{Alnum}]+");

    @Override
    public ParseImportFileResult parse(UploadedFile file, ImportType type) {
        validate(file, type);

        String extension = extensionOf(file.fileName());
        validateMagicBytes(extension, file.content());
        ParsedImportTable table = switch (extension) {
            case "xlsx", "xls" -> parseExcel(file.fileName(), file.content());
            case "csv" -> parseCsv(file.fileName(), file.content());
            default -> throw new IllegalArgumentException("File format is not supported for parsing");
        };

        Map<String, String> suggestedMapping = suggestMapping(table.headers(), type);
        return new ParseImportFileResult(
                table.headers(),
                suggestedMapping,
                table.rows(),
                table.sampleRows(),
                table.totalRows()
        );
    }

    private void validate(UploadedFile file, ImportType type) {
        if (file == null) {
            throw new IllegalArgumentException("Import file must not be empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Import type must not be blank");
        }
        if (file.content() == null || file.content().length == 0 || file.size() <= 0) {
            throw new IllegalArgumentException("Import file must not be empty");
        }
    }

    private void validateMagicBytes(String extension, byte[] content) {
        if (content.length < 4) {
            return; // let the parser report the format error itself
        }
        rejectMismatchedMagic(extension, hasZipMagic(content), hasOle2Magic(content));
    }

    private boolean hasZipMagic(byte[] content) {
        return content[0] == 0x50 && content[1] == 0x4B
                && content[2] == 0x03 && content[3] == 0x04;
    }

    private boolean hasOle2Magic(byte[] content) {
        return content.length >= 8
                && (content[0] & 0xFF) == 0xD0 && (content[1] & 0xFF) == 0xCF
                && (content[2] & 0xFF) == 0x11 && (content[3] & 0xFF) == 0xE0;
    }

    private void rejectMismatchedMagic(String extension, boolean isZipMagic, boolean isOle2Magic) {
        switch (extension) {
            case "xlsx" -> {
                if (!isZipMagic) {
                    throw new IllegalArgumentException("File content is not in xlsx format");
                }
            }
            case "xls" -> {
                if (!isOle2Magic) {
                    throw new IllegalArgumentException("File content is not in xls format");
                }
            }
            case "csv" -> {
                if (isZipMagic || isOle2Magic) {
                    throw new IllegalArgumentException("The CSV file contains invalid binary content");
                }
            }
            default -> {
                // any other format will be rejected at the parse switch step
            }
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private ParsedImportTable parseExcel(String fileName, byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            return readFirstSheet(workbook);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read data from file " + fileName, e);
        }
    }

    private ParsedImportTable readFirstSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new IllegalArgumentException("Import file is missing a header");
        }

        List<String> headers = getHeadersFromExcelHeader(headerRow, formatter);
        validateHeaders(headers);

        ImportRowCollector collector = new ImportRowCollector();
        for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                collectIfNotBlank(collector, readExcelRow(row, headers, formatter));
            }
        }

        return collector.toTable(headers);
    }

    private Map<String, String> readExcelRow(Row row, List<String> headers, DataFormatter formatter) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        for (int headerIndex = 0; headerIndex < headers.size(); headerIndex++) {
            values.put(headers.get(headerIndex), formatter.formatCellValue(row.getCell(headerIndex)));
        }
        return values;
    }

    private void collectIfNotBlank(ImportRowCollector collector, Map<String, String> values) {
        if (!isBlankRow(values)) {
            collector.add(values);
        }
    }

    private List<String> getHeadersFromExcelHeader(Row headerRow, DataFormatter formatter) {
        List<String> headers = new ArrayList<String>();
        for (short cellIndex = headerRow.getFirstCellNum(); cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            headers.add(formatter.formatCellValue(headerRow.getCell(cellIndex)).strip());
        }
        return headers;
    }

    private ParsedImportTable parseCsv(String fileName, byte[] content) {
        String csvContent = removeUtf8Bom(new String(content, StandardCharsets.UTF_8));
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(false)
                .get();
        try (
                CSVParser parser = format.parse(new StringReader(csvContent));
        ) {
            return readCsvRecords(parser);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read data from file " + fileName, e);
        }
    }

    private ParsedImportTable readCsvRecords(CSVParser parser) {
        List<String> headers = new ArrayList<String>(parser.getHeaderMap().keySet());
        validateHeaders(headers);

        ImportRowCollector collector = new ImportRowCollector();
        for (CSVRecord record : parser) {
            Map<String, String> values = new LinkedHashMap<String, String>();
            for (String header : headers) {
                values.put(header, record.get(header));
            }
            collectIfNotBlank(collector, values);
        }

        return collector.toTable(headers);
    }

    private void validateHeaders(List<String> headers) {
        if (headers.isEmpty() || headers.stream().allMatch(h -> h.isBlank())) {
            throw new IllegalArgumentException("Import file is missing a header");
        }
    }

    private boolean isBlankRow(Map<String, String> row) {
        return row.values().stream().allMatch(value -> value == null || value.isBlank());
    }

    private Map<String, String> suggestMapping(List<String> originalHeaders, ImportType type) {
        Map<String, String> lookup = aliasLookup(type);
        Map<String, String> mapping = new LinkedHashMap<String, String>();
        for (String originalHeader : originalHeaders) {
            String systemField = lookup.get(normalizeHeader(originalHeader));
            if (systemField != null) {
                mapping.put(originalHeader, systemField);
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(mapping));
    }

    private Map<String, String> aliasLookup(ImportType type) {
        Set<String> ambiguousAliases = new HashSet<String>();
        Map<String, String> lookup = new LinkedHashMap<String, String>();
        for (Map.Entry<String, List<String>> entry : aliasesBySystemField(type).entrySet()) {
            String systemField = entry.getKey();
            for (String alias : entry.getValue()) {
                String normalizedAlias = normalizeHeader(alias);
                String existing = lookup.putIfAbsent(normalizedAlias, systemField);
                if (existing != null && !existing.equals(systemField)) {
                    ambiguousAliases.add(normalizedAlias);
                }
            }
        }
        ambiguousAliases.forEach(lookup::remove);
        return lookup;
    }

    private Map<String, List<String>> aliasesBySystemField(ImportType type) {
        return switch (type) {
            case SCHOOL_CLASS -> schoolClassAliases();
            case USER -> userAliases();
            case SCHOOL_CLASS_USER -> schoolClassUserAliases();
            case SCHOOL_GRADE -> schoolGradeAliases();
            case SCHOOL_ROOM -> schoolRoomAliases();
            case SCHOOL_DIRECTORY -> schoolDirectoryAliases();
            case RUBRIC_VERSION -> rubricVersionAliases();
            case RUBRIC_CRITERION -> rubricCriterionAliases();
            case RUBRIC_RESULT_BAND -> rubricResultBandAliases();
            case ASSESSMENT_POLICY -> assessmentPolicyAliases();
            case QUESTION -> questionAliases();
            case QUESTION_BANK -> questionBankAliases();
            case QUESTION_TOPIC -> questionTopicAliases();
        };
    }

    private Map<String, List<String>> schoolClassAliases() {
        return Map.of(
            "languageCode", List.of("languageCode", "language", "language code", "ngôn ngữ", "ngon ngu", "mã ngôn ngữ", "ma ngon ngu"),
            "schoolGradeCode", List.of("schoolGradeCode", "grade", "grade code", "khối", "khoi", "mã khối", "ma khoi"),
            "targetSchoolLevelCode", List.of("targetSchoolLevelCode", "level", "level code", "trình độ", "trinh do", "mã trình độ", "ma trinh do"),
            "targetSchoolLevelVersion", List.of("targetSchoolLevelVersion", "version", "level version", "phiên bản", "phien ban"),
            "code", List.of("code", "classCode", "class code", "mã lớp", "ma lop"),
            "name", List.of("name", "className", "class name", "tên lớp", "ten lop"),
            "description", List.of("description", "note", "notes", "ghi chú", "ghi chu", "mô tả", "mo ta")
        );
    }

    private Map<String, List<String>> userAliases() {
        return Map.of(
            "roleCode", List.of("roleCode", "role", "userRole", "role code", "vai trò", "vai tro", "mã vai trò", "ma vai tro"),
            "fullName", List.of("fullName", "full name", "name", "họ tên", "ho ten", "họ và tên", "ho va ten"),
            "email", List.of("email", "mail", "email address", "địa chỉ email", "dia chi email"),
            "phone", List.of("phone", "phoneNumber", "phone number", "số điện thoại", "so dien thoai", "sdt"),
            "studentId", List.of("studentId", "student code", "student id", "mã học sinh", "ma hoc sinh"),
            "dateOfBirth", List.of("dateOfBirth", "date of birth", "birthday", "dob", "ngày sinh", "ngay sinh"),
            "startDate", List.of("startDate", "start date", "ngày bắt đầu", "ngay bat dau"),
            "endDate", List.of("endDate", "end date", "ngày kết thúc", "ngay ket thuc"),
            "address", List.of("address", "địa chỉ", "dia chi")
        );
    }

    private Map<String, List<String>> schoolClassUserAliases() {
        return Map.of(
            "email", List.of("email", "mail", "email address", "địa chỉ email", "dia chi email"),
            "classCode", List.of("classCode", "class code", "mã lớp", "ma lop", "lớp", "lop")
        );
    }

    private Map<String, List<String>> schoolGradeAliases() {
        return Map.of(
        "schoolGradeLevelCode", List.of("schoolGradeLevelCode", "gradeLevelCode", "grade level code", "mã khối", "ma khoi", "khối", "khoi"),
        "code", List.of("code", "gradeCode", "schoolGradeCode", "grade code", "mã năm học", "ma nam hoc", "mã", "ma"),
        "name", List.of("name", "gradeName", "grade name", "tên năm học", "ten nam hoc", "tên", "ten"),
        "description", List.of("description", "note", "notes", "ghi chú", "ghi chu", "mô tả", "mo ta"),
        "startDate", List.of("startDate", "start date", "ngày bắt đầu", "ngay bat dau"),
        "endDate", List.of("endDate", "end date", "ngày kết thúc", "ngay ket thuc")
        );
    }

    private Map<String, List<String>> schoolRoomAliases() {
        return Map.of(
        "code", List.of("code", "roomCode", "room code", "mã phòng", "ma phong", "mã"),
        "name", List.of("name", "roomName", "room name", "tên phòng", "ten phong", "tên"),
        "description", List.of("description", "note", "notes", "ghi chú", "ghi chu", "mô tả", "mo ta")
        );
    }

    private Map<String, List<String>> schoolDirectoryAliases() {
        return Map.of(
            "code", List.of("code", "schoolCode", "school code", "mã trường", "ma truong", "mã định danh", "ma dinh danh"),
            "name", List.of("name", "schoolName", "school name", "tên trường", "ten truong"),
            "provinceCode", List.of("provinceCode", "province code", "mã tỉnh", "ma tinh"),
            "provinceName", List.of("provinceName", "province", "province name", "tỉnh", "tinh", "tỉnh thành", "tinh thanh"),
            "districtName", List.of("districtName", "district", "district name", "quận huyện", "quan huyen", "huyện", "huyen"),
            "domain", List.of("domain", "tên miền", "ten mien", "email domain", "school domain"),
            "address", List.of("address", "địa chỉ", "dia chi")
        );
    }

    private Map<String, List<String>> rubricVersionAliases() {
        return Map.of(
            "version", List.of("version", "version number", "phiên bản", "phien ban", "số version","STT Version"),
            "name", List.of("name", "version name", "tên phiên bản", "ten phien ban", "tên"),
            "description", List.of("description", "note", "notes", "mô tả", "mo ta", "ghi chú", "ghi chu"),
            "scoringScaleMin", List.of("scoringScaleMin", "min score", "điểm sàn", "diem san", "điểm tối thiểu"),
            "scoringScaleMax", List.of("scoringScaleMax", "max score", "điểm trần", "diem tran", "điểm tối đa"),
            "totalScoreMethod", List.of("totalScoreMethod", "method", "phương pháp", "cách tính điểm", "phương pháp tính điểm tổng"),
            "effectiveFrom", List.of("effectiveFrom", "from date", "ngày bắt đầu", "ngày áp dụng", "ngay bat dau"),
            "effectiveTo", List.of("effectiveTo", "to date", "ngày kết thúc", "ngày hết hạn", "ngay ket thuc")
        );
    }

    private Map<String, List<String>> rubricCriterionAliases() {
        return Map.of(
            "code", List.of("code", "mã tiêu chí", "ma tieu chi", "criterion code", "mã"),
            "name", List.of("name", "tên tiêu chí", "ten tieu chi", "criterion name", "tên"),
            "description", List.of("description", "mô tả", "mo ta", "ghi chú"),
            "frameworkCriterionCode", List.of("frameworkCriterionCode", "framework code", "mã khung tiêu chuẩn", "mã khung", "ma khung","khung tiêu chuẩn"),
            "weight", List.of("weight", "trọng số", "trong so", "tỉ lệ"),
            "examples", List.of("examples", "ví dụ", "vi du", "mẫu", "mẫu tiêu chí"),
            "minScore", List.of("minScore", "min score", "điểm sàn", "diem san", "điểm tối thiểu"),
            "maxScore", List.of("maxScore", "max score", "điểm trần", "diem tran", "điểm tối đa"),
            "isRequired", List.of("isRequired", "bắt buộc", "bat buoc", "required"),
            "order", List.of("order", "thứ tự", "thu tu", "số thứ tự")
        );
    }

    private Map<String, List<String>> rubricResultBandAliases() {
        return Map.of(
            "code", List.of("code", "mã xếp loại", "ma xep loai", "mã kết quả", "ma ket qua"),
            "name", List.of("name", "tên xếp loại", "ten xep loai", "tên kết quả", "ten ket qua"),
            "description", List.of("description", "mô tả", "mo ta", "ghi chú"),
            "scoreMin", List.of("scoreMin", "điểm tối thiểu", "diem toi thieu", "điểm sàn"),
            "scoreMax", List.of("scoreMax", "điểm tối đa", "diem toi da", "điểm trần"),
            "order", List.of("order", "thứ tự", "thu tu")
        );
    }

    private Map<String, List<String>> assessmentPolicyAliases() {
        return Map.ofEntries(
                // Assessment core
                Map.entry("frameworkVersion", List.of("frameworkVersion", "phiên bản khung", "phien ban khung", "Phiên bản khung năng lực", "mã phiên bản khung", "tên phiên bản khung")),
                Map.entry("rubricVersion", List.of("rubricVersion", "phiên bản rubric", "phien ban rubric", "mã phiên bản rubric", "tên phiên bản rubric")),
                Map.entry("language", List.of("language", "ngôn ngữ", "ngon ngu", "mã ngôn ngữ", "tên ngôn ngữ")),

                // Scope of application (the static Grade Level and the dynamic School Grade are told apart clearly)
                Map.entry("schoolGradeLevel", List.of("schoolGradeLevel", "khối", "khoi", "mã khối", "tên khối")),
                Map.entry("schoolGrade", List.of("schoolGrade", "khối năm học", "khoi nam hoc", "mã khối năm học", "tên khối năm học")),
                Map.entry("schoolClass", List.of("schoolClass", "lớp", "lop", "mã lớp", "tên lớp")),

                // Assessment parameters
                Map.entry("targetFrameworkBand", List.of("targetFrameworkBand", "band mục tiêu", "band muc tieu", "mã band mục tiêu", "tên band mục tiêu")),
                Map.entry("passingScore", List.of("passingScore", "điểm đạt", "diem dat")),
                Map.entry("strictness", List.of("strictness", "mức độ nghiêm ngặt", "muc do nghiem ngat")),
                Map.entry("effectiveFrom", List.of("effectiveFrom", "ngày bắt đầu", "ngay bat dau")),
                Map.entry("effectiveTo", List.of("effectiveTo", "ngày kết thúc", "ngay ket thuc"))
        );
    }

    private Map<String, List<String>> questionTargetAliases() {
        return Map.ofEntries(
            // The destination of each row, which lets ONE file spread questions across several banks/topics.
            // When left empty it falls back to the bank/topic chosen on the upload screen (the old flow).
            //
            // schoolCode is ONLY a cross-check, NOT a source of authority: permissions are derived from the
            // logged-in user. Letting a spreadsheet cell decide which school applies would let a teacher type
            // another school's code and push questions into that school's bank.
            Map.entry("schoolCode", List.of("schoolCode", "school code", "mã trường", "ma truong")),
            Map.entry("questionBankCode", List.of("questionBankCode", "bank code", "mã ngân hàng", "ma ngan hang", "mã ngân hàng câu hỏi")),
            Map.entry("questionTopicCode", List.of("questionTopicCode", "topic code", "mã chủ đề", "ma chu de"))
        );
    }

    private Map<String, List<String>> questionContentAliases() {
        return Map.ofEntries(
            Map.entry("code", List.of("code", "question code", "mã câu hỏi", "ma cau hoi")),
            Map.entry("type", List.of("type", "question type", "loại câu hỏi", "loai cau hoi")),
            Map.entry("questionText", List.of("questionText", "question text", "content", "nội dung câu hỏi", "noi dung cau hoi")),
            Map.entry("instructionText", List.of("instructionText", "instruction", "hướng dẫn", "huong dan")),
            Map.entry("promptText", List.of("promptText", "prompt", "gợi ý", "goi y")),
            Map.entry("preparationText", List.of("preparationText", "preparation text", "văn bản chuẩn bị", "van ban chuan bi")),
            Map.entry("preparationTimeSeconds", List.of("preparationTimeSeconds", "prep time", "thời gian chuẩn bị", "thoi gian chuan bi")),
            Map.entry("minResponseSeconds", List.of("minResponseSeconds", "min response", "thời gian trả lời tối thiểu", "thoi gian tra loi toi thieu")),
            Map.entry("maxResponseSeconds", List.of("maxResponseSeconds", "max response", "thời gian trả lời tối đa", "thoi gian tra loi toi da")),
            Map.entry("sharing", List.of("sharing", "chia sẻ", "chia se")),
            Map.entry("evaluationExpectedContent", List.of("expectedContent", "expected content", "nội dung mong đợi", "noi dung mong doi")),
            Map.entry("evaluationKeyPoints", List.of("keyPoints", "key points", "ý chính", "y chinh")),
            Map.entry("evaluationAcceptableResponses", List.of("acceptableResponses", "acceptable responses", "câu trả lời chấp nhận", "cau tra loi chap nhan")),
            Map.entry("evaluationOffTopicExamples", List.of("offTopicExamples", "off topic examples", "ví dụ lạc đề", "vi du lac de")),
            Map.entry("evaluationScoringHints", List.of("scoringHints", "scoring hints", "gợi ý chấm điểm", "goi y cham diem")),
            Map.entry("evaluationCommonMistakes", List.of("commonMistakes", "common mistakes", "lỗi thường gặp", "loi thuong gap"))
        );
    }

    private Map<String, List<String>> questionAssetAliases() {
        return Map.ofEntries(
            // The asset that comes with a question. An empty assetType means the question has no asset.
            // transcript/description is the ONLY thing the AI knows about an asset (it can neither see an
            // image nor hear a file), so they are required by exactly the rules of QuestionAssetContentValidator.
            Map.entry("assetType", List.of("assetType", "asset type", "loại tài nguyên", "loai tai nguyen")),
            Map.entry("assetUrl", List.of("assetUrl", "asset url", "đường dẫn tài nguyên", "duong dan tai nguyen")),
            Map.entry("assetTitle", List.of("assetTitle", "asset title", "tiêu đề tài nguyên", "tieu de tai nguyen")),
            Map.entry("assetAltText", List.of("assetAltText", "alt text", "văn bản thay thế", "van ban thay the")),
            Map.entry("assetTranscript", List.of("assetTranscript", "asset transcript", "bản chép lời", "ban chep loi")),
            Map.entry("assetDescription", List.of("assetDescription", "asset description", "mô tả tài nguyên", "mo ta tai nguyen")),
            Map.entry("assetDurationSeconds", List.of("assetDurationSeconds", "asset duration", "thời lượng tài nguyên", "thoi luong tai nguyen"))
        );
    }

    private Map<String, List<String>> questionAliases() {
        Map<String, List<String>> aliases = new LinkedHashMap<String, List<String>>();
        aliases.putAll(questionTargetAliases());
        aliases.putAll(questionContentAliases());
        aliases.putAll(questionAssetAliases());
        return Collections.unmodifiableMap(aliases);
    }

    private Map<String, List<String>> questionBankAliases() {
        return Map.ofEntries(
            Map.entry("code", List.of("code", "bank code", "mã ngân hàng", "ma ngan hang", "mã ngân hàng câu hỏi")),
            Map.entry("name", List.of("name", "bank name", "tên ngân hàng", "ten ngan hang", "tên ngân hàng câu hỏi")),
            Map.entry("description", List.of("description", "mô tả", "mo ta")),
            // A question bank must have a language (questionBank.languageId is not null), and each row may use a
            // different language, so it is read from the FILE instead of an upload parameter like questionBankId
            // in the question import flow.
            Map.entry("language", List.of("language", "ngôn ngữ", "ngon ngu", "mã ngôn ngữ", "ma ngon ngu"))
        );
    }

    private Map<String, List<String>> questionTopicAliases() {
        return Map.ofEntries(
            Map.entry("code", List.of("code", "topic code", "mã chủ đề", "ma chu de")),
            Map.entry("name", List.of("name", "topic name", "tên chủ đề", "ten chu de")),
            Map.entry("description", List.of("description", "mô tả", "mo ta"))
        );
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(header, Normalizer.Form.NFD)).replaceAll("");
        return NON_ALPHANUMERIC.matcher(withoutDiacritics.toLowerCase(Locale.ROOT).strip())
                .replaceAll(" ")
                .strip();
    }

    private String removeUtf8Bom(String content) {
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
    }

    private record ParsedImportTable(
            List<String> headers,
            List<Map<String, String>> rows,
            List<Map<String, String>> sampleRows,
            long totalRows
    ) {
    }

    private static final class ImportRowCollector {

        private final List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        private final List<Map<String, String>> sampleRows = new ArrayList<Map<String, String>>();
        private long totalRows = 0L;

        void add(Map<String, String> values) {
            totalRows++;
            rows.add(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
            if (sampleRows.size() < SAMPLE_ROW_LIMIT) {
                sampleRows.add(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
            }
        }

        ParsedImportTable toTable(List<String> headers) {
            return new ParsedImportTable(List.copyOf(headers), List.copyOf(rows), List.copyOf(sampleRows), totalRows);
        }
    }
}

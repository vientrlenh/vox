package com.sep.vox.infrastructure.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
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

        var extension = extensionOf(file.fileName());
        validateMagicBytes(extension, file.content());
        var table = switch (extension) {
            case "xlsx", "xls" -> parseExcel(file.fileName(), file.content());
            case "csv" -> parseCsv(file.fileName(), file.content());
            default -> throw new IllegalArgumentException("Định dạng file không được hỗ trợ để parse");
        };

        var suggestedMapping = suggestMapping(table.headers(), type);
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
            throw new IllegalArgumentException("File import không được để trống");
        }
        if (type == null) {
            throw new IllegalArgumentException("Loại import không được để trống");
        }
        if (file.content() == null || file.content().length == 0 || file.size() <= 0) {
            throw new IllegalArgumentException("File import không được để trống");
        }
    }

    private void validateMagicBytes(String extension, byte[] content) {
        if (content.length < 4) {
            return; // để parser tự báo lỗi định dạng
        }
        var isZipMagic = content[0] == 0x50 && content[1] == 0x4B
                && content[2] == 0x03 && content[3] == 0x04;
        var isOle2Magic = content.length >= 8
                && (content[0] & 0xFF) == 0xD0 && (content[1] & 0xFF) == 0xCF
                && (content[2] & 0xFF) == 0x11 && (content[3] & 0xFF) == 0xE0;
        switch (extension) {
            case "xlsx" -> {
                if (!isZipMagic) {
                    throw new IllegalArgumentException("Nội dung file không đúng định dạng xlsx");
                }
            }
            case "xls" -> {
                if (!isOle2Magic) {
                    throw new IllegalArgumentException("Nội dung file không đúng định dạng xls");
                }
            }
            case "csv" -> {
                if (isZipMagic || isOle2Magic) {
                    throw new IllegalArgumentException("File CSV chứa nội dung nhị phân không hợp lệ");
                }
            }
            default -> {
                // định dạng khác sẽ bị từ chối ở bước switch parse
            }
        }
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        var dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private ParsedImportTable parseExcel(String fileName, byte[] content) {
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            var sheet = workbook.getSheetAt(0);
            var formatter = new DataFormatter();
            var headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("File import thiếu header");
            }

            var headers = getHeadersFromExcelHeader(headerRow, formatter);
            validateHeaders(headers);

            var rows = new ArrayList<Map<String, String>>();
            var sampleRows = new ArrayList<Map<String, String>>();
            var totalRows = 0L;
            for (var rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                var row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                var values = new LinkedHashMap<String, String>();
                for (var headerIndex = 0; headerIndex < headers.size(); headerIndex++) {
                    values.put(headers.get(headerIndex), formatter.formatCellValue(row.getCell(headerIndex)));
                }
                if (isBlankRow(values)) {
                    continue;
                }
                totalRows++;
                rows.add(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
                if (sampleRows.size() < SAMPLE_ROW_LIMIT) {
                    sampleRows.add(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
                }
            }

            return new ParsedImportTable(List.copyOf(headers), List.copyOf(rows), List.copyOf(sampleRows), totalRows);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc dữ liệu từ file " + fileName, e);
        }
    }

    private List<String> getHeadersFromExcelHeader(Row headerRow, DataFormatter formatter) {
        var headers = new ArrayList<String>();
        for (var cellIndex = headerRow.getFirstCellNum(); cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            headers.add(formatter.formatCellValue(headerRow.getCell(cellIndex)).strip());
        }
        return headers;
    }

    private ParsedImportTable parseCsv(String fileName, byte[] content) {
        var csvContent = removeUtf8Bom(new String(content, StandardCharsets.UTF_8));
        var format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(false)
                .get();
        try (
                var parser = format.parse(new StringReader(csvContent));
        ) {
            var headers = new ArrayList<String>(parser.getHeaderMap().keySet());
            validateHeaders(headers);

            var rows = new ArrayList<Map<String, String>>();
            var sampleRows = new ArrayList<Map<String, String>>();
            var totalRows = 0L;
            for (var record : parser) {
                var values = new LinkedHashMap<String, String>();
                for (var header : headers) {
                    values.put(header, record.get(header));
                }
                if (isBlankRow(values)) {
                    continue;
                }
                totalRows++;
                rows.add(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
                if (sampleRows.size() < SAMPLE_ROW_LIMIT) {
                    sampleRows.add(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
                }
            }

            return new ParsedImportTable(List.copyOf(headers), List.copyOf(rows), List.copyOf(sampleRows), totalRows);
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc dữ liệu từ file " + fileName, e);
        }
    }

    private void validateHeaders(List<String> headers) {
        if (headers.isEmpty() || headers.stream().allMatch(h -> h.isBlank())) {
            throw new IllegalArgumentException("File import thiếu header");
        }
    }

    private boolean isBlankRow(Map<String, String> row) {
        return row.values().stream().allMatch(value -> value == null || value.isBlank());
    }

    private Map<String, String> suggestMapping(List<String> originalHeaders, ImportType type) {
        var lookup = aliasLookup(type);
        var mapping = new LinkedHashMap<String, String>();
        for (var originalHeader : originalHeaders) {
            var systemField = lookup.get(normalizeHeader(originalHeader));
            if (systemField != null) {
                mapping.put(originalHeader, systemField);
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(mapping));
    }

    private Map<String, String> aliasLookup(ImportType type) {
        var ambiguousAliases = new java.util.HashSet<String>();
        var lookup = new LinkedHashMap<String, String>();
        for (var entry : aliasesBySystemField(type).entrySet()) {
            var systemField = entry.getKey();
            for (var alias : entry.getValue()) {
                var normalizedAlias = normalizeHeader(alias);
                var existing = lookup.putIfAbsent(normalizedAlias, systemField);
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
            case SCHOOL_CLASS -> Map.of(
                    "languageCode", List.of("languageCode", "language", "language code", "ngôn ngữ", "ngon ngu", "mã ngôn ngữ", "ma ngon ngu"),
                    "schoolGradeCode", List.of("schoolGradeCode", "grade", "grade code", "khối", "khoi", "mã khối", "ma khoi"),
                    "targetSchoolLevelCode", List.of("targetSchoolLevelCode", "level", "level code", "trình độ", "trinh do", "mã trình độ", "ma trinh do"),
                    "targetSchoolLevelVersion", List.of("targetSchoolLevelVersion", "version", "level version", "phiên bản", "phien ban"),
                    "code", List.of("code", "classCode", "class code", "mã lớp", "ma lop"),
                    "name", List.of("name", "className", "class name", "tên lớp", "ten lop"),
                    "description", List.of("description", "note", "notes", "ghi chú", "ghi chu", "mô tả", "mo ta")
            );
            case USER -> Map.of(
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
            case SCHOOL_CLASS_USER -> Map.of(
                    "email", List.of("email", "mail", "email address", "địa chỉ email", "dia chi email"),
                    "classCode", List.of("classCode", "class code", "mã lớp", "ma lop", "lớp", "lop")
            );
            case SCHOOL_GRADE_LEVEL -> Map.of(
                "code", List.of("code", "gradeLevelCode", "grade level code", "mã khối", "ma khoi"),
                "name", List.of("name", "gradeLevelName", "grade level name", "tên khối", "ten khoi"),
                "order", List.of("order", "gradeLevelOrder", "thứ tự", "thu tu", "stt"),
                "description", List.of("description", "note", "notes", "ghi chú", "ghi chu", "mô tả", "mo ta")
            );
            case SCHOOL_GRADE -> Map.of(
                "schoolGradeLevelCode", List.of("schoolGradeLevelCode", "gradeLevelCode", "grade level code", "mã khối", "ma khoi", "khối", "khoi"),
                "code", List.of("code", "gradeCode", "schoolGradeCode", "grade code", "mã năm học", "ma nam hoc", "mã", "ma"),
                "name", List.of("name", "gradeName", "grade name", "tên năm học", "ten nam hoc", "tên", "ten"),
                "description", List.of("description", "note", "notes", "ghi chú", "ghi chu", "mô tả", "mo ta"),
                "startDate", List.of("startDate", "start date", "ngày bắt đầu", "ngay bat dau"),
                "endDate", List.of("endDate", "end date", "ngày kết thúc", "ngay ket thuc")
            );
            case SCHOOL_ROOM -> Map.of(
                "code", List.of("code", "roomCode", "room code", "mã phòng", "ma phong", "mã"),
                "name", List.of("name", "roomName", "room name", "tên phòng", "ten phong", "tên"),
                "description", List.of("description", "note", "notes", "ghi chú", "ghi chu", "mô tả", "mo ta")
            );

            case SCHOOL_DIRECTORY -> Map.of(
                    "code", List.of("code", "schoolCode", "school code", "mã trường", "ma truong", "mã định danh", "ma dinh danh"),
                    "name", List.of("name", "schoolName", "school name", "tên trường", "ten truong"),
                    "provinceCode", List.of("provinceCode", "province code", "mã tỉnh", "ma tinh"),
                    "provinceName", List.of("provinceName", "province", "province name", "tỉnh", "tinh", "tỉnh thành", "tinh thanh"),
                    "districtName", List.of("districtName", "district", "district name", "quận huyện", "quan huyen", "huyện", "huyen"),
                    "domain", List.of("domain", "tên miền", "ten mien", "email domain", "school domain"),
                    "address", List.of("address", "địa chỉ", "dia chi")
            );
            case RUBRIC_VERSION -> Map.of(
                    "version", List.of("version", "version number", "phiên bản", "phien ban", "số version"),
                    "name", List.of("name", "version name", "tên phiên bản", "ten phien ban", "tên"),
                    "description", List.of("description", "note", "notes", "mô tả", "mo ta", "ghi chú", "ghi chu"),
                    "scoringScaleMin", List.of("scoringScaleMin", "min score", "điểm sàn", "diem san", "điểm tối thiểu"),
                    "scoringScaleMax", List.of("scoringScaleMax", "max score", "điểm trần", "diem tran", "điểm tối đa"),
                    "totalScoreMethod", List.of("totalScoreMethod", "method", "phương pháp", "cách tính điểm", "phương pháp tính điểm tổng"),
                    "effectiveFrom", List.of("effectiveFrom", "from date", "ngày bắt đầu", "ngày áp dụng", "ngay bat dau"),
                    "effectiveTo", List.of("effectiveTo", "to date", "ngày kết thúc", "ngày hết hạn", "ngay ket thuc")
            );

            case RUBRIC_CRITERION -> Map.of(
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

            case RUBRIC_CRITERION_BAND -> Map.of(
                    "code", List.of("code", "mã mức độ", "ma muc do", "band code", "mã tiêu chí"),
                    "scoreMin", List.of("scoreMin", "score min", "điểm tối thiểu", "diem toi thieu", "điểm thấp nhất", "min"),
                    "scoreMax", List.of("scoreMax", "score max", "điểm tối đa", "diem toi da", "điểm cao nhất", "max")
            );

            case RUBRIC_RESULT_BAND -> Map.of(
                    "code", List.of("code", "mã xếp loại", "ma xep loai", "mã kết quả", "ma ket qua"),
                    "name", List.of("name", "tên xếp loại", "ten xep loai", "tên kết quả", "ten ket qua"),
                    "description", List.of("description", "mô tả", "mo ta", "ghi chú"),
                    "scoreMin", List.of("scoreMin", "điểm tối thiểu", "diem toi thieu", "điểm sàn"),
                    "scoreMax", List.of("scoreMax", "điểm tối đa", "diem toi da", "điểm trần"),
                    "order", List.of("order", "thứ tự", "thu tu")
            );
            case ASSESSMENT_POLICY -> Map.ofEntries(
                    // Lõi đánh giá
                    Map.entry("frameworkVersion", List.of("frameworkVersion", "phiên bản khung", "phien ban khung", "Phiên bản khung năng lực", "mã phiên bản khung", "tên phiên bản khung")),
                    Map.entry("rubricVersion", List.of("rubricVersion", "phiên bản rubric", "phien ban rubric", "mã phiên bản rubric", "tên phiên bản rubric")),
                    Map.entry("language", List.of("language", "ngôn ngữ", "ngon ngu", "mã ngôn ngữ", "tên ngôn ngữ")),

                    // Phạm vi áp dụng (Đã phân biệt rõ Khối tĩnh và Khối động)
                    Map.entry("schoolGradeLevel", List.of("schoolGradeLevel", "khối", "khoi", "mã khối", "tên khối")),
                    Map.entry("schoolGrade", List.of("schoolGrade", "khối năm học", "khoi nam hoc", "mã khối năm học", "tên khối năm học")),
                    Map.entry("schoolClass", List.of("schoolClass", "lớp", "lop", "mã lớp", "tên lớp")),

                    // Thông số đánh giá
                    Map.entry("targetFrameworkBand", List.of("targetFrameworkBand", "band mục tiêu", "band muc tieu", "mã band mục tiêu", "tên band mục tiêu")),
                    Map.entry("minimumFrameworkBand", List.of("minimumFrameworkBand", "band tối thiểu", "band toi thieu", "mã band tối thiểu", "tên band tối thiểu")),
                    Map.entry("passingScore", List.of("passingScore", "điểm đạt", "diem dat")),
                    Map.entry("strictness", List.of("strictness", "mức độ nghiêm ngặt", "muc do nghiem ngat")),
                    Map.entry("effectiveFrom", List.of("effectiveFrom", "ngày bắt đầu", "ngay bat dau")),
                    Map.entry("effectiveTo", List.of("effectiveTo", "ngày kết thúc", "ngay ket thuc"))
            );

            case QUESTION -> Map.ofEntries(
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
        };
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        var withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(header, Normalizer.Form.NFD)).replaceAll("");
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
}

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
        if (headers.isEmpty() || headers.stream().allMatch(String::isBlank)) {
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
            case SCHOOL_DIRECTORY -> Map.of(
                "code", List.of("code", "schoolCode", "school code", "mã trường", "ma truong", "mã định danh", "ma dinh danh"),
                "name", List.of("name", "schoolName", "school name", "tên trường", "ten truong"),
                "provinceCode", List.of("provinceCode", "province code", "mã tỉnh", "ma tinh"),
                "provinceName", List.of("provinceName", "province", "province name", "tỉnh", "tinh", "tỉnh thành", "tinh thanh"),
                "districtName", List.of("districtName", "district", "district name", "quận huyện", "quan huyen", "huyện", "huyen"),
                "domain", List.of("domain", "tên miền", "ten mien", "email domain", "school domain"),
                "address", List.of("address", "địa chỉ", "dia chi")
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
    ) {}
}

package com.sep.vox.interfaces.rest.mapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

import com.sep.vox.application.port.input.command.ImportSchoolClassRowCommand;
import com.sep.vox.application.port.input.command.ImportSchoolClassesCommand;

public final class ImportSchoolClassesCommandMapper {

    private static final String LANGUAGE_CODE = "languageCode";
    private static final String SCHOOL_GRADE_CODE = "schoolGradeCode";
    private static final String TARGET_SCHOOL_LEVEL_CODE = "targetSchoolLevelCode";
    private static final String TARGET_SCHOOL_LEVEL_VERSION = "targetSchoolLevelVersion";
    private static final String CODE = "code";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";

    private static final Set<String> REQUIRED_HEADERS = Set.of(
        LANGUAGE_CODE,
        SCHOOL_GRADE_CODE,
        TARGET_SCHOOL_LEVEL_CODE,
        TARGET_SCHOOL_LEVEL_VERSION,
        CODE,
        NAME
    );

    private ImportSchoolClassesCommandMapper() {
    }

    public static ImportSchoolClassesCommand fromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import không được để trống");
        }

        var extension = extensionOf(file.getOriginalFilename());
        try {
            return switch (extension) {
                case "csv" -> fromCsv(file);
                case "xlsx", "xls" -> fromExcel(file);
                default -> throw new IllegalArgumentException("Chỉ hỗ trợ file .csv, .xlsx, .xls");
            };
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc file import", e);
        }
    }

    private static ImportSchoolClassesCommand fromCsv(MultipartFile file) throws IOException {
        var content = removeUtf8Bom(new String(file.getBytes(), StandardCharsets.UTF_8));
        var format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(false)
            .get();
        try (var parser = format.parse(new StringReader(content))) {
            validateHeaders(parser.getHeaderMap());
            var rows = new ArrayList<ImportSchoolClassRowCommand>();
            for (var record : parser) {
                if (isBlankRow(
                    record.get(LANGUAGE_CODE),
                    record.get(SCHOOL_GRADE_CODE),
                    record.get(TARGET_SCHOOL_LEVEL_CODE),
                    record.get(TARGET_SCHOOL_LEVEL_VERSION),
                    record.get(CODE),
                    record.get(NAME),
                    optional(record.toMap(), DESCRIPTION)
                )) {
                    continue;
                }
                rows.add(new ImportSchoolClassRowCommand(
                    Math.toIntExact(record.getRecordNumber() + 1),
                    record.get(LANGUAGE_CODE),
                    record.get(SCHOOL_GRADE_CODE),
                    record.get(TARGET_SCHOOL_LEVEL_CODE),
                    record.get(TARGET_SCHOOL_LEVEL_VERSION),
                    record.get(CODE),
                    record.get(NAME),
                    optional(record.toMap(), DESCRIPTION)
                ));
            }
            return new ImportSchoolClassesCommand(List.copyOf(rows));
        }
    }

    private static ImportSchoolClassesCommand fromExcel(MultipartFile file) throws IOException {
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(file.getBytes()))) {
            var sheet = workbook.getSheetAt(0);
            var formatter = new DataFormatter();
            var headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("File import thiếu header");
            }

            var headerIndexes = headerIndexes(headerRow, formatter);
            validateHeaders(headerIndexes);
            var rows = new ArrayList<ImportSchoolClassRowCommand>();
            for (var rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                var row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                var languageCode = cell(row, headerIndexes, LANGUAGE_CODE, formatter);
                var schoolGradeCode = cell(row, headerIndexes, SCHOOL_GRADE_CODE, formatter);
                var targetSchoolLevelCode = cell(row, headerIndexes, TARGET_SCHOOL_LEVEL_CODE, formatter);
                var targetSchoolLevelVersion = cell(row, headerIndexes, TARGET_SCHOOL_LEVEL_VERSION, formatter);
                var code = cell(row, headerIndexes, CODE, formatter);
                var name = cell(row, headerIndexes, NAME, formatter);
                var description = cell(row, headerIndexes, DESCRIPTION, formatter);
                if (isBlankRow(languageCode, schoolGradeCode, targetSchoolLevelCode, targetSchoolLevelVersion, code, name, description)) {
                    continue;
                }
                rows.add(new ImportSchoolClassRowCommand(
                    rowIndex + 1,
                    languageCode,
                    schoolGradeCode,
                    targetSchoolLevelCode,
                    targetSchoolLevelVersion,
                    code,
                    name,
                    description
                ));
            }
            return new ImportSchoolClassesCommand(List.copyOf(rows));
        }
    }

    private static Map<String, Integer> headerIndexes(Row headerRow, DataFormatter formatter) {
        var headers = new java.util.HashMap<String, Integer>();
        for (var cellIndex = headerRow.getFirstCellNum(); cellIndex < headerRow.getLastCellNum(); cellIndex++) {
            var value = formatter.formatCellValue(headerRow.getCell(cellIndex)).strip();
            if (!value.isEmpty()) {
                headers.put(value, (int) cellIndex);
            }
        }
        return headers;
    }

    private static void validateHeaders(Map<String, ?> headers) {
        for (var requiredHeader : REQUIRED_HEADERS) {
            if (!headers.containsKey(requiredHeader)) {
                throw new IllegalArgumentException("File import thiếu cột " + requiredHeader);
            }
        }
    }

    private static String cell(Row row, Map<String, Integer> headerIndexes, String header, DataFormatter formatter) {
        var index = headerIndexes.get(header);
        if (index == null) {
            return null;
        }
        return formatter.formatCellValue(row.getCell(index));
    }

    private static boolean isBlankRow(String... values) {
        for (var value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String optional(Map<String, String> values, String header) {
        return values.containsKey(header) ? values.get(header) : null;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        var dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String removeUtf8Bom(String content) {
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
    }
}

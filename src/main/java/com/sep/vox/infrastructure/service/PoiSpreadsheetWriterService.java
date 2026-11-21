package com.sep.vox.infrastructure.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.sep.vox.application.port.output.SpreadsheetWriterPort;
import com.sep.vox.application.response.output.ColumnDropdown;
import com.sep.vox.application.response.output.SpreadsheetCell;
import com.sep.vox.application.response.output.SpreadsheetDocument;
import com.sep.vox.application.response.output.SpreadsheetOptions;

@Service
public class PoiSpreadsheetWriterService implements SpreadsheetWriterPort {

    /** Characters that Excel and LibreOffice read as the start of a formula. */
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";
    private static final int POI_WIDTH_UNITS_PER_CHARACTER = 256;

    @Override
    public byte[] write(SpreadsheetDocument document) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet(document.sheetName());
            writeHeader(workbook, sheet, document);
            writeRows(workbook, sheet, document);
            document.dropdowns().forEach(dropdown -> addDropdown(sheet, dropdown));
            applyLayout(sheet, document);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write the spreadsheet", exception);
        }
    }

    private void writeHeader(XSSFWorkbook workbook, XSSFSheet sheet, SpreadsheetDocument document) {
        CellStyle style = document.options().emphasizedHeader() ? headerStyle(workbook) : null;
        Row row = sheet.createRow(0);
        for (int index = 0; index < document.headers().size(); index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(document.headers().get(index));
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private void writeRows(XSSFWorkbook workbook, XSSFSheet sheet, SpreadsheetDocument document) {
        Map<String, CellStyle> dateStyles = new HashMap<String, CellStyle>();
        int rowIndex = 1;
        for (List<SpreadsheetCell> cells : document.rows()) {
            Row row = sheet.createRow(rowIndex++);
            for (int column = 0; column < cells.size(); column++) {
                writeCell(workbook, row.createCell(column), cells.get(column), document.options(), dateStyles);
            }
        }
    }

    private void writeCell(
            XSSFWorkbook workbook,
            Cell cell,
            SpreadsheetCell value,
            SpreadsheetOptions options,
            Map<String, CellStyle> dateStyles) {
        switch (value) {
            case SpreadsheetCell.Text text -> cell.setCellValue(textOf(text.value(), options));
            case SpreadsheetCell.Number number -> cell.setCellValue(number.value().doubleValue());
            case SpreadsheetCell.DateTime dateTime -> {
                cell.setCellValue(dateTime.value());
                cell.setCellStyle(dateStyles.computeIfAbsent(
                    dateTime.displayPattern(), pattern -> dateStyle(workbook, pattern)));
            }
            case SpreadsheetCell.Blank _ -> cell.setBlank();
        }
    }

    /**
     * A cell starting with {@code = + - @} (also after a tab or carriage return) gets a leading single quote. It is
     * user-entered data, not a formula, but Excel reads it as a formula and RUNS it when the file is opened;
     * {@code HYPERLINK} or {@code DDE} is enough to leak the data out.
     *
     * <p>Writing .xlsx removes the need for the CSV byte-order mark and quoting, but NOT for this: the risk is in
     * how Excel reads the cell, not in how the file is encoded.
     */
    private String textOf(String value, SpreadsheetOptions options) {
        boolean startsLikeFormula = !value.isEmpty() && FORMULA_TRIGGERS.indexOf(value.charAt(0)) >= 0;
        return options.neutralizeFormulas() && startsLikeFormula ? "'" + value : value;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle dateStyle(XSSFWorkbook workbook, String displayPattern) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(displayPattern));
        return style;
    }

    private void addDropdown(Sheet sheet, ColumnDropdown dropdown) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        CellRangeAddressList addresses = new CellRangeAddressList(
            dropdown.firstRow(), dropdown.lastRow(), dropdown.columnIndex(), dropdown.columnIndex());
        DataValidation validation = helper.createValidation(
            helper.createExplicitListConstraint(dropdown.allowedValues().toArray(String[]::new)), addresses);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private void applyLayout(XSSFSheet sheet, SpreadsheetDocument document) {
        SpreadsheetOptions options = document.options();
        int columnCount = document.headers().size();
        if (options.frozenHeader()) {
            sheet.createFreezePane(0, 1);
        }
        if (options.autoFilter()) {
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(document.rows().size(), 1), 0, columnCount - 1));
        }
        autoSizeColumns(sheet, columnCount, options.maxColumnWidthCharacters());
    }

    /** The cap exists because one very long cell would otherwise push the column off the screen. */
    private void autoSizeColumns(Sheet sheet, int columnCount, Integer maxWidthCharacters) {
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
            if (maxWidthCharacters != null) {
                int maxWidth = maxWidthCharacters * POI_WIDTH_UNITS_PER_CHARACTER;
                sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index), maxWidth));
            }
        }
    }
}

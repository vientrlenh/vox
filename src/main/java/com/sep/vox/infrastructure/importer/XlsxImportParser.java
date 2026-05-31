package com.sep.vox.infrastructure.importer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.sep.vox.application.common.importer.ImportFileParser;
import com.sep.vox.application.common.importer.ImportRow;

public class XlsxImportParser implements ImportFileParser {

    @Override
    public List<ImportRow> parse(InputStream inputStream) {
        try (var workbook = WorkbookFactory.create(inputStream)) {
            var sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Collections.emptyList();
            }
            var headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return Collections.emptyList();
            }
            var formatter = new DataFormatter();
            var headers = new ArrayList<String>();
            for (var cell : headerRow) {
                var value = formatter.formatCellValue(cell);
                headers.add(value != null ? value : "");
            }

            List<ImportRow> rows = new ArrayList<>();
            var firstDataRow = headerRow.getRowNum() + 1;
            for (int i = firstDataRow; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                var values = new ArrayList<String>();
                var columns = new LinkedHashMap<String, String>();
                boolean hasValue = false;
                for (int c = 0; c < headers.size(); c++) {
                    var cell = row.getCell(c);
                    var value = formatter.formatCellValue(cell);
                    if (value != null && !value.isBlank()) {
                        hasValue = true;
                    }
                    values.add(value != null ? value : null);
                    var header = headers.get(c);
                    if (header != null && !header.isBlank()) {
                        columns.put(header, value);
                    }
                }
                if (!hasValue) {
                    continue;
                }
                var rowNumber = row.getRowNum() + 1;
                rows.add(new ImportRow(rowNumber, columns, values, null));
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file Excel", e);
        }
    }
}

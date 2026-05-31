package com.sep.vox.infrastructure.importer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;

import com.sep.vox.application.common.importer.ImportFileParser;
import com.sep.vox.application.common.importer.ImportRow;

public class CsvImportParser implements ImportFileParser {

    @Override
    public List<ImportRow> parse(InputStream inputStream) {
        try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            var format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build();
            var parser = format.parse(reader);
            List<ImportRow> rows = new ArrayList<>();
            for (var record : parser) {
                var rowNumber = (int) record.getRecordNumber() + 1;
                var values = new ArrayList<String>();
                for (var value : record) {
                    values.add(value != null ? value : null);
                }
                rows.add(new ImportRow(rowNumber, record.toMap(), values, null));
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file CSV", e);
        }
    }
}

package com.sep.vox.infrastructure.importer;

import org.springframework.stereotype.Component;

import com.sep.vox.application.common.importer.ImportFileFormat;
import com.sep.vox.application.common.importer.ImportFileParser;
import com.sep.vox.application.common.importer.ImportParserFactory;
import com.sep.vox.application.common.importer.JsonImportParser;

@Component
public class DefaultImportParserFactory implements ImportParserFactory {

    @Override
    public ImportFileParser forFormat(ImportFileFormat format) {
        return switch (format) {
            case CSV -> new CsvImportParser();
            case XLSX -> new XlsxImportParser();
            case JSON -> new JsonImportParser();
        };
    }
}

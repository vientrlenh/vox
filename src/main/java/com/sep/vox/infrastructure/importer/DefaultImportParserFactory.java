package com.sep.vox.infrastructure.importer;

import org.springframework.stereotype.Component;

import com.sep.vox.application.common.importer.ImportFileFormat;
import com.sep.vox.application.common.importer.ImportFileParser;
import com.sep.vox.application.common.importer.ImportParserFactory;

import tools.jackson.databind.ObjectMapper;

@Component
public class DefaultImportParserFactory implements ImportParserFactory {

    private final ObjectMapper objectMapper;

    public DefaultImportParserFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ImportFileParser forFormat(ImportFileFormat format) {
        return switch (format) {
            case CSV -> new CsvImportParser();
            case XLSX -> new XlsxImportParser();
            case JSON -> new JsonImportParser(objectMapper);
        };
    }
}

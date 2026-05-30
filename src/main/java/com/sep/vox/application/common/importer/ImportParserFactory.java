package com.sep.vox.application.common.importer;

public final class ImportParserFactory {

    private ImportParserFactory() {
    }

    public static ImportFileParser forFormat(ImportFileFormat format) {
        return switch (format) {
            case CSV -> new CsvImportParser();
            case XLSX -> new XlsxImportParser();
            case JSON -> new JsonImportParser();
        };
    }
}

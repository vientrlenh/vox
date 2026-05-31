package com.sep.vox.application.common.importer;

public interface ImportParserFactory {
    ImportFileParser forFormat(ImportFileFormat format);
}

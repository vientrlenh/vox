package com.sep.vox.application.common.importer;

import java.io.InputStream;
import java.util.List;

public interface ImportFileParser {
    List<ImportRow> parse(InputStream inputStream);
}

package com.sep.vox.application.common.importer;

import java.util.List;
import java.util.Map;

public record ImportRow(
    int rowNumber,
    Map<String, String> columns,
    List<String> values,
    Map<String, Object> jsonValues
) {
}

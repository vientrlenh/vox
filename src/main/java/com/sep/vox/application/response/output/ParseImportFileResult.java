package com.sep.vox.application.response.output;

import java.util.List;
import java.util.Map;

public record ParseImportFileResult(
    List<String> originalHeaders, 
    Map<String, String> suggestedMapping, 
    List<Map<String, String>> sampleRows, 
    long totalRows
) {
    
}

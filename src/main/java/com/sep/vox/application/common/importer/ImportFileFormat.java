package com.sep.vox.application.common.importer;

public enum ImportFileFormat {
    CSV,
    XLSX,
    JSON;

    public static ImportFileFormat fromFileName(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("Không xác định được định dạng file");
        }
        var lowered = fileName.toLowerCase();
        if (lowered.endsWith(".csv")) {
            return CSV;
        }
        if (lowered.endsWith(".xlsx")) {
            return XLSX;
        }
        if (lowered.endsWith(".json")) {
            return JSON;
        }
        throw new IllegalArgumentException("Định dạng file không được hỗ trợ");
    }
}

package com.sep.vox.domain.dto;

import java.util.List;

public record SchoolClassImportResultDto(
        int totalRows,
        int createdCount,
        List<SchoolClassDto> classes) {
}

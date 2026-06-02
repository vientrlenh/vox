package com.sep.vox.application.port.input.command;

public record ImportSchoolClassRowCommand(
        int rowNumber,
        String languageCode,
        String schoolGradeCode,
        String targetSchoolLevelCode,
        String targetSchoolLevelVersion,
        String code,
        String name,
        String description) {
}

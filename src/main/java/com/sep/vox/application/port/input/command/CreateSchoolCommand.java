package com.sep.vox.application.port.input.command;

public record CreateSchoolCommand(
    String code,
    String name,
    String description,
    String contactPhone,
    String contactEmail,
    String domain,
    String address,
    int studentCount
) {
}

package com.sep.vox.application.port.input.command;

public record ImportFieldMapping(
    String column,
    Integer index,
    String path,
    String dateFormat
) {
}

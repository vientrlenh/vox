package com.sep.vox.application.port.input.command;

import java.util.List;

public record ImportFieldMapping(
    String column,
    Integer index,
    List<String> aliases,
    String path,
    String dateFormat
) {
}

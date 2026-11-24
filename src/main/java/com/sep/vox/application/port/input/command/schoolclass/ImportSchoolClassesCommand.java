package com.sep.vox.application.port.input.command.schoolclass;

import java.util.List;

public record ImportSchoolClassesCommand(List<ImportSchoolClassRowCommand> rows) {
}

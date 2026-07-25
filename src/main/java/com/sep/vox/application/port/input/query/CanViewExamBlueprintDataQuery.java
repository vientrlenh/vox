package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record CanViewExamBlueprintDataQuery(UUID examSchoolId, String examStatus) {
}

package com.sep.vox.interfaces.rest.dto.request;

import java.util.UUID;

public record CreateExamPaperRequest(
    String source,
    UUID copyFromPaperId
) {
}

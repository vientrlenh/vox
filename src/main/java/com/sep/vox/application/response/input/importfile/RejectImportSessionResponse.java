package com.sep.vox.application.response.input.importfile;

import java.util.UUID;

public record RejectImportSessionResponse(
    UUID importSessionId,
    String status,
    String reason
) {
}

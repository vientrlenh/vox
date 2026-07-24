package com.sep.vox.application.port.input.query;

import java.util.UUID;

public record GetQuestionAssetUploadUrlQuery(
    UUID questionId,
    String contentType
) {
}

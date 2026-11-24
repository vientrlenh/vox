package com.sep.vox.application.port.input.query.assessmentturn;

import java.util.UUID;

public record GetTurnUploadUrlQuery(
    UUID attemptAnswerId,
    int turnOrder
) {
}

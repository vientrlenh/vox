package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.model.subscription.RequestStatus;

public record ViewRequestsQuery(
    RequestStatus status,
    int page,
    int size
) {
}

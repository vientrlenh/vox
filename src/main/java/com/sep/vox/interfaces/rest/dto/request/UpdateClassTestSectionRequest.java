package com.sep.vox.interfaces.rest.dto.request;

import java.util.List;
import java.util.UUID;

public record UpdateClassTestSectionRequest(
    String title,
    List<UUID> questionIds
) {
}

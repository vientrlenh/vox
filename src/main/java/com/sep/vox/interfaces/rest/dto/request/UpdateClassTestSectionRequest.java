package com.sep.vox.interfaces.rest.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateClassTestSectionRequest(
    String title,
    String instruction,
    BigDecimal weight,
    List<UUID> questionIds
) {
}

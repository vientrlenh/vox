package com.sep.vox.domain.model.school;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SchoolBalance {
    private UUID id;
    private UUID schoolId;
    private BigDecimal amountVnd;
    private Instant createdAt;
    private Instant updatedAt;
}

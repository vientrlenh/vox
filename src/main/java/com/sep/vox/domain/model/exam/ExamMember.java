package com.sep.vox.domain.model.exam;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ExamMember {
    private UUID id;
    private UUID examId;
    private UUID userId;
    private ExamMemberRole role;
    private OffsetDateTime grantedAt;
    private UUID grantedBy;
}

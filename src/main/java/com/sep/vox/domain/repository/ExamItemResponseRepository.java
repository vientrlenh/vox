package com.sep.vox.domain.repository;

import java.util.UUID;

public interface ExamItemResponseRepository {
    int sumDurationSecondsBySessionId(UUID sessionId);
}
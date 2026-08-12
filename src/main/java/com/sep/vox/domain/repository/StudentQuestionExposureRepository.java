package com.sep.vox.domain.repository;

import java.util.UUID;

public interface StudentQuestionExposureRepository {

    void recordExposure(UUID studentId, UUID questionId);

    void removeExposure(UUID studentId, UUID questionId);
}

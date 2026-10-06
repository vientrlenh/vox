package com.sep.vox.application.projection.repository;

import java.util.UUID;

import com.sep.vox.application.projection.dto.LearnerProfileInfo;

public interface LearnerProfileQueryRepository {

    LearnerProfileInfo findCurrent(UUID studentId);
}

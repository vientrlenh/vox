package com.sep.vox.application.query.repository;

import java.util.UUID;

import com.sep.vox.application.query.dto.LearnerProfileInfo;

public interface LearnerProfileQueryRepository {

    LearnerProfileInfo findCurrent(UUID studentId);
}

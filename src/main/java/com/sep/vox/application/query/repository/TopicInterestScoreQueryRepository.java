package com.sep.vox.application.query.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.application.query.dto.TopicInterestRowInfo;

public interface TopicInterestScoreQueryRepository {

    List<TopicInterestRowInfo> findInterestProfileRows(UUID studentId);
}

package com.sep.vox.application.query.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SessionRowInfo {

    UUID getId();

    UUID getPracticePaperId();

    UUID getChosenPracticeTopicId();

    String getTopicName();

    String getOrigin();

    String getStatus();

    String getAbandonDiagnosis();

    Double getOverallScore();

    int getGradedSeconds();

    String getOfferedTopicIdsJson();

    OffsetDateTime getStartedAt();

    OffsetDateTime getEndedAt();
}

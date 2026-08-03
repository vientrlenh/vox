package com.sep.vox.application.query.dto;

import java.time.Instant;
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

    // Instant chu khong OffsetDateTime: driver tra TIMESTAMPTZ ve duoi dang Instant, va
    // projection cua Spring Data KHONG co converter Instant -> OffsetDateTime nen se nem
    // UnsupportedOperationException ngay khi co dong du lieu dau tien.
    Instant getStartedAt();

    Instant getEndedAt();
}

package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

public interface TopicInterestRowInfo {

    UUID getId();

    String getName();

    double getScore();

    int getSessionsMentioned();

    // Instant: xem chu thich trong SessionRowInfo -- projection khong doi duoc sang OffsetDateTime.
    java.time.Instant getLastMentionedAt();
}

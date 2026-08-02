package com.sep.vox.application.query.dto;

import java.util.UUID;

public interface TopicInterestRowInfo {

    UUID getId();

    String getName();

    double getScore();

    int getSessionsMentioned();

    java.time.OffsetDateTime getLastMentionedAt();
}

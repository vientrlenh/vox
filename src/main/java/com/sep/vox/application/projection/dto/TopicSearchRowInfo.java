package com.sep.vox.application.projection.dto;

import java.util.UUID;

public interface TopicSearchRowInfo {

    UUID getId();

    String getName();

    String getInterestDimension();

    boolean getSavedByMe();
}

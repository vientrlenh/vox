package com.sep.vox.application.projection.dto;

import java.util.UUID;

public interface RankedTopicInfo {

    UUID getId();

    String getName();

    String getInterestDimension();

    String getCurriculumGroup();

    double getTopicScore();

    int getMentions();

    double getDimensionScore();

    double getRecency();

    boolean getSavedByMe();
}

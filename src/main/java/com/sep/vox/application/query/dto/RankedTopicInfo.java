package com.sep.vox.application.query.dto;

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

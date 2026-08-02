package com.sep.vox.application.query.dto;

import java.util.UUID;

public interface WeaknessFrequencyInfo {

    UUID getStudentId();

    UUID getFrameworkCriterionId();

    String getSubAttribute();

    int getFrequency();

    int getRecentFrequency();
}

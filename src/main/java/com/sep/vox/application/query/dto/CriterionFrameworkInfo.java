package com.sep.vox.application.query.dto;

import java.util.UUID;

public interface CriterionFrameworkInfo {

    UUID getRubricCriterionId();

    String getRubricCode();

    double getWeight();

    double getMinScore();

    double getMaxScore();

    String getFrameworkCode();

    String getFrameworkName();

    String getFrameworkDescription();

    String getTargetBandId();

    String getTargetBandCode();

    String getTargetBandLabel();

    String getBandCode();

    String getBandLabel();

    int getBandOrder();

    String getDescriptor();
}

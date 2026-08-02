package com.sep.vox.application.query.dto;

public interface CriterionWeaknessInfo {

    String getCriterionCode();

    String getCriterionName();

    double getWeakness();

    int getObservationCount();

    boolean getReliable();
}

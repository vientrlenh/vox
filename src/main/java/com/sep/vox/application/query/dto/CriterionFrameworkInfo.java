package com.sep.vox.application.query.dto;

import java.util.UUID;

public interface CriterionFrameworkInfo {

    /** The grouping key -- from V13 it is framework_criteria.id, before that it was rubric_criterions.id. */
    UUID getCriterionId();

    /** The criterion code used as the criterionKey sent down to Python. */
    String getCriterionCode();


    /** The scoring scale of the criterion. Always 0 for practice. */
    double getMinScore();

    /** Always 100 for practice -- Azure returns HundredMark, so this is the native scale. */
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

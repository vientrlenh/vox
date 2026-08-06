package com.sep.vox.application.query.dto;

import java.util.UUID;

public interface WeaknessFrequencyInfo {

    UUID getStudentId();

    UUID getFrameworkCriterionId();

    String getSubAttribute();

    int getFrequency();

    /** Tổng phân rã theo số lần chấm đã trôi qua -- xem SpringDataWeaknessObservationRepository. */
    double getDecayedFrequency();
}

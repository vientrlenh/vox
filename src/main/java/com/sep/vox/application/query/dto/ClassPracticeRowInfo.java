package com.sep.vox.application.query.dto;

import java.util.UUID;

public interface ClassPracticeRowInfo {

    UUID getStudentId();

    String getFullName();

    String getWeakestCriterionCode();
}

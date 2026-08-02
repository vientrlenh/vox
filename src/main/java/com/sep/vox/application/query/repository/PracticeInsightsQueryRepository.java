package com.sep.vox.application.query.repository;

import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.ClassPracticeOverview;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionProgressPoint;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

import java.util.List;
import java.util.UUID;

public interface PracticeInsightsQueryRepository {

    WeaknessProfile weaknessProfile(UUID studentId);

    List<CriterionProgressPoint> progress(
        UUID studentId,
        String criterionCode,
        int days
    );

    void requireTeacherCanReadStudent(UUID teacherId, UUID studentId);

    void requireTeacherCanReadClass(UUID teacherId, UUID classId);

    ClassPracticeOverview classOverview(UUID classId);
}

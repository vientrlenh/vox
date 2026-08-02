package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.ClassPracticeOverview;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.ClassPracticeRow;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionProgressPoint;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionWeakness;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.SubAttributeWeakness;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.service.WeaknessVectorSettings;
import com.sep.vox.application.query.repository.PracticeInsightsQueryRepository;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeInsightsQueryRepository;

@Service
@Transactional(readOnly = true)
public class JpaPracticeInsightsQueryRepository
        implements PracticeInsightsQueryRepository {

    private final SpringDataPracticeInsightsQueryRepository practiceInsightsQueryRepository;
    private final WeaknessVectorSettings weaknessVectorSettings;

    public JpaPracticeInsightsQueryRepository(
            SpringDataPracticeInsightsQueryRepository practiceInsightsQueryRepository,
            WeaknessVectorSettings weaknessVectorSettings) {
        this.practiceInsightsQueryRepository = practiceInsightsQueryRepository;
        this.weaknessVectorSettings = weaknessVectorSettings;
    }

    @Override
    public WeaknessProfile weaknessProfile(UUID studentId) {
        var criteria = practiceInsightsQueryRepository.findCriterionWeaknesses(studentId).stream()
            .map(row -> new CriterionWeakness(
                row.getCriterionCode(),
                row.getCriterionName(),
                row.getWeakness(),
                row.getObservationCount(),
                row.getReliable()
            ))
            .toList();
        var subAttributes = practiceInsightsQueryRepository.findSubAttributeWeaknesses(studentId).stream()
            .map(row -> new SubAttributeWeakness(
                row.getCriterionCode(),
                row.getSubAttribute(),
                row.getOccurrenceCount(),
                row.getSeverity(),
                row.getPracticeable()
            ))
            .toList();
        var sessionsAnalysed = practiceInsightsQueryRepository.countSessionsAnalysed(
            studentId,
            OffsetDateTime.now().minus(weaknessVectorSettings.observationWindow())
        );
        var trendCounts = practiceInsightsQueryRepository.findWeaknessTrendCounts(studentId);
        return new WeaknessProfile(
            criteria,
            subAttributes,
            sessionsAnalysed,
            trendCounts.getNearlyFixed(),
            trendCounts.getNewlyFound()
        );
    }

    @Override
    public List<CriterionProgressPoint> progress(UUID studentId, String criterionCode, int days) {
        var safeDays = Math.max(1, Math.min(days, 3650));
        return practiceInsightsQueryRepository.findProgress(
            studentId,
            OffsetDateTime.now().minusDays(safeDays),
            criterionCode
        ).stream()
            .map(row -> new CriterionProgressPoint(
                row.getCriterionCode(),
                row.getObservedDate(),
                row.getLatentLevel(),
                "EXAM"
            ))
            .toList();
    }

    @Override
    public void requireTeacherCanReadStudent(UUID teacherId, UUID studentId) {
        if (!practiceInsightsQueryRepository.canTeacherReadStudent(teacherId, studentId)) {
            throw new ForbiddenException("Bạn không có quyền xem dữ liệu của học sinh này");
        }
    }

    @Override
    public void requireTeacherCanReadClass(UUID teacherId, UUID classId) {
        if (!practiceInsightsQueryRepository.canTeacherReadClass(teacherId, classId)) {
            throw new ForbiddenException("Bạn không có quyền xem dữ liệu của lớp này");
        }
    }

    @Override
    public ClassPracticeOverview classOverview(UUID classId) {
        var rows = practiceInsightsQueryRepository.findClassOverviewRows(classId).stream()
            .map(row -> new ClassPracticeRow(
                row.getStudentId(),
                row.getFullName(),
                0,
                0,
                null,
                null,
                row.getWeakestCriterionCode()
            ))
            .toList();
        return new ClassPracticeOverview(classId, rows.size(), 0, rows);
    }
}

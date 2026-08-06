package com.sep.vox.infrastructure.persistence.query;

import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionWeakness;
import static com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        var sessionsAnalysed = practiceInsightsQueryRepository.countSessionsAnalysed(
            studentId,
            Instant.now().minus(weaknessVectorSettings.observationWindow())
        );
        return new WeaknessProfile(criteria, sessionsAnalysed);
    }
}

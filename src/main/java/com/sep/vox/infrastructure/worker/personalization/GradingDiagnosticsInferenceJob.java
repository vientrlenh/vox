package com.sep.vox.infrastructure.worker.personalization;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sep.vox.application.event.HumanGradingSubmittedEvent;
import com.sep.vox.domain.model.personalization.WeaknessObservation;
import com.sep.vox.domain.model.personalization.WeaknessObservationSourceType;
import com.sep.vox.domain.repository.WeaknessObservationRepository;
import com.sep.vox.infrastructure.service.GradingDiagnosticsClient;

/**
 * Nghe HumanGradingSubmittedEvent (RegradeResultUseCase) sau commit, gọi Python phân loại
 * feedbackSummary giáo viên thành nhãn điểm yếu theo đúng taxonomy đóng, rồi ghi
 * weakness_observation -- song song với nhánh AI (WeaknessObservationDerivationService),
 * không thay thế/xoá gì của nhánh đó.
 */
@Component
public class GradingDiagnosticsInferenceJob {

    private static final Logger log = LoggerFactory.getLogger(GradingDiagnosticsInferenceJob.class);

    private final GradingDiagnosticsClient gradingDiagnosticsClient;
    private final WeaknessObservationRepository weaknessObservationRepository;

    public GradingDiagnosticsInferenceJob(
            GradingDiagnosticsClient gradingDiagnosticsClient,
            WeaknessObservationRepository weaknessObservationRepository) {
        this.gradingDiagnosticsClient = gradingDiagnosticsClient;
        this.weaknessObservationRepository = weaknessObservationRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHumanGradingSubmitted(HumanGradingSubmittedEvent event) {
        try {
            var observedAt = OffsetDateTime.now();
            var labels = gradingDiagnosticsClient.infer(event.items());
            var saved = 0;
            for (var label : labels) {
                var observation = new WeaknessObservation(
                    event.studentId(),
                    WeaknessObservationSourceType.EXAM,
                    label.evaluationId(),
                    label.frameworkCriterionId(),
                    label.criterionCode(),
                    label.label(),
                    label.evidenceSpan(),
                    observedAt
                );
                var alreadyExists = weaknessObservationRepository.existsForKey(
                    observation.getSourceEvaluationId(),
                    observation.getFrameworkCriterionId(),
                    observation.getSubAttribute(),
                    observation.getEvidenceSpan()
                );
                if (!alreadyExists) {
                    weaknessObservationRepository.save(observation);
                    saved++;
                }
            }
            if (saved > 0) {
                log.info(
                    "Đã ghi {} weakness observation từ nhận xét chấm tay cho học sinh {}",
                    saved,
                    event.studentId()
                );
            }
        } catch (Exception exception) {
            log.warn(
                "Không thể suy nhãn điểm yếu từ nhận xét chấm tay cho học sinh {}",
                event.studentId(),
                exception
            );
        }
    }
}

package com.sep.vox.domain.model.assessment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentTests {

    private final UUID blueprintId = UUID.randomUUID();
    private final UUID schoolId = UUID.randomUUID();
    private final UUID languageId = UUID.randomUUID();

    @Test
    void centralizedDraftStartsAsDraftWithLabDeliveryAndOtpRequired() {
        Assessment assessment = Assessment.centralizedDraft(blueprintId, "CODE", "Name", "Description", schoolId, languageId);

        assertThat(assessment.getBlueprintId()).isEqualTo(blueprintId);
        assertThat(assessment.getCode()).isEqualTo("CODE");
        assertThat(assessment.getName()).isEqualTo("Name");
        assertThat(assessment.getDescription()).isEqualTo("Description");
        assertThat(assessment.getSchoolId()).isEqualTo(schoolId);
        assertThat(assessment.getLanguageId()).isEqualTo(languageId);
        assertThat(assessment.getKind()).isEqualTo(AssessmentKind.CENTRALIZED);
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.DRAFT);
        assertThat(assessment.getDeliveryMode()).isEqualTo(AssessmentDeliveryMode.LAB);
        assertThat(assessment.isRequiresOtp()).isTrue();
        assertThat(assessment.getMaxAttempt()).isEqualTo(1);
        assertThat(assessment.getResultDecisionMethod()).isEqualTo(ResultDecisionMethod.HIGHEST);
    }

    @Test
    void classTestDraftHasNoBlueprintAndDefaultsToStudentDeviceWithoutOtp() {
        Assessment assessment = Assessment.classTestDraft("CT-1", "Name", "Description", schoolId, languageId);

        assertThat(assessment.getBlueprintId()).isNull();
        assertThat(assessment.getCode()).isEqualTo("CT-1");
        assertThat(assessment.getSchoolId()).isEqualTo(schoolId);
        assertThat(assessment.getLanguageId()).isEqualTo(languageId);
        assertThat(assessment.getKind()).isEqualTo(AssessmentKind.CLASS_TEST);
        assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.DRAFT);
        assertThat(assessment.getDeliveryMode()).isEqualTo(AssessmentDeliveryMode.STUDENT_DEVICE);
        assertThat(assessment.isRequiresOtp()).isFalse();
        assertThat(assessment.getMaxAttempt()).isEqualTo(1);
        assertThat(assessment.getResultDecisionMethod()).isEqualTo(ResultDecisionMethod.HIGHEST);
    }

    @Test
    void configureWithNullsKeepsTheDraftDefaults() {
        Assessment assessment = Assessment.centralizedDraft(blueprintId, "CODE", "Name", null, schoolId, languageId);

        assessment.configure(null, null, null, null, null);

        assertThat(assessment.getDeliveryMode()).isEqualTo(AssessmentDeliveryMode.LAB);
        assertThat(assessment.getMaxAttempt()).isEqualTo(1);
        assertThat(assessment.getAssessmentTimeDurationSecond()).isNull();
        assertThat(assessment.getResultDecisionMethod()).isEqualTo(ResultDecisionMethod.HIGHEST);
        assertThat(assessment.isRequiresOtp()).isTrue();
    }

    @Test
    void configureWithValuesOverridesTheDraftDefaults() {
        Assessment assessment = Assessment.classTestDraft("CT-1", "Name", null, schoolId, languageId);

        assessment.configure(AssessmentDeliveryMode.LAB, 3, 600, ResultDecisionMethod.values()[0], true);

        assertThat(assessment.getDeliveryMode()).isEqualTo(AssessmentDeliveryMode.LAB);
        assertThat(assessment.getMaxAttempt()).isEqualTo(3);
        assertThat(assessment.getAssessmentTimeDurationSecond()).isEqualTo(600);
        assertThat(assessment.getResultDecisionMethod()).isEqualTo(ResultDecisionMethod.values()[0]);
        assertThat(assessment.isRequiresOtp()).isTrue();
    }

    @Test
    void scheduleBetweenSetsTheWindowAndThePolicy() {
        Assessment assessment = Assessment.classTestDraft("CT-1", "Name", null, schoolId, languageId);
        Instant openAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant closeAt = Instant.parse("2026-01-02T00:00:00Z");
        UUID policyId = UUID.randomUUID();

        assessment.scheduleBetween(openAt, closeAt, policyId);

        assertThat(assessment.getOpenAt()).isEqualTo(openAt);
        assertThat(assessment.getCloseAt()).isEqualTo(closeAt);
        assertThat(assessment.getAssessmentPolicyId()).isEqualTo(policyId);
    }

    @Test
    void useStreamProctoringSetsStreamTypesAndConfidenceThreshold() {
        Assessment assessment = Assessment.classTestDraft("CT-1", "Name", null, schoolId, languageId);

        assessment.useStreamProctoring(AssessmentRequiredStreamType.values()[0], AssessmentStreamTypePermission.values()[0],
            new BigDecimal("85"));

        assertThat(assessment.getRequiredStreamType()).isEqualTo(AssessmentRequiredStreamType.values()[0]);
        assertThat(assessment.getStreamTypePermission()).isEqualTo(AssessmentStreamTypePermission.values()[0]);
        assertThat(assessment.getAiConfidenceThresholdPercent()).isEqualByComparingTo("85");
    }
}

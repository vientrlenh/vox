package com.sep.vox.domain.model.assessmentpolicy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssessmentPolicyTests {

    private final UUID gradeLevelId = UUID.randomUUID();
    private final UUID languageId = UUID.randomUUID();
    private final UUID frameworkVersionId = UUID.randomUUID();
    private final UUID rubricVersionId = UUID.randomUUID();
    private final UUID bandId = UUID.randomUUID();
    private final Instant effectiveFrom = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant effectiveTo = Instant.parse("2026-12-31T00:00:00Z");
    private final AssessmentPolicyStrictness strictness = AssessmentPolicyStrictness.values()[0];

    @Test
    void draftForScopeIsADraftWithTheGivenScopeAndVersion() {
        UUID schoolId = UUID.randomUUID();
        UUID schoolGradeId = UUID.randomUUID();
        UUID schoolClassId = UUID.randomUUID();

        AssessmentPolicy policy = AssessmentPolicy.draftForScope(schoolId, gradeLevelId, schoolGradeId, schoolClassId,
            languageId, frameworkVersionId, rubricVersionId, bandId, new BigDecimal("5"), strictness, 3,
            effectiveFrom, effectiveTo);

        assertThat(policy.getSchoolId()).isEqualTo(schoolId);
        assertThat(policy.getGradeLevelId()).isEqualTo(gradeLevelId);
        assertThat(policy.getSchoolGradeId()).isEqualTo(schoolGradeId);
        assertThat(policy.getSchoolClassId()).isEqualTo(schoolClassId);
        assertThat(policy.getLanguageId()).isEqualTo(languageId);
        assertThat(policy.getFrameworkVersionId()).isEqualTo(frameworkVersionId);
        assertThat(policy.getRubricVersionId()).isEqualTo(rubricVersionId);
        assertThat(policy.getTargetFrameworkBandId()).isEqualTo(bandId);
        assertThat(policy.getPassingScore()).isEqualByComparingTo("5");
        assertThat(policy.getStrictness()).isEqualTo(strictness);
        assertThat(policy.getVersion()).isEqualTo(3);
        assertThat(policy.getStatus()).isEqualTo(AssessmentPolicyStatus.DRAFT);
        assertThat(policy.getEffectiveFrom()).isEqualTo(effectiveFrom);
        assertThat(policy.getEffectiveTo()).isEqualTo(effectiveTo);
    }

    @Test
    void draftSystemWideHasNoSchoolGradeOrClassScope() {
        AssessmentPolicy policy = AssessmentPolicy.draftSystemWide(gradeLevelId, languageId, frameworkVersionId,
            rubricVersionId, bandId, null, strictness, 1, effectiveFrom, null);

        assertThat(policy.getSchoolId()).isNull();
        assertThat(policy.getSchoolGradeId()).isNull();
        assertThat(policy.getSchoolClassId()).isNull();
        assertThat(policy.getGradeLevelId()).isEqualTo(gradeLevelId);
        assertThat(policy.getLanguageId()).isEqualTo(languageId);
        assertThat(policy.getPassingScore()).isNull();
        assertThat(policy.getVersion()).isEqualTo(1);
        assertThat(policy.getStatus()).isEqualTo(AssessmentPolicyStatus.DRAFT);
        assertThat(policy.getEffectiveFrom()).isEqualTo(effectiveFrom);
        assertThat(policy.getEffectiveTo()).isNull();
    }
}

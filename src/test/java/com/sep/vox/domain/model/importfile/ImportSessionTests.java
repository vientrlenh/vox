package com.sep.vox.domain.model.importfile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportSessionTests {

    private static final Instant EXPIRES_AT = Instant.parse("2026-11-24T10:00:00Z");

    @Test
    void previewedSetsTheGivenValuesAndStartsWithEmptyCounters() {
        UUID schoolId = UUID.randomUUID();

        ImportSession session = ImportSession.previewed(
                schoolId, ImportType.QUESTION, "questions.xlsx", "[\"h\"]", "{\"h\":\"x\"}", 5L, EXPIRES_AT);

        assertThat(session.getSchoolId()).isEqualTo(schoolId);
        assertThat(session.getType()).isEqualTo(ImportType.QUESTION);
        assertThat(session.getFileName()).isEqualTo("questions.xlsx");
        assertThat(session.getOriginalHeadersJson()).isEqualTo("[\"h\"]");
        assertThat(session.getSuggestedMappingJson()).isEqualTo("{\"h\":\"x\"}");
        assertThat(session.getTotalRows()).isEqualTo(5L);
        assertThat(session.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.PREVIEWED);
        assertThat(session.getImportedEntityId()).isNull();
        assertThat(session.getConfirmedMappingJson()).isNull();
        assertThat(session.getFailureReason()).isNull();
        assertThat(session.getClaimedAt()).isNull();
        assertThat(session.getClaimedBy()).isNull();
        assertThat(session.getLeaseExpiresAt()).isNull();
        assertThat(session.getValidRows()).isZero();
        assertThat(session.getInvalidRows()).isZero();
        assertThat(session.getImportedRows()).isZero();
        assertThat(session.getSkippedRows()).isZero();
        assertThat(session.getAttempts()).isZero();
    }

    @Test
    void previewedForEntityAlsoKeepsTheTargetEntityId() {
        UUID entityId = UUID.randomUUID();

        ImportSession session = ImportSession.previewedForEntity(
                null, ImportType.RUBRIC_VERSION, "f.xlsx", "[]", "{}", 2L, EXPIRES_AT, entityId);

        assertThat(session.getImportedEntityId()).isEqualTo(entityId);
        assertThat(session.getSchoolId()).isNull();
        assertThat(session.getType()).isEqualTo(ImportType.RUBRIC_VERSION);
        assertThat(session.getStatus()).isEqualTo(ImportSessionStatus.PREVIEWED);
        assertThat(session.getTotalRows()).isEqualTo(2L);
    }
}

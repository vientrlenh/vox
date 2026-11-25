package com.sep.vox.domain.model.framework;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FrameworkVersionTests {

    private static final Instant FROM = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2027-01-01T00:00:00Z");

    @Test
    void draftSetsEveryArgumentAndStartsInDraftStatus() {
        UUID frameworkId = UUID.randomUUID();

        FrameworkVersion version = FrameworkVersion.draft(frameworkId, "V1", "Version 1", "first", 1, FROM, TO);

        assertThat(version.getId()).isNull();
        assertThat(version.getFrameworkId()).isEqualTo(frameworkId);
        assertThat(version.getCode()).isEqualTo("V1");
        assertThat(version.getName()).isEqualTo("Version 1");
        assertThat(version.getDescription()).isEqualTo("first");
        assertThat(version.getVersion()).isEqualTo(1);
        assertThat(version.getEffectiveFrom()).isEqualTo(FROM);
        assertThat(version.getEffectiveTo()).isEqualTo(TO);
        assertThat(version.getStatus()).isEqualTo(FrameworkVersionStatus.DRAFT);
    }

    @Test
    void withEditableFieldsReplacesEditableFieldsAndKeepsIdentityVersionStatusAndCreation() {
        UUID id = UUID.randomUUID();
        UUID frameworkId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-06-01T00:00:00Z");
        FrameworkVersion stored = FrameworkVersion.builder()
                .id(id)
                .frameworkId(frameworkId)
                .code("OLD")
                .name("Old")
                .description("old description")
                .version(3)
                .effectiveFrom(FROM)
                .effectiveTo(TO)
                .status(FrameworkVersionStatus.DRAFT)
                .createdAt(createdAt)
                .createdBy("creator")
                .updatedAt(createdAt)
                .updatedBy("editor")
                .build();
        Instant newFrom = Instant.parse("2026-02-01T00:00:00Z");

        FrameworkVersion edited = stored.withEditableFields("NEW", "New", null, newFrom, null);

        assertThat(edited).isNotSameAs(stored);
        assertThat(edited.getId()).isEqualTo(id);
        assertThat(edited.getFrameworkId()).isEqualTo(frameworkId);
        assertThat(edited.getCode()).isEqualTo("NEW");
        assertThat(edited.getName()).isEqualTo("New");
        assertThat(edited.getDescription()).isNull();
        assertThat(edited.getVersion()).isEqualTo(3);
        assertThat(edited.getEffectiveFrom()).isEqualTo(newFrom);
        assertThat(edited.getEffectiveTo()).isNull();
        assertThat(edited.getStatus()).isEqualTo(FrameworkVersionStatus.DRAFT);
        assertThat(edited.getCreatedAt()).isEqualTo(createdAt);
        assertThat(edited.getCreatedBy()).isEqualTo("creator");
        assertThat(edited.getUpdatedAt()).isNull();
        assertThat(edited.getUpdatedBy()).isNull();
    }
}

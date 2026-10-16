package com.sep.vox.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkVersion;

public interface FrameworkVersionRepository {
    Optional<FrameworkVersion> findById(UUID id);
    boolean containsDraftInFrameworkId(UUID frameworkId);
    long findLatestNonDraftVersionNumberOnFrameworkId(UUID frameworkId);
    FrameworkVersion save(FrameworkVersion version);
    FrameworkVersion saveAndFlush(FrameworkVersion version);
}

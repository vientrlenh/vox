package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;

/**
 * Target band ceiling per (grade level, framework version). See GradeLevelBandScope.
 */
public interface GradeLevelBandScopeRepository {

    Optional<GradeLevelBandScope> findByGradeLevelIdAndFrameworkVersionId(UUID gradeLevelId, UUID frameworkVersionId);

    /**
     * Preloads many pairs for the import flow: Assessment Policy import reads in batches and must not let
     * each Excel row fire its own query.
     */
    List<GradeLevelBandScope> findByGradeLevelIdInAndFrameworkVersionIdIn(
            Collection<UUID> gradeLevelIds, Collection<UUID> frameworkVersionIds);

    /** Used to block deleting a band that some grade declares as its ceiling/default band. */
    boolean existsByBandId(UUID bandId);

    GradeLevelBandScope save(GradeLevelBandScope bandScope);

    void deleteById(UUID id);
}

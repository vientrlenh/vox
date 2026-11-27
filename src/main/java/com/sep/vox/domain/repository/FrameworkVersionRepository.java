package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;

public interface FrameworkVersionRepository {
    Optional<FrameworkVersion> findById(UUID id);
    Optional<FrameworkVersion> findByCode(String code);
    Optional<FrameworkVersion> findByName(String name);
    List<FrameworkVersion> findByIdIn(Collection<UUID> ids);
    List<FrameworkVersion> findByCodeIn(Collection<String> codes);
    List<FrameworkVersion> findByNameIn(Collection<String> names);
    Optional<FrameworkVersion> findByIdForUpdate(UUID id);
    PageResult<FrameworkVersion> findByFrameworkId(UUID frameworkId, int pageNumber, int size);
    List<FrameworkVersion> findByFrameworkIdAndStatus(UUID frameworkId, FrameworkVersionStatus status);
    PageResult<FrameworkVersion> findByFrameworkIdAndStatus(UUID frameworkId, FrameworkVersionStatus status, int pageNumber, int size);
    Optional<FrameworkVersion> findByFrameworkIdAndVersion(UUID frameworkId, int version);
    FrameworkVersion save(FrameworkVersion version);
    int updateStatus(UUID id, FrameworkVersionStatus status);
    void deleteById(UUID id);
    boolean existsByFrameworkId(UUID frameworkId);

    /**
     * The framework version in effect system-wide -- the framework source for PRACTICE.
     *
     * <p>Different from the assessment path (looked up through the assessment policy by student class): practice does not
     * depend on a policy, see V13 and {@code SpringDataFrameworkVersionRepository.findActiveVersionId}.
     *
     * @param code points directly at one version code; null = automatically choose the active version
     */
    Optional<UUID> findActiveVersionId(String code);

    /**
     * Every framework in effect, each with its latest PUBLISHED version -- the source for the
     * framework picker shown before choosing a band on the practice screen.
     *
     * <p>Same effectiveness conditions as {@link #findActiveVersionId}, except that it does not collect down to
     * a single framework.
     */
    List<ActiveFramework> findActiveFrameworks();

    /** A framework in effect together with its latest version. */
    record ActiveFramework(
        UUID frameworkId,
        String frameworkCode,
        String frameworkName,
        String frameworkDescription,
        UUID versionId,
        String versionCode,
        int versionNumber
    ) {
    }
}

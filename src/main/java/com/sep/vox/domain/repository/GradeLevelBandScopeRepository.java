package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.gradelevel.GradeLevelBandScope;

/**
 * Trần bậc mục tiêu theo (khối lớp, phiên bản khung). Xem GradeLevelBandScope.
 */
public interface GradeLevelBandScopeRepository {

    Optional<GradeLevelBandScope> findByGradeLevelIdAndFrameworkVersionId(UUID gradeLevelId, UUID frameworkVersionId);

    /**
     * Nạp sẵn nhiều cặp cho luồng import: import Assessment Policy đọc theo lô, không được để
     * mỗi dòng Excel bắn một câu truy vấn.
     */
    List<GradeLevelBandScope> findByGradeLevelIdInAndFrameworkVersionIdIn(
            Collection<UUID> gradeLevelIds, Collection<UUID> frameworkVersionIds);

    /** Dùng để chặn xóa một bậc đang được khai làm trần/bậc mặc định của khối nào đó. */
    boolean existsByBandId(UUID bandId);

    GradeLevelBandScope save(GradeLevelBandScope bandScope);

    void deleteById(UUID id);
}

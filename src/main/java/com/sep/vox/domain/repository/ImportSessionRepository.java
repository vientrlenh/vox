package com.sep.vox.domain.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;

public interface ImportSessionRepository {
    ImportSession save(ImportSession session);
    Optional<ImportSession> findById(UUID id);
    PageResult<ImportSession> findBySchoolId(UUID schoolId, ImportType type, ImportSessionStatus status, PageRequest pageRequest);
    int markImporting(UUID id, String type, String confirmedMapping, OffsetDateTime now, UUID updatedBy);
    int markQueued(UUID id, String type, String confirmedMapping, OffsetDateTime now, UUID updatedBy);
    int markClaimed(Collection<UUID> ids, UUID worker, OffsetDateTime now, OffsetDateTime leaseUntil);
    List<UUID> lockQueueIds(int limit);
    void extendLease(UUID id, OffsetDateTime leaseUntil);
    int requeueExpiredLeases(OffsetDateTime now, int maxAttempts);
}

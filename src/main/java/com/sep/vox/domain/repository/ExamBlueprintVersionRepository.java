package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamBlueprintVersion;
import com.sep.vox.domain.model.exam.ExamBlueprintVersionStatus;

public interface ExamBlueprintVersionRepository {
    ExamBlueprintVersion save(ExamBlueprintVersion version);
    Optional<ExamBlueprintVersion> findById(UUID id);
    List<ExamBlueprintVersion> findByIdIn(Collection<UUID> ids);
    List<ExamBlueprintVersion> findByBlueprintId(UUID blueprintId);
    List<ExamBlueprintVersion> findByBlueprintIdIn(Collection<UUID> blueprintIds);
    List<ExamBlueprintVersion> findByBlueprintIdAndStatus(UUID blueprintId, ExamBlueprintVersionStatus status);
    int nextVersionNumber(UUID blueprintId);
    boolean existsUsedByVersion(UUID versionId);
    void deleteById(UUID id);
}

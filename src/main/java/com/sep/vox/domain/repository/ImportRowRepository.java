package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;

public interface ImportRowRepository {
    List<ImportRow> findBySessionIdOrderByRowNumber(UUID sessionId);
    PageResult<ImportRow> findBySessionId(UUID sessionId, ImportRowStatus status, int pageNumber, int size);
    List<ImportRow> saveAll(List<ImportRow> rows);
    List<ImportRow> findBySessionId(UUID sessionId);
}

package com.sep.vox.domain.repository;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.importfile.ImportRow;

public interface ImportRowRepository {
    List<ImportRow> findBySessionIdOrderByRowNumber(UUID sessionId);
    List<ImportRow> saveAll(List<ImportRow> rows);
}

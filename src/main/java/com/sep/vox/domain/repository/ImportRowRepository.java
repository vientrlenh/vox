package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.importfile.ImportRow;

public interface ImportRowRepository {
    ImportRow save(ImportRow row);
    List<ImportRow> saveAll(Collection<ImportRow> rows);
    List<ImportRow> findBySessionIdOrderByRowNumberAsc(UUID sessionId);
}

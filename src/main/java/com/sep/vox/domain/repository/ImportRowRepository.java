package com.sep.vox.domain.repository;

import java.util.List;

import com.sep.vox.domain.model.importfile.ImportRow;

public interface ImportRowRepository {
    List<ImportRow> saveAll(List<ImportRow> rows);
}

package com.sep.vox.application.port.input.service;

import java.util.List;

import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportType;

public interface ImportCommitHandler {
    ImportType supportedType();
    ImportCommitResult commit(ImportSession session, List<ImportRow> rows);
}

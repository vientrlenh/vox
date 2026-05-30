package com.sep.vox.application.port.output;

public interface SchoolUserImportFileStoragePort {
    StoredImportFile save(ImportFileData fileData);
    ImportFileResource load(String fileId);
    void delete(String fileId);
}

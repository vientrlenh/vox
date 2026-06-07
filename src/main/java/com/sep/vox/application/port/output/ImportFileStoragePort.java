package com.sep.vox.application.port.output;

import java.util.UUID;

public interface ImportFileStoragePort {
    StoredImportFile save(ImportFileData fileData, UUID schoolId, UUID createdBy);
    ImportFileResource load(String fileId, UUID schoolId, UUID createdBy);
    void delete(String fileId, UUID schoolId, UUID createdBy);
}

package com.sep.vox.application.port.output;

import com.sep.vox.application.common.StoredFile;
import com.sep.vox.application.common.UploadedFile;

public interface StoragePort {
    StoredFile store(String key, UploadedFile file);

    void delete(String key);

    String resolveUrl(String key);
}

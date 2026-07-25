package com.sep.vox.application.port.output;

import java.time.Duration;

import com.sep.vox.application.common.StoredFile;
import com.sep.vox.application.common.UploadedFile;

public interface StoragePort {
    record PresignedUpload(String uploadUrl, String publicUrl) {
    }

    StoredFile store(String key, UploadedFile file);

    void delete(String key);

    String resolveUrl(String key);

    PresignedUpload presignUpload(String key, String contentType, Duration ttl);
}

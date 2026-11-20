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

    /**
     * An EXPIRING read link for an object in a SPECIFIED bucket -- to play directly in a
     * {@code <video>} tag, not to download.
     *
     * <p>S3 returns this link with HTTP Range support, so the browser can play and SEEK without
     * downloading the whole file first. That is why this approach was chosen over proxying through the server: assessment recordings
     * run for tens of minutes, and pushing them through the backend is useless bandwidth.
     *
     * <p>Different from every other method in two points, and both are mandatory: the bucket is passed in instead of
     * taken from configuration, and the key is used verbatim without prepending {@code keyPrefix}.
     *
     * <p>The assessment recording is written by vox-streaming straight to its own bucket
     * ({@code STORAGE_RECORDING_BUCKET}), different from the application's upload bucket
     * ({@code AWS_S3_BUCKET}), and the full key is already in {@code assessment_recordings.s3_key}.
     * Reusing {@code resolveUrl} would both point at the wrong bucket and insert an extra {@code dev/} prefix.
     *
     * @param bucket the bucket that holds the object -- throws if empty, because guessing the bucket is the surest way
     *               to produce a 404 link that nobody understands
     */
    String presignRead(String bucket, String key, Duration ttl);
}

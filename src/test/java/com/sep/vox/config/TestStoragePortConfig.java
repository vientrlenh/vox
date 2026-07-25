package com.sep.vox.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sep.vox.application.common.StoredFile;
import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.output.StoragePort;

/**
 * The only StoragePort implementation is AwsS3StorageService, which is gated on
 * storage.provider=s3 and needs real S3 client beans. Without a provider set,
 * every bean that depends on StoragePort fails and no @SpringBootTest can start
 * its context. This test-classpath-only stub keeps the context loadable.
 *
 * <p>Guarded by @ConditionalOnMissingBean so a test that wires a real or mocked
 * StoragePort still wins.
 */
@Configuration
public class TestStoragePortConfig {

    @Bean
    @ConditionalOnMissingBean(StoragePort.class)
    StoragePort testStoragePort() {
        return new StoragePort() {
            @Override
            public StoredFile store(String key, UploadedFile file) {
                return new StoredFile(key, resolveUrl(key), file.contentType(), file.size(), "test-etag");
            }

            @Override
            public void delete(String key) {
                // no-op
            }

            @Override
            public String resolveUrl(String key) {
                return "https://storage.test/" + key;
            }

            @Override
            public PresignedUpload presignUpload(String key, String contentType, Duration ttl) {
                return new PresignedUpload("https://storage.test/upload/" + key, resolveUrl(key));
            }
        };
    }
}

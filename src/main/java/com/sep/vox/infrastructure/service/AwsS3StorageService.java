package com.sep.vox.infrastructure.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StoredFile;
import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.infrastructure.config.AwsS3StorageProperties;
import com.sep.vox.infrastructure.exception.InfrastructureException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@ConditionalOnProperty(prefix = "storage", name = "provider", havingValue = "s3")
public class AwsS3StorageService implements StoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsS3StorageProperties properties;

    public AwsS3StorageService(S3Client s3Client, S3Presigner s3Presigner, AwsS3StorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public StoredFile store(String key, UploadedFile file) {
        validateKey(key);
        validateFile(file);

        var objectKey = buildObjectKey(key);
        try {
            var response = s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(requiredBucket())
                    .key(objectKey)
                    .contentType(contentTypeOf(file))
                    .contentLength(file.size())
                    .build(),
                RequestBody.fromBytes(file.content())
            );

            return new StoredFile(
                objectKey,
                resolveUrl(objectKey),
                contentTypeOf(file),
                file.size(),
                response.eTag()
            );
        } catch (S3Exception e) {
            throw new InfrastructureException("Khong the tai tep len S3: " + errorMessageOf(e));
        }
    }

    @Override
    public void delete(String key) {
        validateKey(key);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(requiredBucket())
                .key(buildObjectKey(key))
                .build());
        } catch (S3Exception e) {
            throw new InfrastructureException("Khong the xoa tep tren S3: " + errorMessageOf(e));
        }
    }

    @Override
    public String resolveUrl(String key) {
        validateKey(key);

        var objectKey = buildObjectKey(key);
        if (hasText(properties.getPublicBaseUrl())) {
            return properties.getPublicBaseUrl().replaceAll("/+$", "") + "/" + encodePath(objectKey);
        }

        if (hasText(properties.getEndpoint())) {
            return properties.getEndpoint().replaceAll("/+$", "") + "/" + requiredBucket() + "/" + encodePath(objectKey);
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(
            requiredBucket(),
            properties.getRegion(),
            encodePath(objectKey)
        );
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, Duration ttl) {
        validateKey(key);
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("Thời hạn presigned URL không hợp lệ");
        }

        var objectKey = buildObjectKey(key);
        try {
            var putObjectRequest = PutObjectRequest.builder()
                .bucket(requiredBucket())
                .key(objectKey)
                .contentType(hasText(contentType) ? contentType : "application/octet-stream")
                .build();
            var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putObjectRequest)
                .build();
            var presigned = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUpload(presigned.url().toString(), resolveUrl(objectKey));
        } catch (S3Exception e) {
            throw new InfrastructureException("Khong the tao presigned upload URL: " + errorMessageOf(e));
        }
    }

    private String buildObjectKey(String key) {
        var normalizedKey = key.strip().replace("\\", "/").replaceAll("^/+", "");
        var normalizedPrefix = normalizedPrefix();
        if (!hasText(normalizedPrefix)) {
            return normalizedKey;
        }
        if (normalizedKey.equals(normalizedPrefix) || normalizedKey.startsWith(normalizedPrefix + "/")) {
            return normalizedKey;
        }
        return normalizedPrefix + "/" + normalizedKey;
    }

    private String contentTypeOf(UploadedFile file) {
        return hasText(file.contentType()) ? file.contentType() : "application/octet-stream";
    }

    private String requiredBucket() {
        if (!hasText(properties.getBucket())) {
            throw new InfrastructureException("Cau hinh bucket S3 chua duoc thiet lap");
        }
        return properties.getBucket();
    }

    private void validateKey(String key) {
        if (!hasText(key)) {
            throw new IllegalArgumentException("Storage key khong duoc de trong");
        }
    }

    private void validateFile(UploadedFile file) {
        if (file == null || file.content() == null || file.content().length == 0 || file.size() <= 0) {
            throw new IllegalArgumentException("File tai len khong hop le");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizedPrefix() {
        if (!hasText(properties.getKeyPrefix())) {
            return "";
        }
        return properties.getKeyPrefix().strip().replace("\\", "/")
            .replaceAll("^/+", "")
            .replaceAll("/+$", "");
    }

    private String errorMessageOf(S3Exception exception) {
        if (exception.awsErrorDetails() != null && hasText(exception.awsErrorDetails().errorMessage())) {
            return exception.awsErrorDetails().errorMessage();
        }
        return exception.getMessage();
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }
}

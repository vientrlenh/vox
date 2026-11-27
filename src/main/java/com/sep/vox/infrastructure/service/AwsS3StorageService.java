package com.sep.vox.infrastructure.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.StoredFile;
import com.sep.vox.application.common.UploadedFile;
import com.sep.vox.application.port.output.StoragePort;
import com.sep.vox.infrastructure.properties.AwsS3StorageProperties;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@ConditionalOnProperty(prefix = "storage", name = "provider", havingValue = "s3")
@RequiredArgsConstructor
public class AwsS3StorageService implements StoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsS3StorageProperties properties;

    @Override
    public StoredFile store(String key, UploadedFile file) {
        validateKey(key);
        validateFile(file);

        String objectKey = buildObjectKey(key);
        try {
            PutObjectResponse response = putObject(objectKey, file);

            return new StoredFile(
                objectKey,
                resolveUrl(objectKey),
                contentTypeOf(file),
                file.size(),
                response.eTag()
            );
        } catch (S3Exception e) {
            throw new IllegalStateException("Khong the tai tep len S3: " + errorMessageOf(e));
        }
    }

    private PutObjectResponse putObject(String objectKey, UploadedFile file) {
        return s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(requiredBucket())
                .key(objectKey)
                .contentType(contentTypeOf(file))
                .contentLength(file.size())
                .build(),
            RequestBody.fromBytes(file.content())
        );
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
            throw new IllegalStateException("Khong the xoa tep tren S3: " + errorMessageOf(e));
        }
    }

    @Override
    public String resolveUrl(String key) {
        validateKey(key);

        String objectKey = buildObjectKey(key);
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

        String objectKey = buildObjectKey(key);
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(requiredBucket())
                .key(objectKey)
                .contentType(hasText(contentType) ? contentType : "application/octet-stream")
                .build();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(putObjectRequest)
                .build();
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return new PresignedUpload(presigned.url().toString(), resolveUrl(objectKey));
        } catch (S3Exception e) {
            throw new IllegalStateException("Khong the tao presigned upload URL: " + errorMessageOf(e));
        }
    }

    @Override
    public String presignRead(String bucket, String key, Duration ttl) {
        validateKey(key);
        if (!hasText(bucket)) {
            throw new IllegalArgumentException("Bucket khong duoc de trong khi tao presigned read URL");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("Thời hạn presigned URL không hợp lệ");
        }

        try {
            // NOT through buildObjectKey: the key here was generated by another system and is already complete, adding the
            // application's keyPrefix would point at an object that does not exist.
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key.strip().replace("\\", "/").replaceAll("^/+", ""))
                .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (S3Exception e) {
            throw new IllegalStateException("Khong the tao presigned read URL: " + errorMessageOf(e));
        }
    }

    private String buildObjectKey(String key) {
        String normalizedKey = key.strip().replace("\\", "/").replaceAll("^/+", "");
        String normalizedPrefix = normalizedPrefix();
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
            throw new IllegalStateException("Cau hinh bucket S3 chua duoc thiet lap");
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

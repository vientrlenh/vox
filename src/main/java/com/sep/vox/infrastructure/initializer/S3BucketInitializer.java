package com.sep.vox.infrastructure.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;


import com.sep.vox.infrastructure.properties.AwsS3StorageProperties;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Bao dam bucket S3 ton tai va dung cau hinh (policy + CORS) ngay khi ung dung khoi dong.
 *
 * <p>Bucket khong tao duoc luon lam startup that bai vi khong co bucket thi moi upload deu hong.
 * Cac buoc policy/CORS chi la hardening nen mac dinh chi canh bao — moi truong prod chay bang
 * credentials least-privileged thuong khong co quyen ghi bucket-level. Dat
 * {@code storage.s3.fail-on-error=true} neu muon chung cung chan startup.
 */
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class S3BucketInitializer implements ApplicationRunner {

    private static final String US_EAST_1 = "us-east-1";

    private final S3Client s3Client;
    private final AwsS3StorageProperties properties;
    private final JsonMapper jsonMapper;

    @Override
    public void run(ApplicationArguments args) {
        String bucket = properties.getBucket();
        if (!hasText(bucket)) {
            log.warn("storage.s3.bucket chua duoc cau hinh. Bo qua buoc khoi tao bucket S3");
            return;
        }

        if (bucketExists(bucket)) {
            log.info("Bucket S3 '{}' da ton tai", bucket);
        } else {
            createBucket(bucket);
        }

        applyBestEffort(bucket, "public access block", () -> relaxPublicAccessBlock(bucket));
        applyBestEffort(bucket, "bucket policy", () -> applyBucketPolicy(bucket));
        applyBestEffort(bucket, "CORS", () -> applyCorsConfiguration(bucket));
    }

    private boolean bucketExists(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            if (e.statusCode() == 403) {
                // Thieu quyen s3:ListBucket: coi nhu bucket da ton tai thay vi tao moi.
                log.warn("Khong du quyen kiem tra bucket S3 '{}'. Gia dinh bucket da ton tai", bucket);
                return true;
            }
            if (e.statusCode() == 301) {
                // S3 answers 301 when the client calls the wrong regional endpoint. The real region is in the response header.
                throw new IllegalStateException(
                    "Bucket S3 '%s' nam o region '%s' nhung storage.s3.region dang duoc cau hinh la '%s'"
                        .formatted(bucket, bucketRegionOf(e), properties.getRegion()));
            }
            throw new IllegalStateException(
                "Khong the kiem tra bucket S3 '" + bucket + "': " + messageOf(e));
        }
    }

    private void createBucket(String bucket) {
        String region = properties.getRegion();

        try {
            s3Client.createBucket(buildCreateBucketRequest(bucket, region));
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("Bucket S3 '{}' vua duoc tao boi mot tien trinh khac", bucket);
            return;
        } catch (BucketAlreadyExistsException e) {
            throw new IllegalStateException(
                "Ten bucket S3 '" + bucket + "' da thuoc ve mot AWS account khac. Can doi ten bucket");
        } catch (AwsServiceException e) {
            throw new IllegalStateException(
                "Khong the tao bucket S3 '" + bucket + "': " + messageOf(e));
        }

        s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder().bucket(bucket).build());
        log.info("Da tao bucket S3 '{}' tai region '{}'", bucket, region);
    }

    private CreateBucketRequest buildCreateBucketRequest(String bucket, String region) {
        CreateBucketRequest.Builder request = CreateBucketRequest.builder().bucket(bucket);
        // us-east-1 is the default S3 region and fails if a LocationConstraint is sent with it.
        if (hasText(region) && !US_EAST_1.equalsIgnoreCase(region)) {
            request.createBucketConfiguration(CreateBucketConfiguration.builder()
                .locationConstraint(BucketLocationConstraint.fromValue(region))
                .build());
        }
        return request.build();
    }

    private void relaxPublicAccessBlock(String bucket) {
        if (!properties.isPublicRead()) {
            return;
        }
        if (hasText(properties.getEndpoint())) {
            // MinIO/LocalStack khong implement PublicAccessBlock nen bo qua khi dung endpoint tu cau hinh.
            return;
        }

        // Bucket moi tren AWS mac dinh bat Block Public Access, khien policy "Principal": "*"
        // bi tu choi. Van chan public ACL vi quyen doc public duoc mo bang policy.
        s3Client.putPublicAccessBlock(PutPublicAccessBlockRequest.builder()
            .bucket(bucket)
            .publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
                .blockPublicAcls(true)
                .ignorePublicAcls(true)
                .blockPublicPolicy(false)
                .restrictPublicBuckets(false)
                .build())
            .build());
    }

    private void applyBucketPolicy(String bucket) {
        ArrayNode statements = buildPolicyStatements(bucket);

        if (statements.isEmpty()) {
            log.info("Khong co statement nao can ap dung cho bucket S3 '{}'. Bo qua bucket policy", bucket);
            return;
        }

        ObjectNode policy = jsonMapper.createObjectNode();
        policy.put("Version", "2012-10-17");
        policy.set("Statement", statements);

        s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
            .bucket(bucket)
            .policy(policy.toString())
            .build());
    }

    private ArrayNode buildPolicyStatements(String bucket) {
        String resource = "arn:aws:s3:::" + bucket + "/*";
        ArrayNode statements = jsonMapper.createArrayNode();

        if (properties.isPublicRead()) {
            statements.add(publicReadStatement(resource));
        }

        if (hasText(properties.getUploaderPrincipalArn())) {
            statements.add(uploaderPutStatement(resource));
        }
        return statements;
    }

    private ObjectNode publicReadStatement(String resource) {
        ObjectNode statement = jsonMapper.createObjectNode();
        statement.put("Sid", "PublicReadObjects");
        statement.put("Effect", "Allow");
        statement.put("Principal", "*");
        statement.put("Action", "s3:GetObject");
        statement.put("Resource", resource);
        return statement;
    }

    private ObjectNode uploaderPutStatement(String resource) {
        ObjectNode statement = jsonMapper.createObjectNode();
        statement.put("Sid", "AllowUploaderPutObject");
        statement.put("Effect", "Allow");
        statement.putObject("Principal").put("AWS", properties.getUploaderPrincipalArn().strip());
        statement.put("Action", "s3:PutObject");
        statement.put("Resource", resource);
        return statement;
    }

    private void applyCorsConfiguration(String bucket) {
        CORSRule rule = CORSRule.builder()
            .allowedHeaders("Range")
            .allowedMethods("GET", "HEAD")
            .allowedOrigins("*")
            .maxAgeSeconds(3600)
            .build();

        s3Client.putBucketCors(PutBucketCorsRequest.builder()
            .bucket(bucket)
            .corsConfiguration(CORSConfiguration.builder().corsRules(rule).build())
            .build());
    }

    private void applyBestEffort(String bucket, String step, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            String message = "Khong the ap dung %s cho bucket S3 '%s': %s".formatted(step, bucket, messageOf(e));
            if (properties.isFailOnError()) {
                throw new IllegalStateException(message);
            }
            log.warn("{}. Ung dung van tiep tuc khoi dong", message, e);
        }
    }

    private String bucketRegionOf(S3Exception exception) {
        if (exception.awsErrorDetails() == null || exception.awsErrorDetails().sdkHttpResponse() == null) {
            return "khong xac dinh";
        }
        return exception.awsErrorDetails().sdkHttpResponse()
            .firstMatchingHeader("x-amz-bucket-region")
            .orElse("khong xac dinh");
    }

    private String messageOf(Exception exception) {
        if (exception instanceof AwsServiceException awsException
            && awsException.awsErrorDetails() != null
            && hasText(awsException.awsErrorDetails().errorMessage())) {
            return awsException.awsErrorDetails().errorMessage();
        }
        return exception.getMessage();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

package com.sep.vox.infrastructure.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;


import com.sep.vox.infrastructure.exception.InfrastructureException;
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

/**
 * Bao dam bucket S3 ton tai va dung cau hinh (policy + CORS) ngay khi ung dung khoi dong.
 *
 * <p>Bucket khong tao duoc luon lam startup that bai vi khong co bucket thi moi upload deu hong.
 * Cac buoc policy/CORS chi la hardening nen mac dinh chi canh bao — moi truong prod chay bang
 * credentials least-privileged thuong khong co quyen ghi bucket-level. Dat
 * {@code storage.s3.fail-on-error=true} neu muon chung cung chan startup.
 */
@Order(0)
public class S3BucketInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3BucketInitializer.class);
    private static final String US_EAST_1 = "us-east-1";

    private final S3Client s3Client;
    private final AwsS3StorageProperties properties;
    private final JsonMapper jsonMapper;

    public S3BucketInitializer(S3Client s3Client, AwsS3StorageProperties properties, JsonMapper jsonMapper) {
        this.s3Client = s3Client;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        var bucket = properties.getBucket();
        if (!hasText(bucket)) {
            LOGGER.warn("storage.s3.bucket chua duoc cau hinh. Bo qua buoc khoi tao bucket S3");
            return;
        }

        if (bucketExists(bucket)) {
            LOGGER.info("Bucket S3 '{}' da ton tai", bucket);
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
                LOGGER.warn("Khong du quyen kiem tra bucket S3 '{}'. Gia dinh bucket da ton tai", bucket);
                return true;
            }
            if (e.statusCode() == 301) {
                // S3 tra 301 khi client goi nham regional endpoint. Region that nam o header phan hoi.
                throw new InfrastructureException(
                    "Bucket S3 '%s' nam o region '%s' nhung storage.s3.region dang duoc cau hinh la '%s'"
                        .formatted(bucket, bucketRegionOf(e), properties.getRegion()));
            }
            throw new InfrastructureException(
                "Khong the kiem tra bucket S3 '" + bucket + "': " + messageOf(e));
        }
    }

    private void createBucket(String bucket) {
        var request = CreateBucketRequest.builder().bucket(bucket);
        var region = properties.getRegion();
        // us-east-1 la region mac dinh cua S3 va se bao loi neu gui kem LocationConstraint.
        if (hasText(region) && !US_EAST_1.equalsIgnoreCase(region)) {
            request.createBucketConfiguration(CreateBucketConfiguration.builder()
                .locationConstraint(BucketLocationConstraint.fromValue(region))
                .build());
        }

        try {
            s3Client.createBucket(request.build());
        } catch (BucketAlreadyOwnedByYouException e) {
            LOGGER.info("Bucket S3 '{}' vua duoc tao boi mot tien trinh khac", bucket);
            return;
        } catch (BucketAlreadyExistsException e) {
            throw new InfrastructureException(
                "Ten bucket S3 '" + bucket + "' da thuoc ve mot AWS account khac. Can doi ten bucket");
        } catch (AwsServiceException e) {
            throw new InfrastructureException(
                "Khong the tao bucket S3 '" + bucket + "': " + messageOf(e));
        }

        s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder().bucket(bucket).build());
        LOGGER.info("Da tao bucket S3 '{}' tai region '{}'", bucket, region);
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
        var resource = "arn:aws:s3:::" + bucket + "/*";
        var statements = jsonMapper.createArrayNode();

        if (properties.isPublicRead()) {
            var statement = jsonMapper.createObjectNode();
            statement.put("Sid", "PublicReadObjects");
            statement.put("Effect", "Allow");
            statement.put("Principal", "*");
            statement.put("Action", "s3:GetObject");
            statement.put("Resource", resource);
            statements.add(statement);
        }

        if (hasText(properties.getUploaderPrincipalArn())) {
            var statement = jsonMapper.createObjectNode();
            statement.put("Sid", "AllowUploaderPutObject");
            statement.put("Effect", "Allow");
            statement.putObject("Principal").put("AWS", properties.getUploaderPrincipalArn().strip());
            statement.put("Action", "s3:PutObject");
            statement.put("Resource", resource);
            statements.add(statement);
        }

        if (statements.isEmpty()) {
            LOGGER.info("Khong co statement nao can ap dung cho bucket S3 '{}'. Bo qua bucket policy", bucket);
            return;
        }

        var policy = jsonMapper.createObjectNode();
        policy.put("Version", "2012-10-17");
        policy.set("Statement", statements);

        s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
            .bucket(bucket)
            .policy(policy.toString())
            .build());
    }

    private void applyCorsConfiguration(String bucket) {
        var rule = CORSRule.builder()
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
            var message = "Khong the ap dung %s cho bucket S3 '%s': %s".formatted(step, bucket, messageOf(e));
            if (properties.isFailOnError()) {
                throw new InfrastructureException(message);
            }
            LOGGER.warn("{}. Ung dung van tiep tuc khoi dong", message, e);
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

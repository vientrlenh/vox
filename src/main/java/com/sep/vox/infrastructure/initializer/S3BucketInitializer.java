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
 * Ensures the S3 bucket exists and is correctly configured (policy + CORS) as soon as the application starts.
 *
 * <p>A bucket that cannot be created always fails startup because without a bucket every upload is broken.
 * The policy/CORS steps are only hardening so by default they only warn -- production environments running with
 * least-privileged credentials usually lack bucket-level write permission. Set
 * {@code storage.s3.fail-on-error=true} if you want them to block startup too.
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
            log.warn("storage.s3.bucket is not configured. Skipping the S3 bucket initialization step");
            return;
        }

        if (bucketExists(bucket)) {
            log.info("S3 bucket '{}' already exists", bucket);
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
                // Missing the s3:ListBucket permission: treat the bucket as existing instead of creating a new one.
                log.warn("Not enough permission to check S3 bucket '{}'. Assuming the bucket exists", bucket);
                return true;
            }
            if (e.statusCode() == 301) {
                // S3 answers 301 when the client calls the wrong regional endpoint. The real region is in the response header.
                throw new IllegalStateException(
                    "S3 bucket '%s' is in region '%s' but storage.s3.region is configured as '%s'"
                        .formatted(bucket, bucketRegionOf(e), properties.getRegion()));
            }
            throw new IllegalStateException(
                "Could not check S3 bucket '" + bucket + "': " + messageOf(e));
        }
    }

    private void createBucket(String bucket) {
        String region = properties.getRegion();

        try {
            s3Client.createBucket(buildCreateBucketRequest(bucket, region));
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("S3 bucket '{}' was just created by another process", bucket);
            return;
        } catch (BucketAlreadyExistsException e) {
            throw new IllegalStateException(
                "The S3 bucket name '" + bucket + "' already belongs to another AWS account. Choose a different bucket name");
        } catch (AwsServiceException e) {
            throw new IllegalStateException(
                "Could not create S3 bucket '" + bucket + "': " + messageOf(e));
        }

        s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder().bucket(bucket).build());
        log.info("Created S3 bucket '{}' in region '{}'", bucket, region);
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
            // MinIO/LocalStack do not implement PublicAccessBlock so it is skipped when using a configured endpoint.
            return;
        }

        // A new AWS bucket has Block Public Access on by default, which makes a policy with "Principal": "*"
        // get rejected. Public ACLs are still blocked because public read access is opened through the policy.
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
            log.info("No statement to apply for S3 bucket '{}'. Skipping the bucket policy", bucket);
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
            String message = "Could not apply %s to S3 bucket '%s': %s".formatted(step, bucket, messageOf(e));
            if (properties.isFailOnError()) {
                throw new IllegalStateException(message);
            }
            log.warn("{}. The application continues to start", message, e);
        }
    }

    private String bucketRegionOf(S3Exception exception) {
        if (exception.awsErrorDetails() == null || exception.awsErrorDetails().sdkHttpResponse() == null) {
            return "unknown";
        }
        return exception.awsErrorDetails().sdkHttpResponse()
            .firstMatchingHeader("x-amz-bucket-region")
            .orElse("unknown");
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

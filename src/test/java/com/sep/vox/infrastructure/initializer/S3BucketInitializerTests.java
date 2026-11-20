package com.sep.vox.infrastructure.initializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.infrastructure.exception.InfrastructureException;
import com.sep.vox.infrastructure.properties.AwsS3StorageProperties;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

import tools.jackson.databind.json.JsonMapper;

class S3BucketInitializerTests {

    private static final String BUCKET = "vox-exam-uploads";
    private static final String UPLOADER_ARN = "arn:aws:iam::000000000000:user/Vox-Exam-storage-user";

    private S3Client s3Client;
    private AwsS3StorageProperties properties;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        jsonMapper = JsonMapper.builder().build();

        properties = new AwsS3StorageProperties();
        properties.setBucket(BUCKET);
        properties.setRegion("ap-southeast-1");
        properties.setEnsureBucket(true);
        properties.setPublicRead(true);
        properties.setUploaderPrincipalArn(UPLOADER_ARN);
    }

    private S3BucketInitializer initializer() {
        return new S3BucketInitializer(s3Client, properties, jsonMapper);
    }

    private void bucketMissing() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(NoSuchBucketException.builder().statusCode(404).message("Not Found").build());
        when(s3Client.waiter()).thenReturn(mock(S3Waiter.class));
    }

    @Test
    void skipsEverythingWhenBucketNotConfigured() {
        properties.setBucket("  ");

        initializer().run(null);

        verify(s3Client, never()).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void doesNotCreateBucketWhenItAlreadyExists() {
        initializer().run(null);

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
        verify(s3Client).putBucketPolicy(any(PutBucketPolicyRequest.class));
        verify(s3Client).putBucketCors(any(PutBucketCorsRequest.class));
    }

    @Test
    void createsBucketWithLocationConstraintWhenMissing() {
        bucketMissing();

        initializer().run(null);

        var captor = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3Client).createBucket(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().createBucketConfiguration().locationConstraintAsString())
            .isEqualTo("ap-southeast-1");
    }

    @Test
    void omitsLocationConstraintForUsEast1() {
        properties.setRegion("us-east-1");
        bucketMissing();

        initializer().run(null);

        var captor = ArgumentCaptor.forClass(CreateBucketRequest.class);
        verify(s3Client).createBucket(captor.capture());
        assertThat(captor.getValue().createBucketConfiguration()).isNull();
    }

    @Test
    void treatsForbiddenHeadBucketAsExistingBucket() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());

        initializer().run(null);

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void reportsActualBucketRegionWhenClientTargetsTheWrongEndpoint() {
        properties.setRegion("ap-southeast-2");
        var redirect = S3Exception.builder()
            .statusCode(301)
            .message("Moved Permanently")
            .awsErrorDetails(AwsErrorDetails.builder()
                .sdkHttpResponse(SdkHttpResponse.builder()
                    .statusCode(301)
                    .putHeader("x-amz-bucket-region", "ap-southeast-1")
                    .build())
                .build())
            .build();
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(redirect);

        assertThatThrownBy(() -> initializer().run(null))
            .isInstanceOf(InfrastructureException.class)
            .hasMessageContaining("ap-southeast-1")
            .hasMessageContaining("ap-southeast-2");

        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void relaxesPublicPolicyBlockButKeepsAclBlocked() {
        initializer().run(null);

        var captor = ArgumentCaptor.forClass(PutPublicAccessBlockRequest.class);
        verify(s3Client).putPublicAccessBlock(captor.capture());
        var config = captor.getValue().publicAccessBlockConfiguration();
        assertThat(config.blockPublicPolicy()).isFalse();
        assertThat(config.restrictPublicBuckets()).isFalse();
        assertThat(config.blockPublicAcls()).isTrue();
        assertThat(config.ignorePublicAcls()).isTrue();
    }

    @Test
    void skipsPublicAccessBlockWhenPublicReadDisabled() {
        properties.setPublicRead(false);

        initializer().run(null);

        verify(s3Client, never()).putPublicAccessBlock(any(PutPublicAccessBlockRequest.class));
    }

    @Test
    void skipsPublicAccessBlockWhenCustomEndpointIsUsed() {
        properties.setEndpoint("http://localhost:9000");

        initializer().run(null);

        verify(s3Client, never()).putPublicAccessBlock(any(PutPublicAccessBlockRequest.class));
    }

    @Test
    void buildsPolicyWithPublicReadAndUploaderStatements() {
        initializer().run(null);

        var captor = ArgumentCaptor.forClass(PutBucketPolicyRequest.class);
        verify(s3Client).putBucketPolicy(captor.capture());

        var policy = jsonMapper.readTree(captor.getValue().policy());
        assertThat(policy.get("Version").asString()).isEqualTo("2012-10-17");

        var statements = policy.get("Statement");
        assertThat(statements).hasSize(2);

        var publicRead = statements.get(0);
        assertThat(publicRead.get("Sid").asString()).isEqualTo("PublicReadObjects");
        assertThat(publicRead.get("Principal").asString()).isEqualTo("*");
        assertThat(publicRead.get("Action").asString()).isEqualTo("s3:GetObject");
        assertThat(publicRead.get("Resource").asString()).isEqualTo("arn:aws:s3:::" + BUCKET + "/*");

        var uploader = statements.get(1);
        assertThat(uploader.get("Sid").asString()).isEqualTo("AllowUploaderPutObject");
        assertThat(uploader.get("Principal").get("AWS").asString()).isEqualTo(UPLOADER_ARN);
        assertThat(uploader.get("Action").asString()).isEqualTo("s3:PutObject");
    }

    @Test
    void omitsUploaderStatementWhenPrincipalArnIsBlank() {
        properties.setUploaderPrincipalArn("");

        initializer().run(null);

        var captor = ArgumentCaptor.forClass(PutBucketPolicyRequest.class);
        verify(s3Client).putBucketPolicy(captor.capture());

        var statements = jsonMapper.readTree(captor.getValue().policy()).get("Statement");
        assertThat(statements).hasSize(1);
        assertThat(statements.get(0).get("Sid").asString()).isEqualTo("PublicReadObjects");
    }

    @Test
    void skipsPolicyWhenNoStatementApplies() {
        properties.setPublicRead(false);
        properties.setUploaderPrincipalArn(null);

        initializer().run(null);

        verify(s3Client, never()).putBucketPolicy(any(PutBucketPolicyRequest.class));
    }

    @Test
    void appliesReadOnlyCorsRule() {
        initializer().run(null);

        var captor = ArgumentCaptor.forClass(PutBucketCorsRequest.class);
        verify(s3Client).putBucketCors(captor.capture());

        var rules = captor.getValue().corsConfiguration().corsRules();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).allowedMethods()).containsExactly("GET", "HEAD");
        assertThat(rules.get(0).allowedOrigins()).containsExactly("*");
        assertThat(rules.get(0).allowedHeaders()).containsExactly("Range");
        assertThat(rules.get(0).maxAgeSeconds()).isEqualTo(3600);
    }

    @Test
    void keepsStartingWhenPolicyStepFailsAndFailOnErrorIsDisabled() {
        when(s3Client.putBucketPolicy(any(PutBucketPolicyRequest.class)))
            .thenThrow(S3Exception.builder().statusCode(403).message("AccessDenied").build());

        assertThatCode(() -> initializer().run(null)).doesNotThrowAnyException();

        // Buoc sau van chay du buoc truoc that bai.
        verify(s3Client).putBucketCors(any(PutBucketCorsRequest.class));
    }

    @Test
    void blocksStartupWhenPolicyStepFailsAndFailOnErrorIsEnabled() {
        properties.setFailOnError(true);
        when(s3Client.putBucketPolicy(any(PutBucketPolicyRequest.class)))
            .thenThrow(S3Exception.builder().statusCode(403).message("AccessDenied").build());

        assertThatThrownBy(() -> initializer().run(null))
            .isInstanceOf(InfrastructureException.class)
            .hasMessageContaining("bucket policy");
    }

    @Test
    void alwaysBlocksStartupWhenBucketCannotBeCreated() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
            .thenThrow(NoSuchBucketException.builder().statusCode(404).message("Not Found").build());
        when(s3Client.createBucket(any(CreateBucketRequest.class)))
            .thenThrow(S3Exception.builder().statusCode(403).message("AccessDenied").build());

        assertThatThrownBy(() -> initializer().run(null))
            .isInstanceOf(InfrastructureException.class)
            .hasMessageContaining("Khong the tao bucket S3");
    }
}

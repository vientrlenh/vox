package com.sep.vox.infrastructure.config;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sep.vox.infrastructure.initializer.S3BucketInitializer;
import com.sep.vox.infrastructure.properties.AwsS3StorageProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(AwsS3StorageProperties.class)
@ConditionalOnProperty(prefix = "storage", name = "provider", havingValue = "s3")
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(AwsS3StorageProperties properties) {
        var builder = S3Client.builder()
            .region(Region.of(properties.getRegion()))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                .build());

        configureCredentialsAndEndpoint(properties, builder);
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "storage.s3", name = "ensure-bucket", havingValue = "true")
    public S3BucketInitializer s3BucketInitializer(
        S3Client s3Client,
        AwsS3StorageProperties properties,
        JsonMapper jsonMapper
    ) {
        return new S3BucketInitializer(s3Client, properties, jsonMapper);
    }

    @Bean
    public S3Presigner s3Presigner(AwsS3StorageProperties properties) {
        var builder = S3Presigner.builder()
            .region(Region.of(properties.getRegion()));

        configureCredentialsAndEndpoint(properties, builder);
        return builder.build();
    }

    private void configureCredentialsAndEndpoint(AwsS3StorageProperties properties, S3ClientBuilder builder) {
        if (hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        if (hasText(properties.getAccessKey()) && hasText(properties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
        }
    }

    private void configureCredentialsAndEndpoint(AwsS3StorageProperties properties, S3Presigner.Builder builder) {
        if (hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        if (hasText(properties.getAccessKey()) && hasText(properties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

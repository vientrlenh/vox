package com.sep.vox.infrastructure.config;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
@EnableConfigurationProperties(AwsS3StorageProperties.class)
@ConditionalOnProperty(prefix = "spring.storage", name = "provider", havingValue = "s3")
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(AwsS3StorageProperties properties) {
        var builder = S3Client.builder()
            .region(Region.of(properties.getRegion()))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(properties.isPathStyleAccessEnabled())
                .build());

        if (hasText(properties.getEndpoint())) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        if (hasText(properties.getAccessKey()) && hasText(properties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

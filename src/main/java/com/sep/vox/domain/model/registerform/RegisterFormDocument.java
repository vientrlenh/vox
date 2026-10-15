package com.sep.vox.domain.model.registerform;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RegisterFormDocument {
    private UUID id;
    private UUID registerFormId;
    private String url;
    private Instant createdAt;

    protected RegisterFormDocument() {}

    public RegisterFormDocument(UUID id, UUID registerFormId, String url, Instant createdAt) {
        this.id = id;
        this.registerFormId = registerFormId;
        this.url = url;
        this.createdAt = createdAt;
    }

    public RegisterFormDocument(Builder builder) {
        this.id = builder.id;
        this.registerFormId = builder.registerFormId;
        this.url = builder.url;
        this.createdAt = builder.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRegisterFormId() {
        return registerFormId;
    }

    public void setRegisterFormId(UUID registerFormId) {
        this.registerFormId = registerFormId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new RegisterFormDocument.Builder();
    }

    public static RegisterFormDocument create(UUID registerFormId, String url) {
        return RegisterFormDocument.builder()
            .registerFormId(registerFormId)
            .url(url)
            .build();
    }

    public static List<RegisterFormDocument> createMany(UUID registerFormId, List<String> urls) {
        if (urls.size() > 10) {
            throw new IllegalArgumentException("Chỉ có thể gửi tối đa 10 tài liệu cho một đơn đăng ký");
        }
        return urls.stream()
            .map(url -> RegisterFormDocument.create(registerFormId, url)).toList();
    }

    public static class Builder {
        private UUID id;
        private UUID registerFormId;
        private String url;
        private Instant createdAt;

        protected Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder registerFormId(UUID registerFormId) {
            this.registerFormId = registerFormId;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public RegisterFormDocument build() {
            return new RegisterFormDocument(this);
        }
    }
}

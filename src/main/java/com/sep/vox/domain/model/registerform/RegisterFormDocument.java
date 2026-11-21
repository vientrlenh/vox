package com.sep.vox.domain.model.registerform;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RegisterFormDocument {
    private UUID id;
    private UUID registerFormId;
    private String url;
    private Instant createdAt;

    public RegisterFormDocument() {}

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

    public static RegisterFormDocument create(UUID registerFormId, String url, Instant now) {
        return builder().registerFormId(registerFormId).url(url).createdAt(now).build();
    }

    public static List<RegisterFormDocument> createMany(UUID registerFormId, List<String> urls, Instant now) {
        if (urls.size() > 10) {
            throw new IllegalArgumentException("Chỉ có thể gửi tối đa 10 tài liệu cho một đơn đăng ký");
        }
        return urls.stream()
            .map(url -> create(registerFormId, url, now))
            .toList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RegisterFormDocument target = new RegisterFormDocument();

        private Builder() {
        }

        public Builder id(UUID id) {
            target.id = id;
            return this;
        }

        public Builder registerFormId(UUID registerFormId) {
            target.registerFormId = registerFormId;
            return this;
        }

        public Builder url(String url) {
            target.url = url;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            target.createdAt = createdAt;
            return this;
        }

        public RegisterFormDocument build() {
            return target;
        }
    }
}

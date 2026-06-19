package com.sep.vox.domain.model.registerform;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class RegisterFormDocument {
    private UUID id;
    private UUID registerFormId;
    private String url;
    private OffsetDateTime createdAt;

    public RegisterFormDocument() {}

    public RegisterFormDocument(UUID id, UUID registerFormId, String url, OffsetDateTime createdAt) {
        this.id = id;
        this.registerFormId = registerFormId;
        this.url = url;
        this.createdAt = createdAt;
    }

    public RegisterFormDocument(UUID registerFormId, String url, OffsetDateTime createdAt) {
        this.registerFormId = registerFormId;
        this.url = url;
        this.createdAt = createdAt;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static RegisterFormDocument create(UUID registerFormId, String url, OffsetDateTime now) {
        return new RegisterFormDocument(registerFormId, url, now);
    }

    public static List<RegisterFormDocument> createMany(UUID registerFormId, List<String> urls, OffsetDateTime now) {
        if (urls.size() > 10) {
            throw new IllegalArgumentException("Chỉ có thể gửi tối đa 10 tài liệu cho một đơn đăng ký");
        }
        return urls.stream()
            .map(url -> new RegisterFormDocument(
                registerFormId, url, now
            )).toList();
    }
}

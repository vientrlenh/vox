package com.sep.vox.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "register_form_documents", indexes = {
    @Index(columnList = "register_form_id", name = "idx_register_form_documents_register_form")
})
public class RegisterFormDocumentJpaEntity {
    
    @Id
    @Generated(event = EventType.INSERT)
    @Column(
        name = "id", 
        nullable = false, 
        updatable = false, 
        insertable = false, 
        columnDefinition = "UUID DEFAULT uuidv7()"
    )
    private UUID id;


    @Column(name = "register_form_id", nullable = false, updatable = false)
    private UUID registerFormId;

    @Column(name = "url", nullable = false, length = 4096)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RegisterFormDocumentJpaEntity() {}

    public RegisterFormDocumentJpaEntity(UUID id, UUID registerFormId, String url, OffsetDateTime createdAt) {
        this.id = id;
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

    
}

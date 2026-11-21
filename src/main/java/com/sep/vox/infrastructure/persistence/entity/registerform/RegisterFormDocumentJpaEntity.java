package com.sep.vox.infrastructure.persistence.entity.registerform;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "register_form_documents", indexes = {
    @Index(columnList = "register_form_id", name = "idx_register_form_documents_register_form")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisterFormDocumentJpaEntity {
    
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "register_form_id", nullable = false, updatable = false)
    private UUID registerFormId;

    @Column(name = "url", nullable = false, length = 4096)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

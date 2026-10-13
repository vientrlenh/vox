package com.sep.vox.infrastructure.persistence.entity;

import java.util.UUID;

import com.sep.vox.infrastructure.shared.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "register_form_documents", indexes = {
    @Index(columnList = "register_form_id", name = "idx_register_form_documents_register_form")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class RegisterFormDocumentJpaEntity extends BaseEntity {

    @Column(name = "register_form_id", nullable = false, updatable = false)
    private UUID registerFormId;

    @Column(name = "url", nullable = false, length = 4096)
    private String url;

}

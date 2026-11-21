package com.sep.vox.infrastructure.persistence.entity.supportedlanguage;

import com.sep.vox.infrastructure.persistence.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "supported_languages", indexes = {
    @Index(columnList = "code", name = "idx_language_code", unique = true),
    @Index(columnList = "name", name = "idx_language_name")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportedLanguageJpaEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 2048)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}

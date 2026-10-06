package com.sep.vox.infrastructure.shared;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass 
@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder 
public abstract class BaseEntity {
    @Id 
    @GeneratedValue 
    @UuidGenerator(style = Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;


    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

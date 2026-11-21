package com.sep.vox.infrastructure.persistence.entity.refreshtoken;

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
@Table(name = "refresh_tokens", indexes = {
    @Index(columnList = "session_id", name = "idx_refresh_token_sessions"),
    @Index(columnList = "token_hash", name = "idx_refresh_token_hash", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenJpaEntity {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 512)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expired_at", nullable = false, updatable = false)
    private Instant expiredAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;
}

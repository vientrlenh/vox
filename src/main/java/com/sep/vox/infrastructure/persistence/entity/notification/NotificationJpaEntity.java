package com.sep.vox.infrastructure.persistence.entity.notification;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notifications", indexes = {
    @Index(columnList = "user_id, created_at", name = "idx_notifications_user_created_at")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "event_id"}, name = "uk_notifications_user_event")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "event_id", updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 150, updatable = false)
    private String eventType;

    @Column(name = "title", nullable = false, length = 255, updatable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT", updatable = false)
    private String body;

    // String + JSONB: Hibernate special-cases String as a pure passthrough, so the payload
    // goes straight into the column without Jackson. Same approach as OutboxJpaEntity.payload.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "JSONB", updatable = false)
    private String payload;

    // The only field allowed to change after creation.
    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

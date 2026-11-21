package com.sep.vox.infrastructure.persistence.entity.subscription;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "school_subscription_events")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolSubscriptionEventJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private UUID schoolId;

    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_school_subscription_events_event_type_valid",
            constraint = "event_type IN ('SUSPENDED', 'UNSUSPENDED')"
        )
    })
    private String eventType;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    // Required for SUSPENDED, optional for UNSUSPENDED -- that cross-column rule lives in the table-level CHECK
    // (chk_school_subscription_events_suspend_has_reason) and cannot be expressed with nullable here.
    @Column(name = "reason", updatable = false, length = 2048)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}

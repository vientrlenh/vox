package com.sep.vox.infrastructure.persistence.entity.outbox;

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
@Table(name = "processed_events", indexes = {
    @Index(columnList = "event_id, consumer_group", name = "idx_processed_events_event_consumer_group", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEventJpaEntity {
    
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "consumer_group", nullable = false, updatable = false)
    private String consumerGroup;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    
}

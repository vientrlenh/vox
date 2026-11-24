package com.sep.vox.infrastructure.persistence.audit;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Supplies the {@code updated_at} and {@code updated_by} values for {@code @Modifying} and native statements, which
 * bypass the JPA auditing hook. The actor follows the same rule as {@link AuditActorProvider}.
 */
@Component
@RequiredArgsConstructor
public class AuditStampProvider {

    private final AuditActorProvider auditActorProvider;

    public Instant currentUpdatedAt() {
        return Instant.now();
    }

    public String currentUpdatedBy() {
        return auditActorProvider.getCurrentAuditor().orElseThrow();
    }
}

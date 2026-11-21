package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.infrastructure.persistence.entity.importfile.ImportSessionJpaEntity;

public final class ImportSessionMapper {

    private ImportSessionMapper() {}

    public static ImportSession toDomain(ImportSessionJpaEntity jpa) {
        return ImportSession.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .type(typeFromString(jpa.getType()))
            .fileName(jpa.getFileName())
            .originalHeadersJson(jpa.getOriginalHeadersJson())
            .suggestedMappingJson(jpa.getSuggestedMappingJson())
            .confirmedMappingJson(jpa.getConfirmedMappingJson())
            .validRows(jpa.getValidRows())
            .invalidRows(jpa.getInvalidRows())
            .importedRows(jpa.getImportedRows())
            .skippedRows(jpa.getSkippedRows())
            .totalRows(jpa.getTotalRows())
            .failureReason(jpa.getFailureReason())
            .status(statusFromString(jpa.getStatus()))
            .importedEntityId(jpa.getImportedEntityId())
            .expiresAt(jpa.getExpiresAt())
            .claimedAt(jpa.getClaimedAt())
            .claimedBy(jpa.getClaimedBy())
            .leaseExpiresAt(jpa.getLeaseExpiresAt())
            .attempts(jpa.getAttempts())
            .createdAt(jpa.getCreatedAt())
            .updatedAt(jpa.getUpdatedAt())
            .createdBy(jpa.getCreatedBy())
            .updatedBy(jpa.getUpdatedBy())
            .build();
    }

    public static ImportSessionJpaEntity toJpa(ImportSession session) {
        return ImportSessionJpaEntity.builder()
            .id(session.getId())
            .schoolId(session.getSchoolId())
            .type(valueOf(session.getType()))
            .fileName(session.getFileName())
            .originalHeadersJson(session.getOriginalHeadersJson())
            .suggestedMappingJson(session.getSuggestedMappingJson())
            .confirmedMappingJson(session.getConfirmedMappingJson())
            .validRows(session.getValidRows())
            .invalidRows(session.getInvalidRows())
            .importedRows(session.getImportedRows())
            .skippedRows(session.getSkippedRows())
            .totalRows(session.getTotalRows())
            .failureReason(session.getFailureReason())
            .status(valueOf(session.getStatus()))
            .importedEntityId(session.getImportedEntityId())
            .expiresAt(session.getExpiresAt())
            .claimedAt(session.getClaimedAt())
            .claimedBy(session.getClaimedBy())
            .leaseExpiresAt(session.getLeaseExpiresAt())
            .attempts(session.getAttempts())
            .createdAt(session.getCreatedAt())
            .updatedAt(session.getUpdatedAt())
            .createdBy(session.getCreatedBy())
            .updatedBy(session.getUpdatedBy())
            .build();
    }

    private static ImportType typeFromString(String type) {
        return type == null ? null : ImportType.valueOf(type);
    }

    private static ImportSessionStatus statusFromString(String status) {
        return status == null ? null : ImportSessionStatus.valueOf(status);
    }

    private static String valueOf(ImportType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(ImportSessionStatus status) {
        return status == null ? null : status.name();
    }
}

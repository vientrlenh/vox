package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
import com.sep.vox.infrastructure.persistence.entity.ImportSessionJpaEntity;

public final class ImportSessionMapper {

    private ImportSessionMapper() {}

    public static ImportSession toDomain(ImportSessionJpaEntity jpa) {
        return new ImportSession(
            jpa.getId(),
            jpa.getSchoolId(),
            typeFromString(jpa.getType()),
            jpa.getFileName(),
            jpa.getOriginalHeadersJson(),
            jpa.getSuggestedMappingJson(),
            jpa.getConfirmedMappingJson(),
            jpa.getValidRows(),
            jpa.getInvalidRows(),
            jpa.getImportedRows(),
            jpa.getSkippedRows(),
            jpa.getTotalRows(),
            jpa.getFailureReason(),
            statusFromString(jpa.getStatus()),
            jpa.getImportedEntityId(),
            jpa.getExpiresAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getCreatedBy(),
            jpa.getUpdatedBy()
        );
    }

    public static ImportSessionJpaEntity toJpa(ImportSession session) {
        return new ImportSessionJpaEntity(
            session.getId(),
            session.getSchoolId(),
            valueOf(session.getType()),
            session.getFileName(),
            session.getOriginalHeadersJson(),
            session.getSuggestedMappingJson(),
            session.getConfirmedMappingJson(),
            session.getValidRows(),
            session.getInvalidRows(),
            session.getImportedRows(),
            session.getSkippedRows(),
            session.getTotalRows(),
            session.getFailureReason(),
            valueOf(session.getStatus()),
            session.getImportedEntityId(),
            session.getExpiresAt(),
            session.getCreatedAt(),
            session.getUpdatedAt(),
            session.getCreatedBy(),
            session.getUpdatedBy()
        );
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

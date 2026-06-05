package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.infrastructure.persistence.entity.ImportRowJpaEntity;

public final class ImportRowMapper {

    private ImportRowMapper() {}

    public static ImportRow toDomain(ImportRowJpaEntity jpa) {
        return new ImportRow(
            jpa.getId(),
            jpa.getSessionId(),
            jpa.getRowNumber(),
            jpa.getRawDataJson(),
            jpa.getMappedDataJson(),
            jpa.getErrorsJson(),
            statusFromString(jpa.getStatus())
        );
    }

    public static ImportRowJpaEntity toJpa(ImportRow row) {
        return new ImportRowJpaEntity(
            row.getId(),
            row.getSessionId(),
            row.getRowNumber(),
            row.getRawDataJson(),
            row.getMappedDataJson(),
            row.getErrorsJson(),
            valueOf(row.getStatus())
        );
    }

    private static ImportRowStatus statusFromString(String status) {
        return status == null ? null : ImportRowStatus.valueOf(status);
    }

    private static String valueOf(ImportRowStatus status) {
        return status == null ? null : status.name();
    }
}

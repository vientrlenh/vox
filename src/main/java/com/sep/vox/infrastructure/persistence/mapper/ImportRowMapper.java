package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.infrastructure.persistence.entity.importfile.ImportRowJpaEntity;

public final class ImportRowMapper {

    private ImportRowMapper() {}

    public static ImportRow toDomain(ImportRowJpaEntity jpa) {
        return ImportRow.builder()
            .id(jpa.getId())
            .sessionId(jpa.getSessionId())
            .rowNumber(jpa.getRowNumber())
            .rawDataJson(jpa.getRawDataJson())
            .mappedDataJson(jpa.getMappedDataJson())
            .errorsJson(jpa.getErrorsJson())
            .status(statusFromString(jpa.getStatus()))
            .build();
    }

    public static ImportRowJpaEntity toJpa(ImportRow row) {
        return ImportRowJpaEntity.builder()
            .id(row.getId())
            .sessionId(row.getSessionId())
            .rowNumber(row.getRowNumber())
            .rawDataJson(row.getRawDataJson())
            .mappedDataJson(row.getMappedDataJson())
            .errorsJson(row.getErrorsJson())
            .status(valueOf(row.getStatus()))
            .build();
    }

    private static ImportRowStatus statusFromString(String status) {
        return status == null ? null : ImportRowStatus.valueOf(status);
    }

    private static String valueOf(ImportRowStatus status) {
        return status == null ? null : status.name();
    }
}

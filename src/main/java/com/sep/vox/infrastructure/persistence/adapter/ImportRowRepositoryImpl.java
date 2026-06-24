package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.ImportRowJpaEntity;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportRow;
import com.sep.vox.domain.model.importfile.ImportRowStatus;
import com.sep.vox.domain.repository.ImportRowRepository;
import com.sep.vox.infrastructure.persistence.mapper.ImportRowMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataImportRowRepository;

@Repository
public class ImportRowRepositoryImpl implements ImportRowRepository {

    private final SpringDataImportRowRepository springDataImportRowRepository;

    public ImportRowRepositoryImpl(SpringDataImportRowRepository springDataImportRowRepository) {
        this.springDataImportRowRepository = springDataImportRowRepository;
    }

    @Override
    public List<ImportRow> findBySessionIdOrderByRowNumber(UUID sessionId) {
        return springDataImportRowRepository.findBySessionIdOrderByRowNumber(sessionId)
            .stream()
            .map(ImportRowMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<ImportRow> findBySessionId(UUID sessionId, ImportRowStatus status, int pageNumber, int size) {
        var sort = Sort.by(Sort.Order.asc(ImportRowJpaEntity::getRowNumber));
        var pageable = PageRequest.of(
            pageNumber - 1,
            size,
            sort
        );
        var page = springDataImportRowRepository.findBySessionIdWithFilters(
            sessionId,
            valueOf(status),
            pageable
        );
        var content = page.getContent()
            .stream()
            .map(ImportRowMapper::toDomain)
            .toList();
        return new PageResult<>(
            content,
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public List<ImportRow> saveAll(List<ImportRow> rows) {
        var entities = rows.stream()
            .map(ImportRowMapper::toJpa)
            .toList();
        return springDataImportRowRepository.saveAll(entities)
            .stream()
            .map(ImportRowMapper::toDomain)
            .toList();
    }

    private static String valueOf(ImportRowStatus status) {
        return status == null ? null : status.name();
    }

    @Override
    public List<ImportRow> findBySessionId(UUID sessionId) {
        var entities = springDataImportRowRepository.findBySessionId(sessionId);
        return entities.stream()
            .map(ImportRowMapper::toDomain)
            .toList();
    }
}

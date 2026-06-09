package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
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
    public PageResult<ImportRow> findBySessionId(UUID sessionId, ImportRowStatus status, PageRequest pageRequest) {
        var sort = Sort.by(Sort.Order.asc("rowNumber"));
        var pageable = org.springframework.data.domain.PageRequest.of(
            pageRequest.page() - 1,
            pageRequest.size(),
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
            pageRequest.page(),
            pageRequest.size(),
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
}

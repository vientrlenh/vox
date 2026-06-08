package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.importfile.ImportRow;
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
    public List<ImportRow> saveAll(List<ImportRow> rows) {
        var entities = rows.stream()
            .map(ImportRowMapper::toJpa)
            .toList();
        return springDataImportRowRepository.saveAll(entities)
            .stream()
            .map(ImportRowMapper::toDomain)
            .toList();
    }
}

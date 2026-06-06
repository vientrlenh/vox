package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
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
    public ImportRow save(ImportRow row) {
        var entity = ImportRowMapper.toJpa(row);
        var saved = springDataImportRowRepository.save(entity);
        return ImportRowMapper.toDomain(saved);
    }

    @Override
    public List<ImportRow> saveAll(Collection<ImportRow> rows) {
        return springDataImportRowRepository.saveAll(rows.stream().map(ImportRowMapper::toJpa).toList())
            .stream()
            .map(ImportRowMapper::toDomain)
            .toList();
    }

    @Override
    public List<ImportRow> findBySessionIdOrderByRowNumberAsc(UUID sessionId) {
        return springDataImportRowRepository.findBySessionIdOrderByRowNumberAsc(sessionId)
            .stream()
            .map(ImportRowMapper::toDomain)
            .toList();
    }
}

package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.repository.ImportSessionRepository;
import com.sep.vox.infrastructure.persistence.mapper.ImportSessionMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataImportSessionRepository;

@Repository
public class ImportSessionRepositoryImpl implements ImportSessionRepository {

    private final SpringDataImportSessionRepository springDataImportSessionRepository;

    public ImportSessionRepositoryImpl(SpringDataImportSessionRepository springDataImportSessionRepository) {
        this.springDataImportSessionRepository = springDataImportSessionRepository;
    }

    @Override
    public ImportSession save(ImportSession session) {
        var entity = ImportSessionMapper.toJpa(session);
        var saved = springDataImportSessionRepository.save(entity);
        return ImportSessionMapper.toDomain(saved);
    }

    @Override
    public Optional<ImportSession> findById(UUID id) {
        return springDataImportSessionRepository.findById(id)
            .map(ImportSessionMapper::toDomain);
    }

    @Override
    public Optional<ImportSession> findByIdAndSchoolId(UUID id, UUID schoolId) {
        return springDataImportSessionRepository.findByIdAndSchoolId(id, schoolId)
            .map(ImportSessionMapper::toDomain);
    }
}

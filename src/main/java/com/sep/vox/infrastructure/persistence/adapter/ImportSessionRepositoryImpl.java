package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.common.PageRequest;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.importfile.ImportSession;
import com.sep.vox.domain.model.importfile.ImportSessionStatus;
import com.sep.vox.domain.model.importfile.ImportType;
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
        var saved = springDataImportSessionRepository.saveAndFlush(entity);
        return ImportSessionMapper.toDomain(saved);
    }

    @Override
    public Optional<ImportSession> findById(UUID id) {
        return springDataImportSessionRepository.findById(id)
            .map(ImportSessionMapper::toDomain);
    }

    @Override
    public PageResult<ImportSession> findBySchoolId(UUID schoolId, ImportType type, ImportSessionStatus status, PageRequest pageRequest) {
        var pageable = org.springframework.data.domain.PageRequest.of(
            pageRequest.page() - 1,
            pageRequest.size(),
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        var page = springDataImportSessionRepository.findBySchoolIdWithFilters(
            schoolId,
            valueOf(type),
            valueOf(status),
            pageable
        );
        return new PageResult<>(
            page.getContent().stream()
                .map(ImportSessionMapper::toDomain)
                .toList(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    private static String valueOf(ImportType type) {
        return type == null ? null : type.name();
    }

    private static String valueOf(ImportSessionStatus status) {
        return status == null ? null : status.name();
    }
}

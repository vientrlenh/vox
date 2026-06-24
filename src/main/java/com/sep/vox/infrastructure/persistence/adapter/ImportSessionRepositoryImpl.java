package com.sep.vox.infrastructure.persistence.adapter;

import com.sep.vox.infrastructure.persistence.entity.ImportSessionJpaEntity;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

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
    public PageResult<ImportSession> findBySchoolId(UUID schoolId, ImportType type, ImportSessionStatus status, int pageNumber, int size) {
        var pageable = PageRequest.of(
            pageNumber - 1,
            size,
            Sort.by(Sort.Direction.DESC, ImportSessionJpaEntity::getCreatedAt).and(Sort.by(Sort.Direction.DESC, ImportSessionJpaEntity::getId))
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
            pageNumber,
            size,
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

    @Override
    public int markImporting(UUID id, String type, String confirmedMapping, OffsetDateTime now, UUID updatedBy) {
        return springDataImportSessionRepository.markImporting(id, type, confirmedMapping, now, updatedBy);
    }

    @Override
    public int markQueued(UUID id, String type, String confirmedMapping, OffsetDateTime now, UUID updatedBy) {
        return springDataImportSessionRepository.markQueued(id, type, confirmedMapping, now, updatedBy);
    }

    @Override
    public int markClaimed(Collection<UUID> ids, UUID worker, OffsetDateTime now, OffsetDateTime leaseUntil) {
        return springDataImportSessionRepository.markClaimed(ids, worker, now, leaseUntil);
    }

    @Override
    public List<UUID> lockQueueIds(int limit) {
        return springDataImportSessionRepository.lockQueueIds(limit);
    }

    @Override
    public void extendLease(UUID id, OffsetDateTime leaseUntil) {
        springDataImportSessionRepository.extendLease(id, leaseUntil);
    }

    @Override
    public int requeueExpiredLeases(OffsetDateTime now, int maxAttempts) {
        return springDataImportSessionRepository.requeueExpiredLeases(now, maxAttempts);
    }
}

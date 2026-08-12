package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.PracticePaper;
import com.sep.vox.domain.repository.PracticePaperRepository;
import com.sep.vox.infrastructure.persistence.mapper.personalization.PracticePaperMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticePaperRepository;

@Repository
public class PracticePaperRepositoryImpl implements PracticePaperRepository {

    private final SpringDataPracticePaperRepository repository;

    public PracticePaperRepositoryImpl(SpringDataPracticePaperRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PracticePaper> findReservedPaper(UUID paperId, UUID studentId, Instant now) {
        return repository
            .findByIdAndStudentIdAndStatusAndReservationExpiresAtAfter(paperId, studentId, "RESERVED", now)
            .map(PracticePaperMapper::toDomain);
    }

    @Override
    public PracticePaper save(PracticePaper paper) {
        return PracticePaperMapper.toDomain(repository.save(PracticePaperMapper.toJpa(paper)));
    }

    @Override
    public int countRecentEpsilonPapers(UUID studentId) {
        return repository.countRecentEpsilonPapers(studentId);
    }

    @Override
    public int sumReservedQuotaSeconds(UUID studentId) {
        return repository.sumReservedQuotaSeconds(studentId);
    }
}

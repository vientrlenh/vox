package com.sep.vox.infrastructure.persistence.adapter;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolAiSpendEntry;
import com.sep.vox.domain.repository.SchoolAiSpendEntryRepository;
import com.sep.vox.infrastructure.persistence.entity.SchoolAiSpendEntryJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolAiSpendEntryRepository;

@Repository
public class SchoolAiSpendEntryRepositoryImpl implements SchoolAiSpendEntryRepository {

    private final SpringDataSchoolAiSpendEntryRepository springDataSchoolAiSpendEntryRepository;

    public SchoolAiSpendEntryRepositoryImpl(
            SpringDataSchoolAiSpendEntryRepository springDataSchoolAiSpendEntryRepository) {
        this.springDataSchoolAiSpendEntryRepository = springDataSchoolAiSpendEntryRepository;
    }

    /**
     * No separate mapper: the table is append-only and the read path does not go through this repository, so the
     * entity -> domain direction would have no caller. Building such a function up front would be building dead code.
     */
    @Override
    public SchoolAiSpendEntry save(SchoolAiSpendEntry entry) {
        SchoolAiSpendEntryJpaEntity saved = springDataSchoolAiSpendEntryRepository.save(new SchoolAiSpendEntryJpaEntity(
            entry.getSchoolId(),
            entry.getSubscriptionId(),
            entry.getQuotaType().name(),
            entry.getUserId(),
            entry.getAssessmentSessionId(),
            entry.getPracticeSessionId(),
            entry.getAmountVnd(),
            entry.getOccurredAt()
        ));
        entry.setId(saved.getId());
        return entry;
    }
}

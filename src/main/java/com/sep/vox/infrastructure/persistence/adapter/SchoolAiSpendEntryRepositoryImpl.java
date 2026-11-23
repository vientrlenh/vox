package com.sep.vox.infrastructure.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolAiSpendEntry;
import com.sep.vox.domain.repository.SchoolAiSpendEntryRepository;
import com.sep.vox.infrastructure.persistence.entity.school.SchoolAiSpendEntryJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolAiSpendEntryRepository;

@Repository
@RequiredArgsConstructor
public class SchoolAiSpendEntryRepositoryImpl implements SchoolAiSpendEntryRepository {

    private final SpringDataSchoolAiSpendEntryRepository springDataSchoolAiSpendEntryRepository;

    /**
     * No separate mapper: the table is append-only and the read path does not go through this repository, so the
     * entity -> domain direction would have no caller. Building such a function up front would be building dead code.
     */
    @Override
    public SchoolAiSpendEntry save(SchoolAiSpendEntry entry) {
        SchoolAiSpendEntryJpaEntity saved = springDataSchoolAiSpendEntryRepository.save(SchoolAiSpendEntryJpaEntity.builder()
            .schoolId(entry.getSchoolId())
            .subscriptionId(entry.getSubscriptionId())
            .quotaType(entry.getQuotaType().name())
            .userId(entry.getUserId())
            .assessmentSessionId(entry.getAssessmentSessionId())
            .practiceSessionId(entry.getPracticeSessionId())
            .amountVnd(entry.getAmountVnd())
            .occurredAt(entry.getOccurredAt())
            .build());
        entry.setId(saved.getId());
        return entry;
    }
}

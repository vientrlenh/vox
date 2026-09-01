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
     * Không có mapper riêng: bảng chỉ-ghi-thêm và đường đọc không đi qua repository này, nên chiều
     * entity -> domain sẽ không có ai gọi. Dựng sẵn một hàm như thế là dựng sẵn mã chết.
     */
    @Override
    public SchoolAiSpendEntry save(SchoolAiSpendEntry entry) {
        var saved = springDataSchoolAiSpendEntryRepository.save(new SchoolAiSpendEntryJpaEntity(
            entry.getSchoolId(),
            entry.getSubscriptionId(),
            entry.getQuotaType().name(),
            entry.getUserId(),
            entry.getExamSessionId(),
            entry.getPracticeSessionId(),
            entry.getAmountVnd(),
            entry.getOccurredAt()
        ));
        entry.setId(saved.getId());
        return entry;
    }
}

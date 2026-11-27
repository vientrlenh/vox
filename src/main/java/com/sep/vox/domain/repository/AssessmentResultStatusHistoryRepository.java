package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentResultStatusHistory;

/** Log of assessment result status changes — append and read only, never edited or deleted. */
public interface AssessmentResultStatusHistoryRepository {

    AssessmentResultStatusHistory save(AssessmentResultStatusHistory history);

    List<AssessmentResultStatusHistory> saveAll(List<AssessmentResultStatusHistory> histories);

    /** Timeline of a result, oldest first — the natural reading order for a viewer. */
    List<AssessmentResultStatusHistory> findByCandidateResultIdOrderByCreatedAtAsc(UUID candidateResultId);

    /** Batch for list screens; call once instead of looping per result. */
    List<AssessmentResultStatusHistory> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /** Only used when an assessment session is deleted outright; not a way to edit the log. */
    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);
}

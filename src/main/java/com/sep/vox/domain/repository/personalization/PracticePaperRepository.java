package com.sep.vox.domain.repository.personalization;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.PracticePaper;

public interface PracticePaperRepository {

    Optional<PracticePaper> findReservedPaper(UUID paperId, UUID studentId, Instant now);

    PracticePaper save(PracticePaper paper);

    int countRecentEpsilonPapers(UUID studentId);

    int sumReservedQuotaSeconds(UUID studentId);
}

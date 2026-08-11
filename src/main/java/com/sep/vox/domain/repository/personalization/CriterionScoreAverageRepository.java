package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;


public interface CriterionScoreAverageRepository {


    List<String> findCriterionCodesOrderedByLowestAverageScore(UUID studentId);
}

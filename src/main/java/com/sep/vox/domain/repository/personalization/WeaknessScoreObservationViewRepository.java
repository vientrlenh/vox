package com.sep.vox.domain.repository.personalization;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.personalization.WeaknessScoreObservation;

public interface WeaknessScoreObservationViewRepository {

    List<WeaknessScoreObservation> findAllValidScoreObservations();

    List<UUID> findStudentsNeedingRefresh(Instant staleBefore, int limit);
}

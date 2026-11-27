package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;

import com.sep.vox.domain.model.personalization.InterestDimension;

public interface InterestDimensionRepository {

    /** The whole catalog, including disabled entries -- for the admin screen. */
    List<InterestDimension> findAll();

    /** Dimensions still in effect, used to rank topics (including ACADEMIC_ASSESSMENT). */
    List<InterestDimension> findActive();

    /** Dimensions allowed to be put to the student in the interest quiz -- NOT including ACADEMIC_ASSESSMENT. */
    List<InterestDimension> findQuizEligible();

    Optional<InterestDimension> findByCode(String code);

    InterestDimension save(InterestDimension dimension);

    /** Soft disable -- keeps the historical scores/topics already assigned to this dimension. */
    void deactivate(String code);
}

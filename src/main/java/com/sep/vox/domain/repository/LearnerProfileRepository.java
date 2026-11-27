package com.sep.vox.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.personalization.LearnerProfile;

public interface LearnerProfileRepository {

    Optional<LearnerProfile> findCurrent(UUID studentId);

    Optional<LearnerProfile> findCurrentForUpdate(UUID studentId);

    LearnerProfile save(LearnerProfile profile);

    /** Number of bands of the proficiency scale in use (VSTEP 6, CEFR 6, IELTS 9...) -- empty if there is no
     * scoring policy yet. Read from data instead of assuming a hard-coded 6 bands. */
    List<Integer> findFrameworkBandCount(UUID frameworkVersionId);

    /** The whole band scale (order/code/description) of the framework in use -- to describe the scale to the LLM. */
    List<FrameworkResultBand> findFrameworkBandLadder(UUID frameworkVersionId);
}

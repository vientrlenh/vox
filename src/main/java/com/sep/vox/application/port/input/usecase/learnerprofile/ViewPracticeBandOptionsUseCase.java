package com.sep.vox.application.port.input.usecase.learnerprofile;

import com.sep.vox.domain.model.framework.FrameworkResultBand;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeBandOption;

/**
 * The band ladder of the framework in force, for the difficulty picker before every practice session.
 *
 * <p>Previously the student chose nothing: the system inferred the band from class-test grading plus recent
 * practice performance, then built the paper for that band. Inferring that way treats the difficulty of a QUESTION
 * as the level of the LEARNER -- a claim the system has no basis to make. Now it only lists the bands with their
 * descriptions and lets the student choose.
 */
@Service
@RequiredArgsConstructor
public class ViewPracticeBandOptionsUseCase implements IUseCase<Void, List<PracticeBandOption>> {

    private final PracticeTopicOfferEnrichmentService enrichmentService;

    @Override
    @Transactional(readOnly = true)
    public List<PracticeBandOption> execute(Void input) {
        return execute((UUID) null);
    }

    /**
     * The band ladder of ONE specific framework, after the student has chosen the framework in the picker above.
     *
     * <p>{@code null} = keep the old behaviour: take the framework in force system-wide. Old clients that do not send
     * the parameter keep working exactly as before.
     */
    @Transactional(readOnly = true)
    public List<PracticeBandOption> execute(UUID frameworkVersionId) {
        // The band ladder is a SYSTEM-WIDE concept, independent of any student -- so no
        // studentId is needed here. Keeping strangers out is the job of @PreAuthorize("hasRole('STUDENT')") on
        // GraphQLPracticeController, not of calling UserContextPort and throwing the result away.
        List<FrameworkResultBand> ladder = frameworkVersionId == null
            ? enrichmentService.frameworkBandLadder()
            : enrichmentService.frameworkBandLadder(frameworkVersionId);
        return ladder.stream()
            .map(band -> new PracticeBandOption(
                band.getId(),
                band.getCode(),
                band.getLabel(),
                band.getDescription(),
                band.getOrder()
            ))
            .toList();
    }
}

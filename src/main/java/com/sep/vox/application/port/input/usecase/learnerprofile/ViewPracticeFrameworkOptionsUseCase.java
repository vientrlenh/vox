package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeFrameworkOption;

/**
 * The assessment frameworks a student may choose before choosing a practice band.
 *
 * <p>Each framework shows only its LATEST PUBLISHED VERSION, picked by the server -- the student does not need to
 * know the concept of versions. Drafts and expired versions do not leak out, because the query uses exactly the same
 * conditions as {@code findActiveVersionId}.
 *
 * <p>This list is a SYSTEM-WIDE concept, independent of which student asks -- same reason as
 * {@link ViewPracticeBandOptionsUseCase}: keeping strangers out is the job of {@code @PreAuthorize} on the
 * controller, not of accepting a studentId and then discarding it.
 */
@Service
@RequiredArgsConstructor
public class ViewPracticeFrameworkOptionsUseCase implements IUseCase<Void, List<PracticeFrameworkOption>> {

    private final PracticeTopicOfferEnrichmentService enrichmentService;

    @Override
    @Transactional(readOnly = true)
    public List<PracticeFrameworkOption> execute(Void input) {
        return enrichmentService.activeFrameworks().stream()
            .map(framework -> new PracticeFrameworkOption(
                framework.versionId(),
                framework.frameworkCode(),
                framework.frameworkName(),
                framework.frameworkDescription(),
                enrichmentService.frameworkBandCount(framework.versionId())
            ))
            .toList();
    }
}

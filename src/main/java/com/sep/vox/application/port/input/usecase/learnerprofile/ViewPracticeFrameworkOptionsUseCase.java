package com.sep.vox.application.port.input.usecase.learnerprofile;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.PracticeFrameworkOption;
import com.sep.vox.domain.valueobject.Code;


@Service
public class ViewPracticeFrameworkOptionsUseCase implements IUseCase<Void, List<PracticeFrameworkOption>> {

    private final PracticeTopicOfferEnrichmentService enrichmentService;

    public ViewPracticeFrameworkOptionsUseCase(PracticeTopicOfferEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeFrameworkOption> execute(Void input) {
        return enrichmentService.activeFrameworks().stream()
            .map(framework -> new PracticeFrameworkOption(
                framework.getId(),
                Code.valueOf(framework.frameworkCode()),
                framework.frameworkName(),
                framework.frameworkDescription(),
                enrichmentService.frameworkBandCount(framework.versionId())
            ))
            .toList();
    }
}

package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;

@Service
public class PickRandomTopicUseCase implements IUseCase<Void, PracticeTopicOffer> {

    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final UserContextPort userContextPort;

    public PickRandomTopicUseCase(
            PracticeTopicQueryRepository practiceTopicQueryRepository,
            PracticeTopicOfferEnrichmentService enrichmentService,
            UserContextPort userContextPort) {
        this.practiceTopicQueryRepository = practiceTopicQueryRepository;
        this.enrichmentService = enrichmentService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeTopicOffer execute(Void input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        var row = practiceTopicQueryRepository.findRandomActiveTopic(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Kho chủ đề đang trống."));
        return new PracticeTopicOffer(
            row.getId(),
            row.getName(),
            row.getInterestDimension(),
            row.getSavedByMe(),
            null,
            enrichmentService.minutesForStudent(studentId),
            null,
            List.of()
        );
    }
}

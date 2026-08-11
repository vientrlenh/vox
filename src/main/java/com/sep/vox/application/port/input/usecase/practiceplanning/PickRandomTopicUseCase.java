package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
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
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var row = practiceTopicQueryRepository.findRandomActiveTopic(studentId)
            .orElseThrow(() -> new NotFoundException("Kho chủ đề đang trống."));
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

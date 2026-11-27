package com.sep.vox.application.port.input.usecase.practiceplanning;

import com.sep.vox.application.query.dto.TopicSearchRowInfo;
import java.util.List;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.service.PracticeTopicOfferEnrichmentService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeTopicQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticeTopicOffer;

@Service
@RequiredArgsConstructor
public class PickRandomTopicUseCase implements IUseCase<Void, PracticeTopicOffer> {

    private final PracticeTopicQueryRepository practiceTopicQueryRepository;
    private final PracticeTopicOfferEnrichmentService enrichmentService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public PracticeTopicOffer execute(Void input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        TopicSearchRowInfo row = practiceTopicQueryRepository.findRandomActiveTopic(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("The topic pool is empty."));
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

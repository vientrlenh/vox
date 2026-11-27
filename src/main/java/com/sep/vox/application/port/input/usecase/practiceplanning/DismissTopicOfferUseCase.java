package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.practiceplanning.DismissTopicOfferCommand;
import com.sep.vox.application.port.input.service.InterestVectorService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.PracticeTopicRepository;


@Service
@RequiredArgsConstructor
public class DismissTopicOfferUseCase implements IUseCase<DismissTopicOfferCommand, Boolean> {

    /** Same value as "abandoned out of boredom, default origin" in InterestVectorService. */
    private static final double BORED_SIGNAL = 0.15;
    private static final String EVENT_TYPE = "TOPIC_DISMISSED";

    private final PracticeTopicRepository practiceTopicRepository;
    private final InterestVectorService interestVectorService;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public Boolean execute(DismissTopicOfferCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId();
        if (!practiceTopicRepository.existsActiveById(input.topicId())) {
            throw new ResourceNotFoundException("Practice topic not found.");
        }
        interestVectorService.appendInterestEvent(
            studentId, input.topicId(), null, EVENT_TYPE, BORED_SIGNAL
        );
        interestVectorService.recomputeInterest(studentId);
        return true;
    }
}

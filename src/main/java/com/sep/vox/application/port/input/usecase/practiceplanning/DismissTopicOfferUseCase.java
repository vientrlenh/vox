package com.sep.vox.application.port.input.usecase.practiceplanning;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.DismissTopicOfferCommand;
import com.sep.vox.application.port.input.service.InterestVectorService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.PracticeTopicRepository;


@Service
public class DismissTopicOfferUseCase implements IUseCase<DismissTopicOfferCommand, Boolean> {

    /** Cùng giá trị với "bỏ dở vì chán, origin mặc định" trong InterestVectorService. */
    private static final double BORED_SIGNAL = 0.15;
    private static final String EVENT_TYPE = "TOPIC_DISMISSED";

    private final PracticeTopicRepository practiceTopicRepository;
    private final InterestVectorService interestVectorService;
    private final UserContextPort userContextPort;

    public DismissTopicOfferUseCase(
            PracticeTopicRepository practiceTopicRepository,
            InterestVectorService interestVectorService,
            UserContextPort userContextPort) {
        this.practiceTopicRepository = practiceTopicRepository;
        this.interestVectorService = interestVectorService;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Boolean execute(DismissTopicOfferCommand input) {
        UUID studentId = userContextPort.getCurrentAuthenticatedUserId()
            .orElseThrow(() -> new UnauthorizedException("User is not logged in"));
        if (!practiceTopicRepository.existsActiveById(input.topicId())) {
            throw new ResourceNotFoundException("Không tìm thấy chủ đề luyện tập.");
        }
        interestVectorService.appendInterestEvent(
            studentId, input.topicId(), null, EVENT_TYPE, BORED_SIGNAL
        );
        interestVectorService.recomputeInterest(studentId);
        return true;
    }
}

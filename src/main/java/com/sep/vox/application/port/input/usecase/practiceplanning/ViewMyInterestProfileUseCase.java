package com.sep.vox.application.port.input.usecase.practiceplanning;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.TopicInterestScoreQueryRepository;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.InterestProfile;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.TopicInterest;

@Service
public class ViewMyInterestProfileUseCase implements IUseCase<Void, InterestProfile> {

    private final TopicInterestScoreQueryRepository topicInterestScoreQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyInterestProfileUseCase(
            TopicInterestScoreQueryRepository topicInterestScoreQueryRepository,
            UserContextPort userContextPort) {
        this.topicInterestScoreQueryRepository = topicInterestScoreQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public InterestProfile execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        var topics = topicInterestScoreQueryRepository.findInterestProfileRows(studentId).stream()
            .map(row -> new TopicInterest(
                row.getId(),
                row.getName(),
                row.getScore(),
                row.getSessionsMentioned(),
                row.getLastMentionedAt() == null ? null : row.getLastMentionedAt().toString()
            ))
            .toList();
        return new InterestProfile(topics);
    }
}

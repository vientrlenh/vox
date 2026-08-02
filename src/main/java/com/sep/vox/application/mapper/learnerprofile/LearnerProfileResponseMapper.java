package com.sep.vox.application.mapper.learnerprofile;

import java.util.List;

import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.InterestQuizItem;
import com.sep.vox.application.response.input.learnerprofile.LearnerProfileResponses.LearnerProfile;
import com.sep.vox.application.query.dto.LearnerProfileInfo;
import com.sep.vox.domain.model.personalization.InterestQuizSeedItem;

public final class LearnerProfileResponseMapper {

    private LearnerProfileResponseMapper() {
    }

    public static LearnerProfile toResponse(LearnerProfileInfo dto) {
        if (dto == null) {
            return null;
        }
        return new LearnerProfile(
            dto.goalType(),
            dto.flsaScore(),
            dto.targetFrameworkBandCode(),
            dto.targetFrameworkBandLabel(),
            dto.targetBandAttainmentPercent(),
            dto.estimatedFrameworkBandCode(),
            dto.interestAutoUpdateEnabled(),
            dto.quizCompletedAt()
        );
    }

    public static List<InterestQuizItem> toResponse(
            List<InterestQuizSeedItem> items) {
        return items.stream()
            .map(item -> new InterestQuizItem(item.getId(), item.getStatements()))
            .toList();
    }
}

package com.sep.vox.application.port.input.usecase.practiceinsights;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewMyPracticeProgressQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeInsightsQueryRepository;
import com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionProgressPoint;

@Service
public class ViewMyPracticeProgressUseCase implements IUseCase<ViewMyPracticeProgressQuery, List<CriterionProgressPoint>> {

    private final PracticeInsightsQueryRepository practiceInsightsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyPracticeProgressUseCase(
            PracticeInsightsQueryRepository practiceInsightsQueryRepository,
            UserContextPort userContextPort) {
        this.practiceInsightsQueryRepository = practiceInsightsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriterionProgressPoint> execute(ViewMyPracticeProgressQuery input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return practiceInsightsQueryRepository.progress(studentId, input.criterionCode(), input.days());
    }
}

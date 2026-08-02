package com.sep.vox.application.port.input.usecase.practiceinsights;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewClassPracticeOverviewQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeInsightsQueryRepository;
import com.sep.vox.application.response.input.practiceinsights.PracticeInsights.ClassPracticeOverview;

@Service
public class ViewClassPracticeOverviewUseCase implements IUseCase<ViewClassPracticeOverviewQuery, ClassPracticeOverview> {

    private final PracticeInsightsQueryRepository practiceInsightsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewClassPracticeOverviewUseCase(
            PracticeInsightsQueryRepository practiceInsightsQueryRepository,
            UserContextPort userContextPort) {
        this.practiceInsightsQueryRepository = practiceInsightsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public ClassPracticeOverview execute(ViewClassPracticeOverviewQuery input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        practiceInsightsQueryRepository.requireTeacherCanReadClass(teacherId, input.classId());
        return practiceInsightsQueryRepository.classOverview(input.classId());
    }
}

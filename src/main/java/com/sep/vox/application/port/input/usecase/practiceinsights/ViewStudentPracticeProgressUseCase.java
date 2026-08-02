package com.sep.vox.application.port.input.usecase.practiceinsights;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewStudentPracticeProgressQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeInsightsQueryRepository;
import com.sep.vox.application.response.input.practiceinsights.PracticeInsights.CriterionProgressPoint;

@Service
public class ViewStudentPracticeProgressUseCase
        implements IUseCase<ViewStudentPracticeProgressQuery, List<CriterionProgressPoint>> {

    private final PracticeInsightsQueryRepository practiceInsightsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewStudentPracticeProgressUseCase(
            PracticeInsightsQueryRepository practiceInsightsQueryRepository,
            UserContextPort userContextPort) {
        this.practiceInsightsQueryRepository = practiceInsightsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriterionProgressPoint> execute(ViewStudentPracticeProgressQuery input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        practiceInsightsQueryRepository.requireTeacherCanReadStudent(teacherId, input.studentId());
        return practiceInsightsQueryRepository.progress(input.studentId(), null, 90);
    }
}

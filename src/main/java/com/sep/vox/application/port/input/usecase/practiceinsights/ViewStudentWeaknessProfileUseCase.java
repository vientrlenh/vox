package com.sep.vox.application.port.input.usecase.practiceinsights;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.ViewStudentWeaknessProfileQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeInsightsQueryRepository;
import com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

@Service
public class ViewStudentWeaknessProfileUseCase implements IUseCase<ViewStudentWeaknessProfileQuery, WeaknessProfile> {

    private final PracticeInsightsQueryRepository practiceInsightsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewStudentWeaknessProfileUseCase(
            PracticeInsightsQueryRepository practiceInsightsQueryRepository,
            UserContextPort userContextPort) {
        this.practiceInsightsQueryRepository = practiceInsightsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public WeaknessProfile execute(ViewStudentWeaknessProfileQuery input) {
        var teacherId = userContextPort.getCurrentAuthenticatedUserId();
        practiceInsightsQueryRepository.requireTeacherCanReadStudent(teacherId, input.studentId());
        return practiceInsightsQueryRepository.weaknessProfile(input.studentId());
    }
}

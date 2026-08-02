package com.sep.vox.application.port.input.usecase.practiceinsights;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.PracticeInsightsQueryRepository;
import com.sep.vox.application.response.input.practiceinsights.PracticeInsights.WeaknessProfile;

@Service
public class ViewMyWeaknessProfileUseCase implements IUseCase<Void, WeaknessProfile> {

    private final PracticeInsightsQueryRepository practiceInsightsQueryRepository;
    private final UserContextPort userContextPort;

    public ViewMyWeaknessProfileUseCase(
            PracticeInsightsQueryRepository practiceInsightsQueryRepository,
            UserContextPort userContextPort) {
        this.practiceInsightsQueryRepository = practiceInsightsQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public WeaknessProfile execute(Void input) {
        var studentId = userContextPort.getCurrentAuthenticatedUserId();
        return practiceInsightsQueryRepository.weaknessProfile(studentId);
    }
}

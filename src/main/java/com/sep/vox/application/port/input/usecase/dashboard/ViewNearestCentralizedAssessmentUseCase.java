package com.sep.vox.application.port.input.usecase.dashboard;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.projection.dto.NearestCentralizedAssessmentDto;
import com.sep.vox.application.projection.repository.NearestCentralizedAssessmentQueryRepository;

/** Kỳ thi tập trung gần thời điểm hiện tại nhất của trường — dùng cho dashboard school admin. */
@Service
public class ViewNearestCentralizedAssessmentUseCase implements IUseCase<Void, Optional<NearestCentralizedAssessmentDto>> {

    private final NearestCentralizedAssessmentQueryRepository nearestCentralizedAssessmentQueryRepository;
    private final UserContextPort userContextPort;

    public ViewNearestCentralizedAssessmentUseCase(
            NearestCentralizedAssessmentQueryRepository nearestCentralizedAssessmentQueryRepository,
            UserContextPort userContextPort) {
        this.nearestCentralizedAssessmentQueryRepository = nearestCentralizedAssessmentQueryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NearestCentralizedAssessmentDto> execute(Void input) {
        UUID schoolId = userContextPort.getCurrentSchoolId()
            .orElseThrow(() -> new ForbiddenException("Tài khoản không thuộc trường học nào."));
        return nearestCentralizedAssessmentQueryRepository.findNearestForSchool(schoolId);
    }
}

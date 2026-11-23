package com.sep.vox.application.port.input.usecase.dashboard;

import java.util.Optional;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.NearestCentralizedAssessmentDto;
import com.sep.vox.application.query.repository.NearestCentralizedAssessmentQueryRepository;

/** Kỳ thi tập trung gần thời điểm hiện tại nhất của trường — dùng cho dashboard school admin. */
@Service
@RequiredArgsConstructor
public class ViewNearestCentralizedAssessmentUseCase implements IUseCase<Void, Optional<NearestCentralizedAssessmentDto>> {

    private final NearestCentralizedAssessmentQueryRepository nearestCentralizedAssessmentQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public Optional<NearestCentralizedAssessmentDto> execute(Void input) {
        UUID schoolId = userContextPort.getCurrentSchoolId();
        return nearestCentralizedAssessmentQueryRepository.findNearestForSchool(schoolId);
    }
}

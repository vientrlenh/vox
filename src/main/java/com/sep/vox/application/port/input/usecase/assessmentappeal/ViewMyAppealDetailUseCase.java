package com.sep.vox.application.port.input.usecase.assessmentappeal;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.AppealDetailInfo;
import com.sep.vox.application.query.repository.AssessmentAppealQueryRepository;

// Shares the AppealDetailInfo type with the school admin by the project owner's decision -- the
// reviewer field still exists on this type, but the frontend (web/mobile) must not ask for or show that field to the
// student (see useStudentAppealQueries.ts / appeal_api.dart).
@Service
@RequiredArgsConstructor
public class ViewMyAppealDetailUseCase implements IUseCase<UUID, AppealDetailInfo> {

    private final AssessmentAppealQueryRepository assessmentAppealQueryRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public AppealDetailInfo execute(UUID appealId) {
        return assessmentAppealQueryRepository.findStudentDetailById(
                appealId, userContextPort.getCurrentAuthenticatedUserId())
            .orElseThrow(() -> new ForbiddenException("You are not allowed to view this appeal."));
    }
}

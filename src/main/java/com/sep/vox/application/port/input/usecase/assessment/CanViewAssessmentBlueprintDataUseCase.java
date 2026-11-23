package com.sep.vox.application.port.input.usecase.assessment;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.port.input.query.CanViewAssessmentBlueprintDataQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Answers "may the current user view the blueprint/blueprintVersion attached to this
 * assessment" for a SCHOOL_ADMIN of the same school (or anyone in the same school
 * after the assessment has closed). It does not cover the "is a member of the
 * assessment" branch -- that branch needs the member list through a DataLoader, so it
 * stays in the controller.
 */
@Service
@RequiredArgsConstructor
public class CanViewAssessmentBlueprintDataUseCase implements IUseCase<CanViewAssessmentBlueprintDataQuery, Boolean> {

    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public Boolean execute(CanViewAssessmentBlueprintDataQuery input) {
        if (userContextPort.isSystemAdmin()) {
            return true;
        }

        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (currentSchoolId == null || !currentSchoolId.equals(input.assessmentSchoolId())) {
            return false;
        }

        boolean schoolAdmin = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "SCHOOL_ADMIN".equals(role.roleCode()));
        if (schoolAdmin) {
            return true;
        }

        // Once the assessment has closed, anyone in the same school may see the blueprint link -- membership is no longer required.
        return input.assessmentStatus() == AssessmentStatus.CLOSED
            || input.assessmentStatus() == AssessmentStatus.RESULTS_PUBLISHED;
    }
}

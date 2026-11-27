package com.sep.vox.application.port.input.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Decides whether the current user may manage a question bank or what it contains: a system bank is managed
 * by a system admin, a school bank by a member of the school that owns it.
 */
@Service
@RequiredArgsConstructor
public class QuestionBankManageAccessService {

    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    public void requireManageAccess(QuestionBank bank, UUID currentUserId) {
        if (bank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Access denied");
            }
            return;
        }
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        if (currentSchoolId == null || !currentSchoolId.equals(bank.getSchoolId())) {
            throw new ForbiddenException("Access denied");
        }
    }
}

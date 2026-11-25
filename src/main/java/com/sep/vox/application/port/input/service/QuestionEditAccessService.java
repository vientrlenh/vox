package com.sep.vox.application.port.input.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionAsset;
import com.sep.vox.domain.model.question.QuestionBank;
import com.sep.vox.domain.model.question.QuestionBankOwnerType;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.repository.QuestionAssetRepository;
import com.sep.vox.domain.repository.QuestionBankRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;

/**
 * Decides whether the current user may change a question or the parts attached to it
 * (assets, evaluation guide), whether the change must go to a draft clone instead, and whether
 * the user may create questions in a question bank.
 */
@Service
@RequiredArgsConstructor
public class QuestionEditAccessService {

    private final QuestionRepository questionRepository;
    private final QuestionAssetRepository questionAssetRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserContextPort userContextPort;

    public QuestionAsset requireAssetOf(Question question, UUID assetId) {
        QuestionAsset asset = questionAssetRepository.findById(assetId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài nguyên câu hỏi"));
        if (!asset.getQuestionId().equals(question.getId())) {
            throw new ForbiddenException("Tài nguyên không thuộc câu hỏi này");
        }
        return asset;
    }

    public record EditAccess(boolean owner, boolean editorCollaborator, boolean systemAdminOnSystemBank) {

        public boolean allowed() {
            return systemAdminOnSystemBank || owner || editorCollaborator;
        }
    }

    public EditAccess resolveEditAccess(Question question, QuestionBank bank, UUID currentUserId) {
        boolean owner = question.isCreatedBy(currentUserId);
        boolean editorCollaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId)
            .filter(collaborator -> collaborator.getPermission() == QuestionCollaboratorPermission.CAN_EDIT)
            .isPresent();
        boolean systemAdminOnSystemBank = userContextPort.isSystemAdmin()
            && bank.getOwnerType() == QuestionBankOwnerType.SYSTEM;
        return new EditAccess(owner, editorCollaborator, systemAdminOnSystemBank);
    }

    /**
     * Requires edit access and, for an owner, a status that allows editing.
     *
     * @return {@code true} when the question can no longer be edited in place (published, locked or used
     *         in an assessment), so the caller must apply the change to a draft clone
     */
    public boolean authorizeEditing(Question question, UUID currentUserId) {
        EditAccess access = requireEditAccess(question, currentUserId);
        boolean immutable = isImmutable(question);
        if (!access.systemAdminOnSystemBank()
                && access.owner()
                && !immutable
                && question.getStatus() != QuestionStatus.DRAFT
                && question.getStatus() != QuestionStatus.REVISION_REQUESTED) {
            throw new ForbiddenException("Chỉ được sửa câu hỏi của mình khi ở trạng thái DRAFT hoặc REVISION_REQUESTED");
        }
        return immutable;
    }

    /**
     * Requires edit access only; the question status does not matter because regenerating an analysis is an
     * explicit user action.
     *
     * @return {@code true} when the question can no longer be edited in place
     */
    public boolean authorizeRegeneration(Question question, UUID currentUserId) {
        requireEditAccess(question, currentUserId);
        return isImmutable(question);
    }

    /**
     * A system bank accepts a system admin only; a school bank accepts a teacher of that same school.
     */
    public void requireCreationAccess(QuestionBank bank, UUID currentUserId) {
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean teacher = userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
            .anyMatch(role -> "TEACHER".equals(role.roleCode()));

        if (bank.getOwnerType() == QuestionBankOwnerType.SYSTEM) {
            if (!userContextPort.isSystemAdmin()) {
                throw new ForbiddenException("Quyền truy cập bị từ chối");
            }
        } else if (!teacher || currentSchoolId == null || !currentSchoolId.equals(bank.getSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
    }

    private EditAccess requireEditAccess(Question question, UUID currentUserId) {
        QuestionBank bank = questionBankRepository.findById(question.getQuestionBankId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngân hàng câu hỏi"));
        EditAccess access = resolveEditAccess(question, bank, currentUserId);
        if (!access.allowed()) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return access;
    }

    private boolean isImmutable(Question question) {
        return question.getStatus() == QuestionStatus.PUBLISHED
            || question.isLocked()
            || questionRepository.existsUsedInAssessment(question.getId());
    }
}

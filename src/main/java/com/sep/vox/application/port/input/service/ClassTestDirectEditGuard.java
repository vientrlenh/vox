package com.sep.vox.application.port.input.service;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.domain.model.assessment.Assessment;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.AssessmentMemberRole;
import com.sep.vox.domain.model.assessment.AssessmentStatus;
import com.sep.vox.domain.model.question.Question;
import com.sep.vox.domain.model.question.QuestionCollaborator;
import com.sep.vox.domain.model.question.QuestionCollaboratorPermission;
import com.sep.vox.domain.model.question.QuestionSharing;
import com.sep.vox.domain.repository.AssessmentMemberRepository;
import com.sep.vox.domain.repository.AssessmentRepository;
import com.sep.vox.domain.repository.QuestionCollaboratorRepository;
import com.sep.vox.domain.repository.QuestionRepository;

/**
 * Guards every direct edit of the sections and questions of a class test: only the CHAIR may edit, only before
 * the test starts, never after a student has submitted, and never while a shared blueprint is attached.
 */
@Service
@RequiredArgsConstructor
public class ClassTestDirectEditGuard {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentMemberRepository assessmentMemberRepository;
    private final QuestionRepository questionRepository;
    private final QuestionCollaboratorRepository questionCollaboratorRepository;

    public void requireDirectlyEditable(Assessment assessment, UUID currentUserId) {
        if (assessment.getKind() != AssessmentKind.CLASS_TEST) {
            throw new ForbiddenException("Only applicable to class tests");
        }
        if (!assessmentMemberRepository.existsByAssessmentIdAndUserIdAndRole(assessment.getId(), currentUserId, AssessmentMemberRole.CHAIR)) {
            throw new ForbiddenException("Access denied");
        }
        if (assessment.getStatus() != AssessmentStatus.DRAFT && assessment.getStatus() != AssessmentStatus.SCHEDULED) {
            throw new IllegalArgumentException("Can only be edited before the assessment has started");
        }
        if (assessmentRepository.existsSubmittedSessionByAssessmentId(assessment.getId())) {
            throw new IllegalArgumentException("Questions cannot be edited once a student has submitted");
        }
        requireNoAttachedBlueprint(assessment);
    }

    /**
     * Loads a question the caller may put into a class test: the caller must be able to read it, and unless the caller
     * owns it or it is shared with the school, the caller must be a collaborator with more than READ_ONLY permission.
     */
    public Question loadUsableQuestion(Assessment assessment, UUID questionId, UUID currentUserId) {
        Question question = questionRepository.findAccessibleById(questionId, currentUserId, assessment.getSchoolId(), false, false)
            .orElseThrow(() -> new ForbiddenException("No permission to use question " + questionId));
        boolean isOwner = question.isCreatedBy(currentUserId);
        boolean isSchoolShared = question.getSharing() == QuestionSharing.SCHOOL_SHARED;
        if (!isOwner && !isSchoolShared) {
            Optional<QuestionCollaborator> collaborator = questionCollaboratorRepository.findByQuestionIdAndUserId(question.getId(), currentUserId);
            if (collaborator.isEmpty() || collaborator.get().getPermission() == QuestionCollaboratorPermission.READ_ONLY) {
                throw new ForbiddenException("READ_ONLY permission does not allow using questions in an assessment");
            }
        }
        return question;
    }

    private void requireNoAttachedBlueprint(Assessment assessment) {
        if (assessment.getBlueprintId() != null) {
            throw new IllegalArgumentException(
                "The assessment uses a shared blueprint, so questions cannot be edited directly — use \"Switch to another blueprint\" in the Blueprint tab to change the structure");
        }
    }
}

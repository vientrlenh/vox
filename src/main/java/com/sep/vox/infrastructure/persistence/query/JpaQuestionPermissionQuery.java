package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.question.QuestionStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaQuestionPermissionQuery implements QuestionPermissionQuery {

    @PersistenceContext
    private EntityManager em;

    private final UserContextPort userContextPort;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public JpaQuestionPermissionQuery(
            UserContextPort userContextPort,
            UserRoleQueryRepository userRoleQueryRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    private ResolvedUser resolveCurrentUser() {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("Khong tim thay nguoi dung"));
        var roleInfos = userRoleQueryRepository.findByUserIdWithRoleInfo(userId);
        String role = resolveRole(roleInfos);
        return new ResolvedUser(userId, role, resolveSchoolId(userId, role));
    }

    private String resolveRole(List<UserRoleInfo> roleInfos) {
        if (roleInfos.stream().anyMatch(r -> "SYSTEM_ADMIN".equals(r.roleCode()))) {
            return "SYSTEM_ADMIN";
        }
        if (roleInfos.stream().anyMatch(r -> "SCHOOL_ADMIN".equals(r.roleCode()))) {
            return "SCHOOL_ADMIN";
        }
        if (roleInfos.stream().anyMatch(r -> "TEACHER".equals(r.roleCode()))) {
            return "TEACHER";
        }
        if (roleInfos.stream().anyMatch(r -> "STUDENT".equals(r.roleCode()))) {
            return "STUDENT";
        }
        throw new ForbiddenException("Nguoi dung khong co vai tro hop le");
    }

    private UUID resolveSchoolId(UUID userId, String role) {
        if ("SCHOOL_ADMIN".equals(role) || "TEACHER".equals(role)) {
            return schoolUserRepository.findByUserId(userId)
                .map(SchoolUser::getSchoolId)
                .orElseThrow(() -> new ForbiddenException("Nguoi dung hien tai khong thuoc truong nao"));
        }
        return schoolUserRepository.findByUserId(userId)
            .map(SchoolUser::getSchoolId)
            .orElse(null);
    }

    private QuestionJpaEntity loadQuestion(UUID questionId) {
        return em.createQuery("""
            SELECT q FROM QuestionJpaEntity q
            WHERE q.id = :questionId
            """, QuestionJpaEntity.class)
            .setParameter("questionId", questionId)
            .getSingleResult();
    }

    @Override
    public boolean canEditContent(UUID questionId) {
        var user = resolveCurrentUser();
        var question = loadQuestion(questionId);

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return canSystemAdminEditQuestionBank(questionId, question.getScope());
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return false;
        }
        if (!"TEACHER".equals(user.role())) {
            return false;
        }

        return switch (question.getScope()) {
            case "QUESTION_BANK" -> canTeacherEditOwnQuestion(questionId, user.userId(), "QUESTION_BANK");
            case "CLASSROOM_ASSESSMENT" -> canTeacherEditOwnQuestion(questionId, user.userId(), "CLASSROOM_ASSESSMENT");
            case "CENTRAL_EXAM_DRAFT" -> canTeacherEditOwnQuestion(questionId, user.userId(), "CENTRAL_EXAM_DRAFT");
            case "CENTRAL_EXAM_PAPER" -> false;
            default -> false;
        };
    }

    @Override
    public boolean canReview(UUID questionId, QuestionStatus targetStatus) {
        var user = resolveCurrentUser();
        var question = loadQuestion(questionId);

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return "QUESTION_BANK".equals(question.getScope()) && isSystemBankQuestion(questionId);
        }

        return switch (question.getScope()) {
            case "QUESTION_BANK" -> canReviewQuestionByScope(questionId, user, targetStatus);
            case "CLASSROOM_ASSESSMENT" -> false;
            case "CENTRAL_EXAM_DRAFT" -> canReviewCentralExamDraft(questionId, user, targetStatus);
            case "CENTRAL_EXAM_PAPER" -> canReviewCentralExamPaper(questionId, user, targetStatus);
            default -> false;
        };
    }

    private boolean canReviewQuestionByScope(UUID questionId, ResolvedUser user, QuestionStatus targetStatus) {
        return switch (targetStatus) {
            case SUBMITTED_FOR_REVIEW -> canSubmitForReview(questionId, user);
            case REVISION_REQUESTED -> canRequestRevision(questionId, user);
            case APPROVED -> canApprove(questionId, user);
            case REJECTED -> canReject(questionId, user);
            case PUBLISHED -> canPublish(questionId, user);
            case ARCHIVED -> canArchive(questionId, user);
            case DRAFT -> canRestore(questionId, user);
        };
    }

    private boolean canReviewCentralExamDraft(UUID questionId, ResolvedUser user, QuestionStatus targetStatus) {
        return ("SCHOOL_ADMIN".equals(user.role()) || "TEACHER".equals(user.role()))
            && canReviewQuestionByScope(questionId, user, targetStatus);
    }

    private boolean canReviewCentralExamPaper(UUID questionId, ResolvedUser user, QuestionStatus targetStatus) {
        return "SCHOOL_ADMIN".equals(user.role()) && canReviewQuestionByScope(questionId, user, targetStatus);
    }

    private boolean canSystemAdminEditQuestionBank(UUID questionId, String scope) {
        if (!"QUESTION_BANK".equals(scope)) {
            return false;
        }
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.ownerType = 'SYSTEM'
                  AND q.locked = false
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                  AND q.status IN ('DRAFT', 'REVISION_REQUESTED', 'REJECTED')
                  AND q.scope = 'QUESTION_BANK'
                """)
                .setParameter("questionId", questionId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean canTeacherEditOwnQuestion(UUID questionId, UUID userId, String scope) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.createdBy = :userId
                  AND q.locked = false
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                  AND q.status IN ('DRAFT', 'REVISION_REQUESTED', 'REJECTED')
                  AND q.scope = :scope
                """)
                .setParameter("questionId", questionId)
                .setParameter("userId", userId)
                .setParameter("scope", scope)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean canSubmitForReview(UUID questionId, ResolvedUser user) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.locked = false
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                  AND q.status IN ('DRAFT', 'REVISION_REQUESTED', 'REJECTED')
                  AND (
                    (:role = 'TEACHER' AND q.createdBy = :userId)
                    OR (:role = 'SCHOOL_ADMIN' AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
                  )
                """)
                .setParameter("questionId", questionId)
                .setParameter("userId", user.userId())
                .setParameter("role", user.role())
                .setParameter("schoolId", user.schoolId())
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean canRequestRevision(UUID questionId, ResolvedUser user) {
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwner(questionId, user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            return isReviewer(questionId, user.userId(), user.schoolId());
        }
        return false;
    }

    private boolean canApprove(UUID questionId, ResolvedUser user) {
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwner(questionId, user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            return isReviewer(questionId, user.userId(), user.schoolId());
        }
        return false;
    }

    private boolean canReject(UUID questionId, ResolvedUser user) {
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwner(questionId, user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            return isReviewer(questionId, user.userId(), user.schoolId());
        }
        return false;
    }

    private boolean canPublish(UUID questionId, ResolvedUser user) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.locked = false
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                  AND q.status = 'APPROVED'
                  AND (
                    (:role = 'TEACHER' AND q.createdBy = :userId)
                    OR (:role = 'SCHOOL_ADMIN' AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
                  )
                """)
                .setParameter("questionId", questionId)
                .setParameter("userId", user.userId())
                .setParameter("role", user.role())
                .setParameter("schoolId", user.schoolId())
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean canArchive(UUID questionId, ResolvedUser user) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.status <> 'ARCHIVED'
                  AND (
                    (:role = 'TEACHER' AND q.createdBy = :userId AND q.status <> 'PUBLISHED')
                    OR (:role = 'SCHOOL_ADMIN' AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
                  )
                """)
                .setParameter("questionId", questionId)
                .setParameter("userId", user.userId())
                .setParameter("role", user.role())
                .setParameter("schoolId", user.schoolId())
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean canRestore(UUID questionId, ResolvedUser user) {
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwner(questionId, user.schoolId());
        }
        return false;
    }

    private boolean isSystemBankQuestion(UUID questionId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.ownerType = 'SYSTEM'
                """)
                .setParameter("questionId", questionId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isSchoolOwner(UUID questionId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                """)
                .setParameter("questionId", questionId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isReviewer(UUID questionId, UUID userId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.status = 'SUBMITTED_FOR_REVIEW'
                  AND q.visibility = 'REVIEWER_ONLY'
                  AND q.createdBy <> :userId
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                """)
                .setParameter("questionId", questionId)
                .setParameter("userId", userId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private record ResolvedUser(UUID userId, String role, UUID schoolId) {}
}

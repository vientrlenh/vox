package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.QuestionViewPermissionQuery;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.infrastructure.persistence.entity.QuestionJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaQuestionViewPermissionQuery implements QuestionViewPermissionQuery {

    @PersistenceContext
    private EntityManager em;

    private final UserContextPort userContextPort;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public JpaQuestionViewPermissionQuery(
            UserContextPort userContextPort,
            UserRoleQueryRepository userRoleQueryRepository,
            UserRepository userRepository,
            SchoolUserRepository schoolUserRepository) {
        this.userContextPort = userContextPort;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userRepository = userRepository;
        this.schoolUserRepository = schoolUserRepository;
    }

    @Override
    public boolean canViewQuestionDetail(UUID questionId) {
        var user = resolveCurrentUser();

        if ("STUDENT".equals(user.role())) {
            return false;
        }

        var question = loadQuestion(questionId);

        if (isCreatorOnActiveHierarchy(questionId, user.userId())) {
            return true;
        }

        return switch (question.getVisibility()) {
            case "AUTHOR_ONLY" -> false;
            case "REVIEWER_ONLY" -> canViewReviewerOnly(questionId, question, user);
            case "BANK_VISIBLE" -> canViewBankVisible(questionId, question, user);
            default -> false;
        };
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

    private boolean canViewReviewerOnly(UUID questionId, QuestionJpaEntity question, ResolvedUser user) {
        return switch (question.getScope()) {
            case "QUESTION_BANK", "CENTRAL_EXAM_DRAFT" ->
                canSchoolAdminViewRestricted(questionId, user)
                    || canTeacherReviewQuestion(questionId, user);
            case "CENTRAL_EXAM_PAPER", "CLASSROOM_ASSESSMENT" ->
                canSchoolAdminViewRestricted(questionId, user);
            default -> false;
        };
    }

    private boolean canViewBankVisible(UUID questionId, QuestionJpaEntity question, ResolvedUser user) {
        return switch (question.getScope()) {
            case "QUESTION_BANK" -> canViewQuestionBank(questionId, user);
            case "CLASSROOM_ASSESSMENT" -> canViewClassroomAssessment(questionId, user);
            case "CENTRAL_EXAM_DRAFT" -> canViewCentralExamDraft(questionId, user);
            case "CENTRAL_EXAM_PAPER" -> canViewCentralExamPaper(questionId, user);
            default -> false;
        };
    }

    private boolean canViewQuestionBank(UUID questionId, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return isPublishedBankVisibleForAdmin(questionId);
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isPublishedBankVisibleForSchool(questionId, user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            return isPublishedBankVisibleForSchool(questionId, user.schoolId());
        }
        return false;
    }

    private boolean canViewClassroomAssessment(UUID questionId, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return isNonArchivedQuestion(questionId);
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSameSchoolNonArchived(questionId, user.schoolId());
        }
        return false;
    }

    private boolean canViewCentralExamDraft(UUID questionId, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return isNonArchivedQuestion(questionId);
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSameSchoolNonArchived(questionId, user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            return isPublishedSameSchoolQuestion(questionId, user.schoolId());
        }
        return false;
    }

    private boolean canViewCentralExamPaper(UUID questionId, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return isNonArchivedQuestion(questionId);
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSameSchoolNonArchived(questionId, user.schoolId());
        }
        return false;
    }

    private boolean canSchoolAdminViewRestricted(UUID questionId, ResolvedUser user) {
        return "SCHOOL_ADMIN".equals(user.role())
            && isSameSchoolNonArchived(questionId, user.schoolId());
    }

    private boolean canTeacherReviewQuestion(UUID questionId, ResolvedUser user) {
        return "TEACHER".equals(user.role())
            && isReviewQueueQuestion(questionId, user.userId(), user.schoolId());
    }

    private boolean isCreatorOnActiveHierarchy(UUID questionId, UUID userId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.createdBy = :userId
                  AND qb.status <> 'ARCHIVED'
                  AND qt.status <> 'ARCHIVED'
                  AND q.status <> 'ARCHIVED'
                """)
                .setParameter("questionId", questionId)
                .setParameter("userId", userId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isPublishedBankVisibleForAdmin(UUID questionId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.status = 'PUBLISHED'
                  AND qt.status = 'PUBLISHED'
                  AND q.status = 'PUBLISHED'
                  AND q.visibility = 'BANK_VISIBLE'
                """)
                .setParameter("questionId", questionId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isPublishedBankVisibleForSchool(UUID questionId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.status = 'PUBLISHED'
                  AND qt.status = 'PUBLISHED'
                  AND q.status = 'PUBLISHED'
                  AND q.visibility = 'BANK_VISIBLE'
                  AND (
                    qb.ownerType = 'SYSTEM'
                    OR (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
                  )
                """)
                .setParameter("questionId", questionId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isReviewQueueQuestion(UUID questionId, UUID userId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.scope IN ('QUESTION_BANK', 'CENTRAL_EXAM_DRAFT')
                  AND q.status = 'SUBMITTED_FOR_REVIEW'
                  AND q.visibility = 'REVIEWER_ONLY'
                  AND q.createdBy <> :userId
                  AND qb.ownerType = 'SCHOOL'
                  AND qb.schoolId = :schoolId
                  AND qb.status <> 'ARCHIVED'
                  AND qt.status <> 'ARCHIVED'
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

    private boolean isSameSchoolNonArchived(UUID questionId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.ownerType = 'SCHOOL'
                  AND qb.schoolId = :schoolId
                  AND qb.status <> 'ARCHIVED'
                  AND qt.status <> 'ARCHIVED'
                  AND q.status <> 'ARCHIVED'
                """)
                .setParameter("questionId", questionId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isPublishedSameSchoolQuestion(UUID questionId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.ownerType = 'SCHOOL'
                  AND qb.schoolId = :schoolId
                  AND qb.status <> 'ARCHIVED'
                  AND qt.status <> 'ARCHIVED'
                  AND q.status = 'PUBLISHED'
                """)
                .setParameter("questionId", questionId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isNonArchivedQuestion(UUID questionId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.status <> 'ARCHIVED'
                  AND qt.status <> 'ARCHIVED'
                  AND q.status <> 'ARCHIVED'
                """)
                .setParameter("questionId", questionId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private record ResolvedUser(UUID userId, String role, UUID schoolId) {}
}

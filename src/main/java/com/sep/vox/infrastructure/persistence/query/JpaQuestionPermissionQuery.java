package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.QuestionPermissionQuery;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.model.question.QuestionStatus;
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
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("Không tìm thấy người dùng"));
        var roleInfos = userRoleQueryRepository.findByUserIdWithRoleInfo(userId);
        String role = resolveRole(roleInfos);
        return new ResolvedUser(userId, role, resolveSchoolId(role, userId));
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
        throw new ForbiddenException("Người dùng không có vai trò hợp lệ");
    }

    @Override
    public boolean canEditContent(UUID questionId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return isSystemQuestionEditable(questionId);
        }

        try {
            em.createQuery("""
                SELECT q FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.locked = false
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                  AND q.status IN ('DRAFT', 'REVISION_REQUESTED', 'REJECTED')
                  AND (
                    (:role = 'TEACHER' AND q.createdBy = :userId)
                    OR (:role = 'SCHOOL_ADMIN' AND q.scope = 'QUESTION_BANK' AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId)
                  )
                """, QuestionJpaEntity.class)
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

    @Override
    public boolean canReview(UUID questionId, QuestionStatus targetStatus) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return canSystemAdminReview(questionId, targetStatus);
        }

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
        if ("TEACHER".equals(user.role())) {
            return isTeacherOwnerArchivedQuestion(questionId, user.userId());
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwner(questionId, user.schoolId());
        }
        return false;
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

    private boolean isTeacherOwnerArchivedQuestion(UUID questionId, UUID userId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.createdBy = :userId
                  AND q.status = 'ARCHIVED'
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                """)
                .setParameter("questionId", questionId)
                .setParameter("userId", userId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isSystemQuestionEditable(UUID questionId) {
        return systemQuestionMatches(questionId, List.of("DRAFT", "REVISION_REQUESTED", "REJECTED"), false);
    }

    private boolean canSystemAdminReview(UUID questionId, QuestionStatus targetStatus) {
        return switch (targetStatus) {
            case SUBMITTED_FOR_REVIEW -> systemQuestionMatches(
                questionId,
                List.of("DRAFT", "REVISION_REQUESTED", "REJECTED"),
                false
            );
            case REVISION_REQUESTED, APPROVED, REJECTED -> systemQuestionMatches(
                questionId,
                List.of("SUBMITTED_FOR_REVIEW"),
                false
            );
            case PUBLISHED -> systemQuestionMatches(questionId, List.of("APPROVED"), false);
            case ARCHIVED -> systemQuestionMatches(
                questionId,
                List.of("DRAFT", "REVISION_REQUESTED", "REJECTED", "APPROVED", "PUBLISHED", "SUBMITTED_FOR_REVIEW"),
                true
            );
            case DRAFT -> systemQuestionMatches(questionId, List.of("ARCHIVED"), true);
        };
    }

    private boolean systemQuestionMatches(UUID questionId, List<String> statuses, boolean ignoreLocked) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.ownerType = 'SYSTEM'
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
                  AND q.status IN :statuses
                  AND (:ignoreLocked = true OR q.locked = false)
                """)
                .setParameter("questionId", questionId)
                .setParameter("statuses", statuses)
                .setParameter("ignoreLocked", ignoreLocked)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private UUID resolveSchoolId(String role, UUID userId) {
        if ("SYSTEM_ADMIN".equals(role)) {
            return null;
        }
        return getSchoolId(userId);
    }

    private UUID getSchoolId(UUID userId) {
        return schoolUserRepository.findByUserId(userId)
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new ForbiddenException("Nguoi dung hien tai khong thuoc truong nao"));
    }

    private record ResolvedUser(UUID userId, String role, UUID schoolId) {}
}

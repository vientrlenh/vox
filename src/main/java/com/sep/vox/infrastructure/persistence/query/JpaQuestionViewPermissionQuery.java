package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.QuestionViewPermissionQuery;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.UserRepository;

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

    public JpaQuestionViewPermissionQuery(
            UserContextPort userContextPort,
            UserRoleQueryRepository userRoleQueryRepository,
            UserRepository userRepository) {
        this.userContextPort = userContextPort;
        this.userRoleQueryRepository = userRoleQueryRepository;
        this.userRepository = userRepository;
    }

    private ResolvedUser resolveCurrentUser() {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("Không tìm thấy người dùng"));
        var roleInfos = userRoleQueryRepository.findByUserIdWithRoleInfo(userId);
        String role = resolveRole(roleInfos);
        return new ResolvedUser(userId, role, user.getSchoolId());
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
    public boolean canViewQuestionDetail(UUID questionId) {
        var user = resolveCurrentUser();

        // STUDENT: deny all
        if ("STUDENT".equals(user.role())) {
            return false;
        }

        // SYSTEM_ADMIN: can view any non-ARCHIVED question
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsNonArchived(questionId);
        }

        // SCHOOL_ADMIN: own school bank, non-archived
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return canSchoolAdminView(questionId, user.schoolId());
        }

        // TEACHER: multiple paths
        if ("TEACHER".equals(user.role())) {
            return canTeacherView(questionId, user.userId(), user.schoolId());
        }

        return false;
    }

    private boolean existsNonArchived(UUID questionId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                WHERE q.id = :questionId AND q.status <> 'ARCHIVED'
                """)
                .setParameter("questionId", questionId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean canSchoolAdminView(UUID questionId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
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

    private boolean canTeacherView(UUID questionId, UUID userId, UUID schoolId) {
        // Path 1: own question, non-archived bank/topic/question
        if (isTeacherOwnQuestion(questionId, userId)) {
            return true;
        }

        // Path 2: published, BANK_VISIBLE
        if (isPublishedBankVisible(questionId)) {
            return true;
        }

        // Path 3: review queue - SUBMITTED_FOR_REVIEW, REVIEWER_ONLY, not author, same school
        if (isReviewQueueQuestion(questionId, userId, schoolId)) {
            return true;
        }

        // Path 4: AUTHOR_ONLY, own question (already covered by path 1)
        // Path 5: ASSESSMENT_ONLY, EXAM_PAPER_ONLY - deny in QUESTION_BANK scope

        return false;
    }

    private boolean isTeacherOwnQuestion(UUID questionId, UUID userId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND q.createdBy = :userId
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
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

    private boolean isPublishedBankVisible(UUID questionId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED'
                  AND q.status = 'PUBLISHED' AND q.visibility = 'BANK_VISIBLE'
                """)
                .setParameter("questionId", questionId)
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
                  AND q.status = 'SUBMITTED_FOR_REVIEW' AND q.visibility = 'REVIEWER_ONLY'
                  AND q.createdBy <> :userId
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                  AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED'
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

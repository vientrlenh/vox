package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaQuestionViewPermissionQuery.class);

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

    private ResolvedUser resolveCurrentUser() {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("Khong tim thay nguoi dung"));
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
        throw new ForbiddenException("Nguoi dung khong co vai tro hop le");
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
    public boolean canViewQuestionDetail(UUID questionId) {
        var user = resolveCurrentUser();

        if ("STUDENT".equals(user.role())) {
            return false;
        }

        var question = loadQuestion(questionId);
        var allowed = switch (question.getScope()) {
            case "QUESTION_BANK" -> canViewQuestionBank(question, user);
            case "CLASSROOM_ASSESSMENT" -> canViewClassroomAssessment(question, user);
            case "CENTRAL_EXAM_DRAFT" -> canViewCentralExamDraft(question, user);
            case "CENTRAL_EXAM_PAPER" -> canViewCentralExamPaper(question, user);
            default -> false;
        };

        if (!allowed) {
            logDeniedQuestionDetail(questionId, question, user);
        }

        return allowed;
    }

    private boolean canViewQuestionBank(QuestionJpaEntity q, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return q.getStatus() != null && !"ARCHIVED".equals(q.getStatus());
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return canSchoolAdminViewQuestion(q.getId(), user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            return canTeacherViewQuestionBank(q.getId(), user.userId(), user.schoolId());
        }
        return false;
    }

    private boolean canSchoolAdminViewQuestion(UUID questionId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND (
                    (qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                        AND qb.status <> 'ARCHIVED' AND qt.status <> 'ARCHIVED' AND q.status <> 'ARCHIVED')
                    OR (qb.ownerType = 'SYSTEM'
                        AND qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED'
                        AND q.status = 'PUBLISHED' AND q.visibility = 'BANK_VISIBLE')
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

    private boolean canTeacherViewQuestionBank(UUID questionId, UUID userId, UUID schoolId) {
        if (isTeacherOwnNonArchived(questionId, userId)) {
            return true;
        }
        if (isPublishedBankVisible(questionId, schoolId)) {
            return true;
        }
        if (isReviewQueueQuestion(questionId, userId, schoolId)) {
            return true;
        }
        return false;
    }

    private boolean canViewClassroomAssessment(QuestionJpaEntity q, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return !"ARCHIVED".equals(q.getStatus());
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSameSchool(q.getId(), user.schoolId())
                || isPublishedBankVisible(q.getId(), user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            if (userIdEquals(q.getCreatedBy(), user.userId())) {
                return true;
            }
            return isPublishedBankVisible(q.getId(), user.schoolId());
        }
        return false;
    }

    private boolean canViewCentralExamDraft(QuestionJpaEntity q, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return !"ARCHIVED".equals(q.getStatus());
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return (isSameSchool(q.getId(), user.schoolId())
                && !"ARCHIVED".equals(q.getStatus()))
                || isPublishedBankVisible(q.getId(), user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            if (userIdEquals(q.getCreatedBy(), user.userId())) {
                return true;
            }
            if (isPublishedBankVisible(q.getId(), user.schoolId())) {
                return true;
            }
            if ("SUBMITTED_FOR_REVIEW".equals(q.getStatus())
                    && "REVIEWER_ONLY".equals(q.getVisibility())
                    && !userIdEquals(q.getCreatedBy(), user.userId())
                    && isSameSchool(q.getId(), user.schoolId())) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean canViewCentralExamPaper(QuestionJpaEntity q, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return true;
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSameSchool(q.getId(), user.schoolId())
                || isPublishedBankVisible(q.getId(), user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            if (userIdEquals(q.getCreatedBy(), user.userId())) {
                return true;
            }
            return isPublishedBankVisible(q.getId(), user.schoolId());
        }
        return false;
    }

    private boolean isTeacherOwnNonArchived(UUID questionId, UUID userId) {
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

    private boolean isPublishedBankVisible(UUID questionId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.status = 'PUBLISHED' AND qt.status = 'PUBLISHED'
                  AND q.status = 'PUBLISHED' AND q.visibility = 'BANK_VISIBLE'
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

    private boolean isSameSchool(UUID questionId, UUID schoolId) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                """)
                .setParameter("questionId", questionId)
                .setParameter("schoolId", schoolId)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean userIdEquals(UUID a, UUID b) {
        return a != null && a.equals(b);
    }

    private void logDeniedQuestionDetail(UUID questionId, QuestionJpaEntity question, ResolvedUser user) {
        try {
            var metadata = em.createQuery("""
                SELECT qb.ownerType, qb.schoolId, qb.status, qt.status
                FROM QuestionJpaEntity q
                JOIN QuestionTopicJpaEntity qt ON q.questionTopicId = qt.id
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE q.id = :questionId
                """, Object[].class)
                .setParameter("questionId", questionId)
                .getSingleResult();

            LOGGER.debug(
                "Denied question detail: questionId={}, role={}, userId={}, schoolId={}, scope={}, questionStatus={}, visibility={}, bankOwnerType={}, bankSchoolId={}, bankStatus={}, topicStatus={}",
                questionId,
                user.role(),
                user.userId(),
                user.schoolId(),
                question.getScope(),
                question.getStatus(),
                question.getVisibility(),
                metadata[0],
                metadata[1],
                metadata[2],
                metadata[3]
            );
        } catch (NoResultException e) {
            LOGGER.debug(
                "Denied question detail without metadata: questionId={}, role={}, userId={}, schoolId={}, scope={}, questionStatus={}, visibility={}",
                questionId,
                user.role(),
                user.userId(),
                user.schoolId(),
                question.getScope(),
                question.getStatus(),
                question.getVisibility()
            );
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

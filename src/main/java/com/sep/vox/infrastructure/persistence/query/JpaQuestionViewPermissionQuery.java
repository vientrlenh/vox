package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
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

    private ResolvedUser resolveCurrentUser() {
        UUID userId = userContextPort.getCurrentAuthenticatedUserId();
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ForbiddenException("Không tìm thấy người dùng"));
        var roleInfos = userRoleQueryRepository.findByUserIdWithRoleInfo(userId);
        String role = resolveRole(roleInfos);
        return new ResolvedUser(userId, role, getSchoolId(userId));
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

    private QuestionJpaEntity loadQuestion(UUID questionId) {
        return em.createQuery("""
            SELECT q FROM QuestionJpaEntity q
            WHERE q.id = :questionId
            """, QuestionJpaEntity.class)
            .setParameter("questionId", questionId)
            .getSingleResult();
    }

    // ==================== MAIN ENTRY ====================

    @Override
    public boolean canViewQuestionDetail(UUID questionId) {
        var user = resolveCurrentUser();

        if ("STUDENT".equals(user.role())) {
            return false;
        }

        var question = loadQuestion(questionId);

        return switch (question.getScope()) {
            case "QUESTION_BANK" -> canViewQuestionBank(question, user);
            case "CLASSROOM_ASSESSMENT" -> canViewClassroomAssessment(question, user);
            case "CENTRAL_EXAM_DRAFT" -> canViewCentralExamDraft(question, user);
            case "CENTRAL_EXAM_PAPER" -> canViewCentralExamPaper(question, user);
            default -> false;
        };
    }

    // ==================== QUESTION_BANK ====================
    // Rules: theo status + visibility + role, giống hiện tại

    private boolean canViewQuestionBank(QuestionJpaEntity q, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return q.getStatus() != null && !"ARCHIVED".equals(q.getStatus());
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return canSchoolAdminViewQuestion(q.getId(), user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            return canTeacherViewQuestionBank(q, user.userId(), user.schoolId());
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

    private boolean canTeacherViewQuestionBank(QuestionJpaEntity q, UUID userId, UUID schoolId) {
        // Path 1: own question
        if (isTeacherOwnNonArchived(q.getId(), userId)) {
            return true;
        }
        // Path 2: published, BANK_VISIBLE
        if (isPublishedBankVisible(q.getId(), schoolId)) {
            return true;
        }
        // Path 3: review queue
        if (isReviewQueueQuestion(q.getId(), userId, schoolId)) {
            return true;
        }
        return false;
    }

    // ==================== CLASSROOM_ASSESSMENT ====================
    // Rules: teacher tạo nó xem được, hoặc ai xem được bài assessment thì xem được câu hỏi

    private boolean canViewClassroomAssessment(QuestionJpaEntity q, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return !"ARCHIVED".equals(q.getStatus());
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSameSchool(q.getId(), user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            // Teacher tạo câu hỏi này
            if (userIdEquals(q.getCreatedBy(), user.userId())) {
                return true;
            }
            // TODO: khi implement module assessment, thêm check:
            // if (canViewAssessment(assessmentId, user)) return true;
            return false;
        }
        return false;
    }

    // ==================== CENTRAL_EXAM_DRAFT ====================
    // Rules: teacher tạo xem được, tùy status mà school/teacher khác xem được

    private boolean canViewCentralExamDraft(QuestionJpaEntity q, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return !"ARCHIVED".equals(q.getStatus());
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            // School admin xem được nếu cùng school và question không ARCHIVED
            return isSameSchool(q.getId(), user.schoolId())
                && !"ARCHIVED".equals(q.getStatus());
        }
        if ("TEACHER".equals(user.role())) {
            // Teacher tạo luôn xem được
            if (userIdEquals(q.getCreatedBy(), user.userId())) {
                return true;
            }
            // Teacher khác: chỉ xem được khi PUBLISHED + cùng school + BANK_VISIBLE
            if ("PUBLISHED".equals(q.getStatus())
                    && "BANK_VISIBLE".equals(q.getVisibility())
                    && isSameSchool(q.getId(), user.schoolId())) {
                return true;
            }
            // Reviewer: SUBMITTED_FOR_REVIEW + REVIEWER_ONLY + cùng school
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

    // ==================== CENTRAL_EXAM_PAPER ====================
    // Rules: giáo viên phụ trách tạo đề xem được, school admin xem được

    private boolean canViewCentralExamPaper(QuestionJpaEntity q, ResolvedUser user) {
        if ("SYSTEM_ADMIN".equals(user.role())) {
            return true;
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSameSchool(q.getId(), user.schoolId());
        }
        if ("TEACHER".equals(user.role())) {
            // Giáo viên phụ trách tạo đề
            return userIdEquals(q.getCreatedBy(), user.userId());
        }
        return false;
    }

    // ==================== HELPERS ====================

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

    private UUID getSchoolId(UUID userId) {
        return schoolUserRepository.findByUserId(userId)
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new ForbiddenException("Nguoi dung hien tai khong thuoc truong nao"));
    }

    private record ResolvedUser(UUID userId, String role, UUID schoolId) {}
}

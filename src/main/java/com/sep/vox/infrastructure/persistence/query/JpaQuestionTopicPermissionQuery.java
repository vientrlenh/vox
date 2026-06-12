package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.QuestionTopicPermissionQuery;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaQuestionTopicPermissionQuery implements QuestionTopicPermissionQuery {

    @PersistenceContext
    private EntityManager em;

    private final UserContextPort userContextPort;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    public JpaQuestionTopicPermissionQuery(
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

    @Override
    public boolean canCreateTopic(UUID bankId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsBankWithStatus(bankId, "DRAFT", "PUBLISHED");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerWithBankStatus(bankId, user.schoolId(), "DRAFT", "PUBLISHED");
        }
        return false;
    }

    @Override
    public boolean canUpdateTopic(UUID topicId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsTopicWithBankStatus(topicId, "DRAFT", "PUBLISHED");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerOfTopic(topicId, user.schoolId(), "DRAFT", "PUBLISHED");
        }
        return false;
    }

    @Override
    public boolean canPublishTopic(UUID topicId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsTopicWithBankAndTopicStatus(topicId, "DRAFT", "PUBLISHED", "DRAFT");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerOfTopicWithStatus(topicId, user.schoolId(), "DRAFT", "PUBLISHED", "DRAFT");
        }
        return false;
    }

    @Override
    public boolean canArchiveTopic(UUID topicId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsTopicWithBankAndTopicStatus(topicId, "DRAFT", "PUBLISHED", "DRAFT", "PUBLISHED");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerOfTopicWithStatus(topicId, user.schoolId(), "DRAFT", "PUBLISHED", "DRAFT", "PUBLISHED");
        }
        return false;
    }

    @Override
    public boolean canRestoreTopic(UUID topicId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsTopicWithBankAndTopicStatus(topicId, "DRAFT", "PUBLISHED", "ARCHIVED");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerOfTopicWithStatus(topicId, user.schoolId(), "DRAFT", "PUBLISHED", "ARCHIVED");
        }
        return false;
    }

    private boolean existsBankWithStatus(UUID bankId, String... statuses) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionBankJpaEntity qb
                WHERE qb.id = :bankId AND qb.status IN :statuses
                """)
                .setParameter("bankId", bankId)
                .setParameter("statuses", List.of(statuses))
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isSchoolOwnerWithBankStatus(UUID bankId, UUID schoolId, String... statuses) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionBankJpaEntity qb
                WHERE qb.id = :bankId
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                  AND qb.status IN :statuses
                """)
                .setParameter("bankId", bankId)
                .setParameter("schoolId", schoolId)
                .setParameter("statuses", List.of(statuses))
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean existsTopicWithBankStatus(UUID topicId, String... bankStatuses) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionTopicJpaEntity qt
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE qt.id = :topicId
                  AND qt.status <> 'ARCHIVED'
                  AND qb.status IN :bankStatuses
                """)
                .setParameter("topicId", topicId)
                .setParameter("bankStatuses", List.of(bankStatuses))
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isSchoolOwnerOfTopic(UUID topicId, UUID schoolId, String... bankStatuses) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionTopicJpaEntity qt
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE qt.id = :topicId
                  AND qt.status <> 'ARCHIVED'
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                  AND qb.status IN :bankStatuses
                """)
                .setParameter("topicId", topicId)
                .setParameter("schoolId", schoolId)
                .setParameter("bankStatuses", List.of(bankStatuses))
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean existsTopicWithBankAndTopicStatus(UUID topicId, String bankStatuses, String topicStatuses) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionTopicJpaEntity qt
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE qt.id = :topicId
                  AND qb.status IN :bankStatuses
                  AND qt.status = :topicStatus
                """)
                .setParameter("topicId", topicId)
                .setParameter("bankStatuses", List.of(bankStatuses))
                .setParameter("topicStatus", topicStatuses)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean existsTopicWithBankAndTopicStatus(UUID topicId, String bankStatus, String... topicStatuses) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionTopicJpaEntity qt
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE qt.id = :topicId
                  AND qb.status IN :bankStatuses
                  AND qt.status IN :topicStatuses
                """)
                .setParameter("topicId", topicId)
                .setParameter("bankStatuses", List.of(bankStatus))
                .setParameter("topicStatuses", List.of(topicStatuses))
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isSchoolOwnerOfTopicWithStatus(UUID topicId, UUID schoolId, String bankStatus, String... topicStatuses) {
        try {
            em.createQuery("""
                SELECT 1 FROM QuestionTopicJpaEntity qt
                JOIN QuestionBankJpaEntity qb ON qt.questionBankId = qb.id
                WHERE qt.id = :topicId
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                  AND qb.status IN :bankStatuses
                  AND qt.status IN :topicStatuses
                """)
                .setParameter("topicId", topicId)
                .setParameter("schoolId", schoolId)
                .setParameter("bankStatuses", List.of(bankStatus))
                .setParameter("topicStatuses", List.of(topicStatuses))
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private UUID getSchoolId(UUID userId) {
        return schoolUserRepository.findByUserId(userId)
            .map(SchoolUser::getSchoolId)
            .orElseThrow(() -> new ForbiddenException("Nguoi dung hien tai khong thuoc truong nao"));
    }

    private record ResolvedUser(UUID userId, String role, UUID schoolId) {}
}

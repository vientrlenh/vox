package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.dto.UserRoleInfo;
import com.sep.vox.application.query.repository.QuestionBankPermissionQuery;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.UserRepository;
import com.sep.vox.infrastructure.persistence.entity.QuestionBankJpaEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaQuestionBankPermissionQuery implements QuestionBankPermissionQuery {

    @PersistenceContext
    private EntityManager em;

    private final UserContextPort userContextPort;
    private final UserRoleQueryRepository userRoleQueryRepository;
    private final UserRepository userRepository;

    public JpaQuestionBankPermissionQuery(
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
    public boolean canUpdateBank(UUID bankId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsBankWithStatus(bankId, "DRAFT", "PUBLISHED");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerWithStatus(bankId, user.schoolId(), "DRAFT", "PUBLISHED");
        }
        return false;
    }

    @Override
    public boolean canPublishBank(UUID bankId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsBankWithStatus(bankId, "DRAFT");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerWithStatus(bankId, user.schoolId(), "DRAFT");
        }
        return false;
    }

    @Override
    public boolean canArchiveBank(UUID bankId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsBankWithStatus(bankId, "DRAFT", "PUBLISHED");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerWithStatus(bankId, user.schoolId(), "DRAFT", "PUBLISHED");
        }
        return false;
    }

    @Override
    public boolean canRestoreBank(UUID bankId) {
        var user = resolveCurrentUser();

        if ("SYSTEM_ADMIN".equals(user.role())) {
            return existsBankWithStatus(bankId, "ARCHIVED");
        }
        if ("SCHOOL_ADMIN".equals(user.role())) {
            return isSchoolOwnerWithStatus(bankId, user.schoolId(), "ARCHIVED");
        }
        return false;
    }

    private boolean existsBankWithStatus(UUID bankId, String... statuses) {
        try {
            var statusList = List.of(statuses);
            em.createQuery("""
                SELECT 1 FROM QuestionBankJpaEntity qb
                WHERE qb.id = :bankId
                  AND qb.status IN :statuses
                """)
                .setParameter("bankId", bankId)
                .setParameter("statuses", statusList)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private boolean isSchoolOwnerWithStatus(UUID bankId, UUID schoolId, String... statuses) {
        try {
            var statusList = List.of(statuses);
            em.createQuery("""
                SELECT 1 FROM QuestionBankJpaEntity qb
                WHERE qb.id = :bankId
                  AND qb.ownerType = 'SCHOOL' AND qb.schoolId = :schoolId
                  AND qb.status IN :statuses
                """)
                .setParameter("bankId", bankId)
                .setParameter("schoolId", schoolId)
                .setParameter("statuses", statusList)
                .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    private record ResolvedUser(UUID userId, String role, UUID schoolId) {}
}

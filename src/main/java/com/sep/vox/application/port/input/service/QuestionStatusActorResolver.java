package com.sep.vox.application.port.input.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.common.RoleConstant;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.query.repository.UserRoleQueryRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.service.question.QuestionStatusTransition;

/**
 * Resolves the authorization context of the person acting on a question.
 *
 * <p>Split out so the bulk path calls it exactly ONCE for the whole batch. Previously these three queries ran
 * again for every question in the loop, so updating 50 questions meant 150 redundant queries.
 *
 * <p>Deliberately NOT annotated with {@code @Transactional}: it is read-only and is always called from inside the use
 * case's transaction so it joins automatically. Adding a transaction proxy here would only reopen the very rollback-only trap
 * that {@link QuestionStatusTransition} was designed to avoid.
 */
@Service
@RequiredArgsConstructor
public class QuestionStatusActorResolver {

    private final UserContextPort userContextPort;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRoleQueryRepository userRoleQueryRepository;

    public QuestionStatusTransition.Actor resolve() {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        boolean systemAdmin = userContextPort.isSystemAdmin();
        UUID currentSchoolId = schoolUserRepository.findByUserId(currentUserId)
            .map(schoolUser -> schoolUser.getSchoolId())
            .orElse(null);
        boolean schoolAdmin = !systemAdmin
            && userRoleQueryRepository.findByUserIdWithRoleInfo(currentUserId).stream()
                .anyMatch(role -> RoleConstant.SCHOOL_ADMIN_ROLE.equals(role.roleCode()));

        return new QuestionStatusTransition.Actor(currentUserId, currentSchoolId, systemAdmin, schoolAdmin);
    }

    /**
     * Resolves the caller for a listing: everyone except a system admin must belong to a school.
     */
    public QuestionStatusTransition.Actor resolveForListing() {
        QuestionStatusTransition.Actor actor = resolve();
        if (!actor.systemAdmin() && actor.schoolId() == null) {
            throw new ForbiddenException("Access denied");
        }
        return actor;
    }
}

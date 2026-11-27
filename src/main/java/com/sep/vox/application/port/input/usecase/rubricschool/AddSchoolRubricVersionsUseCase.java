package com.sep.vox.application.port.input.usecase.rubricschool;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.rubricschool.AddSchoolRubricVersionsCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.User;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddSchoolRubricVersionsUseCase implements IUseCase<AddSchoolRubricVersionsCommand, List<UUID>> {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;

    @Override
    @Transactional
    public List<UUID> execute(AddSchoolRubricVersionsCommand command) {
        requireActiveSchoolMember(command.schoolId());
        Rubric rubric = loadSchoolOwnedRubric(command);

        Set<Integer> existingVersions = rubricVersionRepository.findByRubricId(command.rubricId()).stream()
                .map(v -> v.getVersion())
                .collect(Collectors.toSet());

        Set<Integer> incomingVersions = new HashSet<>();
        Instant now = Instant.now();

        List<RubricVersion> newVersions = command.versions().stream()
                .map(vCmd -> toDraftVersion(command, rubric, vCmd, existingVersions, incomingVersions, now))
                .toList();

        List<RubricVersion> savedVersions = rubricVersionRepository.saveAll(newVersions);

        return savedVersions.stream().map(v -> v.getId()).toList();
    }

    private void requireActiveSchoolMember(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new UnauthorizedException("The account does not exist."));
        if (currentUser.getStatus() != UserStatus.ACTIVE) throw new UnauthorizedException("The account is locked.");

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId).orElseThrow(() -> new ForbiddenException("Does not belong to any school."));
        if (!schoolUser.getSchoolId().equals(schoolId)) throw new ForbiddenException("SECURITY: you are not allowed to operate on this school.");
    }

    private Rubric loadSchoolOwnedRubric(AddSchoolRubricVersionsCommand command) {
        Rubric rubric = rubricRepository.findById(command.rubricId()).orElseThrow(() -> new ResourceNotFoundException("Parent Rubric not found."));
        if (rubric.getOwnerType() != RubricOwnerType.SCHOOL || rubric.getSchoolId() == null || !rubric.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("Security: this Rubric is not owned by your school.");
        }
        return rubric;
    }

    private void requireNewVersionWithValidScale(AddSchoolRubricVersionsCommand.RubricVersionItemCommand vCmd,
                                                 Set<Integer> existingVersions, Set<Integer> incomingVersions) {
        if (!incomingVersions.add(vCmd.version())) {
            throw new IllegalArgumentException("System error: you are sending several records with the same Version number (" + vCmd.version() + ").");
        }
        if (existingVersions.contains(vCmd.version())) {
            throw new IllegalArgumentException("System error: Version " + vCmd.version() + " already exists in this Rubric.");
        }
        if (vCmd.scoringScaleMin().compareTo(vCmd.scoringScaleMax()) > 0) {
            throw new IllegalArgumentException("Version " + vCmd.version() + ": the floor score must not be greater than the ceiling score.");
        }
    }

    private RubricVersion toDraftVersion(AddSchoolRubricVersionsCommand command, Rubric rubric,
                                         AddSchoolRubricVersionsCommand.RubricVersionItemCommand vCmd,
                                         Set<Integer> existingVersions, Set<Integer> incomingVersions, Instant now) {
        requireNewVersionWithValidScale(vCmd, existingVersions, incomingVersions);

        Instant validFrom = vCmd.effectiveFrom() != null ? vCmd.effectiveFrom() : now;
        if (vCmd.effectiveTo() != null && vCmd.effectiveTo().isBefore(validFrom)) {
            throw new IllegalArgumentException("Version " + vCmd.version() + ": the end date must not be before the start date.");
        }

        String safeCode = rubric.getCode() + "_V" + vCmd.version();
        String safeName = (vCmd.name() != null && !vCmd.name().isBlank())
                ? vCmd.name().trim()
                : rubric.getName() + " - Version " + vCmd.version();

        return RubricVersion.draft(
                command.rubricId(), vCmd.version(), safeCode, safeName, rubric.getDescription(),
                validFrom, vCmd.effectiveTo(),
                vCmd.scoringScaleMin(), vCmd.scoringScaleMax(), vCmd.totalScoreMethod());
    }
}

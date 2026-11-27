package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.schoolgrade.DeleteSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolGradeStatus;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolClassRepository;
import com.sep.vox.domain.repository.SchoolClassUserRepository;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteSchoolGradeUseCase implements IUseCase<DeleteSchoolGradeCommand, Void> {

    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassUserRepository schoolClassUserRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserRepository schoolUserRepository;

    @Override
    @Transactional
    public Void execute(DeleteSchoolGradeCommand command) {
        requireCallerBelongsToSchool(command.schoolId());

        SchoolGrade grade = schoolGradeRepository.findById(command.id())
                .orElseThrow(() -> new ResourceNotFoundException("This school year/course was not found."));

        // school_grades carries schoolId directly -- the grade level no longer has to serve as a "bridge".
        if (!grade.getSchoolId().equals(command.schoolId())) {
            throw new ForbiddenException("SECURITY: This data is not managed by your school.");
        }

        // Already soft-deleted (ARCHIVED) earlier -> treated as non-existent.
        if (grade.getStatus() == SchoolGradeStatus.ARCHIVED) {
            throw new ResourceNotFoundException("This school year/course was already deleted.");
        }

        archiveGradeWithClassesAndMemberships(grade);

        return null;
    }

    private void requireCallerBelongsToSchool(UUID schoolId) {
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or is locked.");
        }

        SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Your account is not linked to a school."));

        if (!schoolUser.getSchoolId().equals(schoolId)) {
            throw new ForbiddenException("SECURITY: You are not allowed to act on another school's data.");
        }
    }

    // Soft-delete cascade in one transaction:
    //  - Deactivate every active member of the classes of the school year (only memberships are touched, the
    //    student/teacher accounts are NOT).
    //  - Soft-delete (ARCHIVED) every class of the school year.
    //  - Soft-delete (ARCHIVED) the school year itself.
    // Everything is a soft delete so that references (assessments/scores...) are kept.
    private void archiveGradeWithClassesAndMemberships(SchoolGrade grade) {
        Instant now = Instant.now();
        schoolClassUserRepository.deactivateByGradeId(grade.getId(), now);
        schoolClassRepository.archiveByGradeId(grade.getId());

        grade.setStatus(SchoolGradeStatus.ARCHIVED);
        schoolGradeRepository.save(grade);
    }
}
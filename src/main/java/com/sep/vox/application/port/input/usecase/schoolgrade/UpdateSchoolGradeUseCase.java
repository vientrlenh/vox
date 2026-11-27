package com.sep.vox.application.port.input.usecase.schoolgrade;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.exception.UnauthorizedException;
import com.sep.vox.application.port.input.command.schoolgrade.UpdateSchoolGradeCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.SchoolGrade;
import com.sep.vox.domain.model.school.SchoolUser;
import com.sep.vox.domain.model.user.UserStatus;
import com.sep.vox.domain.repository.SchoolGradeRepository;
import com.sep.vox.domain.repository.SchoolUserRepository;
import com.sep.vox.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateSchoolGradeUseCase implements IUseCase<UpdateSchoolGradeCommand, UUID> {
    private final SchoolGradeRepository schoolGradeRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserContextPort userContextPort;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UUID execute(UpdateSchoolGradeCommand command) {
        SchoolGrade grade = schoolGradeRepository.findById(command.schoolGradeId())
                .orElseThrow(() -> new ResourceNotFoundException("School year/course (School Grade) not found."));

        // school_grades carries schoolId directly -- the grade level no longer has to serve as a "bridge".

        requireCallerMayEdit(grade);
        validateDateOrder(command, grade);

        int updatedRows = updateAtomically(command);

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Update failed or no information changed.");
        }

        return command.schoolGradeId();
    }

    private void requireCallerMayEdit(SchoolGrade grade) {
        // exists call instead of loading the user, for performance
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();
        if (!userRepository.existsByIdAndStatus(currentUserId, UserStatus.ACTIVE)) {
            throw new UnauthorizedException("The account does not exist or has been locked.");
        }

        // SECURITY GUARD: check the school user's rights (a system admin skips it)
        if (!userContextPort.isSystemAdmin()) {
            SchoolUser schoolUser = schoolUserRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ForbiddenException("SECURITY: You are not allowed to edit another school's data."));
            // Compare the user's SchoolId with the SchoolId of this school grade
            if (!schoolUser.getSchoolId().equals(grade.getSchoolId())) {
                throw new ForbiddenException("SECURITY: You are not allowed to edit another school's data.");
            }
        }
    }

    private void validateDateOrder(UpdateSchoolGradeCommand command, SchoolGrade grade) {
        LocalDate startDate = (command.startDate() != null) ? command.startDate() : grade.getStartDate();
        LocalDate endDate = (command.endDate() != null) ? command.endDate() : grade.getEndDate();

        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("The start date must fall before the end date.");
        }
    }

    private int updateAtomically(UpdateSchoolGradeCommand command) {
        return schoolGradeRepository.updateSchoolGradeAtomic(
                command.schoolGradeId(),
                command.name() != null ? StringNormalization.trimAndCollapseSpaces(command.name()) : null,
                command.description() != null ? StringNormalization.trimAndCollapseSpaces(command.description()) : null,
                command.descriptionProvided(),
                command.startDate(),
                command.endDate()
        );
    }
}
package com.sep.vox.application.port.input.usecase.systemadmin;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.port.input.command.CreateSchoolCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.model.school.School;
import com.sep.vox.domain.repository.SchoolRepository;
import com.sep.vox.domain.valueobject.Email;
import com.sep.vox.domain.valueobject.Phone;
import com.sep.vox.domain.valueobject.SchoolCode;
import com.sep.vox.domain.valueobject.SchoolDomain;
import com.sep.vox.domain.valueobject.StudentCount;

@Service
public class CreateSchoolUseCase implements IUseCase<CreateSchoolCommand, Void> {

    private final SchoolRepository schoolRepository;
    private final UserContextPort userContextPort;

    public CreateSchoolUseCase(SchoolRepository schoolRepository, UserContextPort userContextPort) {
        this.schoolRepository = schoolRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public Void execute(CreateSchoolCommand input) {
        var command = normalize(input);

        var userId = userContextPort.getCurrentAuthenticatedUserId();
        var now = OffsetDateTime.now();
        var school = new School(
            new SchoolCode(command.code()), 
            command.name(), 
            command.description(), 
            new Phone(command.contactPhone()), new Email(command.contactEmail()), 
            new SchoolDomain(command.domain()), 
            command.address(), 
            new StudentCount(command.studentCount()), 
            true, 
            now, 
            now, 
            userId, 
            userId
        );
        schoolRepository.save(school);
        return null;
    }

    private CreateSchoolCommand normalize(CreateSchoolCommand input) {
        return new CreateSchoolCommand(
            StringNormalization.normalizeSchoolCode(input.code()), 
            StringNormalization.trimAndCollapseSpaces(input.name()), 
            StringNormalization.trimAndCollapseSpaces(input.description()), 
            StringNormalization.normalizePhone(input.contactPhone()), 
            StringNormalization.normalizeEmail(input.contactEmail()), 
            StringNormalization.normalizeDomain(input.domain()), 
            StringNormalization.trimAndCollapseSpaces(input.address()), 
            input.studentCount());
    }
    
}

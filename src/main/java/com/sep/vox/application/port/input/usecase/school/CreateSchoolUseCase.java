package com.sep.vox.application.port.input.usecase.school;

import com.sep.vox.domain.model.school.SchoolDirectory;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.AvatarUrlPolicy;
import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.school.CreateSchoolCommand;
import com.sep.vox.application.port.input.command.school.ProvisionSchoolCommand;
import com.sep.vox.application.port.input.service.ProvisionSchoolService;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.AvatarConfigPort;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

/**
 * Creates a school DIRECTLY, without a register form -- the SYSTEM_ADMIN enters the school and administrator
 * details straight in, skipping the whole RegisterForm cycle (domain OTP / document upload / waiting for
 * approval) that RegisterFromSchoolDirectoryUseCase + ApproveRegisterFormUseCase have to go through.
 *
 * <p>The persistence part is NOT rewritten: it shares ProvisionSchoolService with the form approval flow, so
 * the duplicate guards (school code, domain, email, phone number), the creation of School + admin User +
 * SCHOOL_ADMIN role + SchoolUser link, and the USER_CREATED event are all identical. That event is what makes
 * the new administrator receive the set-password mail like every other user -- the token is generated in the
 * consumer right before sending, and is never stored in plaintext in the outbox/Kafka.
 */
@Service
@RequiredArgsConstructor
public class CreateSchoolUseCase implements IUseCase<CreateSchoolCommand, UUID> {

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final ProvisionSchoolService provisionSchoolService;
    private final AvatarConfigPort avatarConfigPort;

    @Override
    @Transactional
    public UUID execute(CreateSchoolCommand input) {
        CreateSchoolCommand command = normalize(input);
        validateCommand(command);

        Instant now = Instant.now();
        SchoolInfo school = resolveSchoolInfo(command);

        return provisionSchoolService.provision(new ProvisionSchoolCommand(
            school.code(),
            school.name(),
            null,
            school.domain(),
            school.address(),
            command.studentCount(),
            command.adminEmail(),
            command.adminPhone(),
            command.adminFullName(),
            command.adminDateOfBirth(),
            command.adminAddress(),
            command.adminAvatarUrl(),
            now
        ));
    }

    private CreateSchoolCommand normalize(CreateSchoolCommand input) {
        return new CreateSchoolCommand(
            input.schoolDirectoryId(),
            StringNormalization.normalizeCode(input.schoolCode()),
            StringNormalization.trimAndCollapseSpaces(input.schoolName()),
            StringNormalization.trimAndCollapseSpaces(input.schoolAddress()),
            StringNormalization.normalizeDomain(input.schoolDomain()),
            input.studentCount(),
            StringNormalization.normalizeEmail(input.adminEmail()),
            StringNormalization.normalizePhone(input.adminPhone()),
            StringNormalization.trimAndCollapseSpaces(input.adminFullName()),
            input.adminDateOfBirth(),
            StringNormalization.trimAndCollapseSpaces(input.adminAddress()),
            // Do NOT use trimAndCollapseSpaces as for the other text fields: "collapsing whitespace" is meaningless
            // for a URL, and more importantly it checks nothing -- this field used to accept any URL, so every
            // teacher opening the grading list would call out to that foreign host.
            // See AvatarUrlPolicy. This is the ONLY path with a user-entered avatar among the callers of
            // ProvisionSchoolService; the other two (form approval, OTP verification) pass null.
            AvatarUrlPolicy.normalizeOrThrow(avatarConfigPort.allowedHosts(), input.adminAvatarUrl())
        );
    }

    private void validateCommand(CreateSchoolCommand command) {
        if (command.schoolDirectoryId() == null && (command.schoolCode() == null || command.schoolCode().isBlank() || command.schoolName() == null || command.schoolName().isBlank() || command.schoolAddress() == null || command.schoolAddress().isBlank())) {
            throw new IllegalArgumentException("A school directory entry is required, or else a school code, school name and school address");
        }
    }

    /**
     * The directory takes priority: with a schoolDirectoryId, ALL school information is TAKEN from that entry and
     * the self-declared part of the request is ignored -- the two sources are not mixed, so a school never carries
     * the code/name of this directory entry while its domain was typed by the caller.
     */
    private SchoolInfo resolveSchoolInfo(CreateSchoolCommand command) {
        if (command.schoolDirectoryId() == null) {
            return new SchoolInfo(
                command.schoolCode(),
                command.schoolName(),
                command.schoolAddress(),
                command.schoolDomain()
            );
        }

        SchoolDirectory directory = verifiedDirectory(command.schoolDirectoryId());

        return new SchoolInfo(
            directory.getCode(),
            directory.getName(),
            directory.getAddress(),
            directory.getDomain()
        );
    }

    private SchoolDirectory verifiedDirectory(UUID schoolDirectoryId) {
        SchoolDirectory directory = schoolDirectoryRepository.findById(schoolDirectoryId)
            .orElseThrow(() -> new ResourceNotFoundException("The requested school directory entry was not found"));

        // Do NOT block when the directory entry is unverified -- this API deliberately skips the approval loop,
        // and a SYSTEM_ADMIN creating the school here is itself the act of verification. Mark it right away so
        // there is no mismatched state "the School exists but the directory entry is still unverified" (the same
        // way ApproveRegisterFormUseCase handles a form from the directory).
        if (!directory.isVerified()) {
            directory.verify();
            schoolDirectoryRepository.save(directory);
        }
        return directory;
    }

    /** The four fields that identify the school, once it is settled whether they come from the directory or from the self-declared part. */
    private record SchoolInfo(String code, String name, String address, String domain) {
    }
}

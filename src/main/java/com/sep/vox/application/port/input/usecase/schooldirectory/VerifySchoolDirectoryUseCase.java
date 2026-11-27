package com.sep.vox.application.port.input.usecase.schooldirectory;

import com.sep.vox.domain.model.school.SchoolDirectory;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.schooldirectory.VerifySchoolDirectoryCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
@RequiredArgsConstructor
public class VerifySchoolDirectoryUseCase implements IUseCase<VerifySchoolDirectoryCommand, UUID>{

    private final SchoolDirectoryRepository schoolDirectoryRepository;

    @Override
    @Transactional
    public UUID execute(VerifySchoolDirectoryCommand input) {
        SchoolDirectory directory = schoolDirectoryRepository.findById(input.id())
            .orElseThrow(() -> new ResourceNotFoundException("School directory entry not found"));

        if (directory.isVerified()) {
            throw new IllegalArgumentException("The school directory entry has already been verified");
        }

        directory.verify();
        SchoolDirectory updated = schoolDirectoryRepository.save(directory);
        return updated.getId();
    }

}

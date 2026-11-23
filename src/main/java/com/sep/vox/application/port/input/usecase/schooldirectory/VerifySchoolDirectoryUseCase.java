package com.sep.vox.application.port.input.usecase.schooldirectory;

import com.sep.vox.domain.model.school.SchoolDirectory;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.VerifySchoolDirectoryCommand;
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
            .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục truờng"));

        if (directory.isVerified()) {
            throw new IllegalStateException("Danh mục truờng đã được kiểm duyệt");
        }

        directory.verify();
        SchoolDirectory updated = schoolDirectoryRepository.save(directory);
        return updated.getId();
    }

}

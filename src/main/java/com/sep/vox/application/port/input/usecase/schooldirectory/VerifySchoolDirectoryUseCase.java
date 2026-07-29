package com.sep.vox.application.port.input.usecase.schooldirectory;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.VerifySchoolDirectoryCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.SchoolDirectoryRepository;

@Service
public class VerifySchoolDirectoryUseCase implements IUseCase<VerifySchoolDirectoryCommand, UUID>{

    private final SchoolDirectoryRepository schoolDirectoryRepository;
    private final UserContextPort userContextPort;

    public VerifySchoolDirectoryUseCase(SchoolDirectoryRepository schoolDirectoryRepository, UserContextPort userContextPort) {
        this.schoolDirectoryRepository = schoolDirectoryRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    @Transactional
    public UUID execute(VerifySchoolDirectoryCommand input) {
        var userId = userContextPort.getCurrentAuthenticatedUserId();

        var directory = schoolDirectoryRepository.findById(input.id())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục truờng"));

        if (directory.isVerified()) {
            throw new IllegalStateException("Danh mục truờng đã được kiểm duyệt");
        }

        directory.verify(userId, OffsetDateTime.now());
        var updated = schoolDirectoryRepository.save(directory);
        return updated.getId();
    }

}

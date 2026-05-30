package com.sep.vox.application.port.input.usecase.schooluser;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UploadSchoolUserImportFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ImportFileData;
import com.sep.vox.application.port.output.SchoolUserImportFileStoragePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.schooluser.SchoolUserImportUploadResponse;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UploadSchoolUserImportFileUseCase implements IUseCase<UploadSchoolUserImportFileCommand, SchoolUserImportUploadResponse> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final SchoolUserImportFileStoragePort fileStoragePort;

    public UploadSchoolUserImportFileUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            SchoolUserImportFileStoragePort fileStoragePort) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    public SchoolUserImportUploadResponse execute(UploadSchoolUserImportFileCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();
        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (!input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }
        if (input.content() == null || input.content().length == 0) {
            throw new IllegalArgumentException("File không hợp lệ");
        }

        var stored = fileStoragePort.save(new ImportFileData(
            input.originalFileName(),
            input.contentType(),
            input.content()
        ));

        return new SchoolUserImportUploadResponse(
            stored.fileId(),
            stored.originalFileName(),
            stored.format(),
            stored.sizeBytes(),
            stored.expiresAt()
        );
    }
}

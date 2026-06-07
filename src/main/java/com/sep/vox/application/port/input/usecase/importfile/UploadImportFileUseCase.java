package com.sep.vox.application.port.input.usecase.importfile;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.UploadImportFileCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.ImportFileData;
import com.sep.vox.application.port.output.ImportFileStoragePort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.application.response.input.importfile.ImportFileUploadResponse;
import com.sep.vox.domain.repository.UserRepository;

@Service
public class UploadImportFileUseCase implements IUseCase<UploadImportFileCommand, ImportFileUploadResponse> {

    private final UserContextPort userContextPort;
    private final UserRepository userRepository;
    private final ImportFileStoragePort fileStoragePort;

    public UploadImportFileUseCase(
            UserContextPort userContextPort,
            UserRepository userRepository,
            ImportFileStoragePort fileStoragePort) {
        this.userContextPort = userContextPort;
        this.userRepository = userRepository;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    public ImportFileUploadResponse execute(UploadImportFileCommand input) {
        var callerId = userContextPort.getCurrentAuthenticatedUserId();
        var caller = userRepository.findById(callerId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
        if (caller.getSchoolId() == null || !input.schoolId().equals(caller.getSchoolId())) {
            throw new IllegalArgumentException("Không có quyền thực hiện thao tác này");
        }
        if (input.content() == null || input.content().length == 0) {
            throw new IllegalArgumentException("File không hợp lệ");
        }

        var stored = fileStoragePort.save(new ImportFileData(
            input.originalFileName(),
            input.contentType(),
            input.content()
        ), caller.getSchoolId(), callerId);

        return new ImportFileUploadResponse(
            stored.fileId(),
            stored.originalFileName(),
            stored.format(),
            stored.sizeBytes(),
            stored.expiresAt()
        );
    }
}

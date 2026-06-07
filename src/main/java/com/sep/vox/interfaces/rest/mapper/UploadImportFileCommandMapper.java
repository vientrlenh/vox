package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.sep.vox.application.port.input.command.UploadImportFileCommand;

public final class UploadImportFileCommandMapper {

    private UploadImportFileCommandMapper() {
    }

    public static UploadImportFileCommand fromRequest(UUID schoolId, MultipartFile file) {
        try {
            return new UploadImportFileCommand(
                schoolId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể đọc file upload", e);
        }
    }
}

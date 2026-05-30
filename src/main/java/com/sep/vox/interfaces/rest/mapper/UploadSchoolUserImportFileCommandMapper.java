package com.sep.vox.interfaces.rest.mapper;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.sep.vox.application.port.input.command.UploadSchoolUserImportFileCommand;

public final class UploadSchoolUserImportFileCommandMapper {

    private UploadSchoolUserImportFileCommandMapper() {
    }

    public static UploadSchoolUserImportFileCommand fromRequest(UUID schoolId, MultipartFile file) {
        try {
            return new UploadSchoolUserImportFileCommand(
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
